package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author gushouye
 * @description 课程发布服务实现类
 **/
@Service
@Slf4j
public class CoursePublishServiceImpl implements CoursePublishService {
    @Autowired
    private CourseBaseInfoService courseBaseInfoService;
    @Autowired
    private TeachplanService teachplanService;
    @Autowired
    private CourseMarketMapper courseMarketMapper;
    @Autowired
    private CoursePublishPreMapper coursePublishPreMapper;
    @Autowired
    private CourseBaseMapper courseBaseMapper;
    @Autowired
    private CoursePublishMapper coursePublishMapper;
    @Autowired
    private MqMessageService mqMessageService;
    @Autowired
    private MediaServiceClient mediaServiceClient;
    @Autowired
//    private StringRedisTemplate redisTemplate;
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;


    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        // 查询课程基本信息、课程营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        coursePreviewDto.setCourseBase(courseBaseInfo);
        // 课程计划信息
        List<TeachPlanDto> teachPlanTree = teachplanService.findTeachPlanTree(courseId);
        coursePreviewDto.setTeachPlans(teachPlanTree);
        return coursePreviewDto;
    }

    @Transactional
    @Override
    public void commitAudit(Long companyId, Long courseId) {
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if (courseBaseInfo == null) {
            // 课程不存在
            XueChengPlusException.cast("课程不存在");
        }
        // 审核状态
        String auditStatus = courseBaseInfo.getAuditStatus();
        // 如果课程的审核状态为已提交，则不允许提交
        if ("202003".equals(auditStatus)) {
            XueChengPlusException.cast("课程已提交，不允许重复提交");
        }
        // 本机构只能提交本机构的课程
        if (!companyId.equals(courseBaseInfo.getCompanyId())) {
            XueChengPlusException.cast("只能提交本机构的课程");
        }
        // 课程的图片、课程计划没有填写，则不允许提交
        if (courseBaseInfo.getPic() == null || courseBaseInfo.getPic().isEmpty()) {
            XueChengPlusException.cast("请上传课程图片");
        }
        // 课程计划没有填写，则不允许提交
        List<TeachPlanDto> teachPlanTree = teachplanService.findTeachPlanTree(courseId);
        if (teachPlanTree == null || teachPlanTree.isEmpty()) {
            XueChengPlusException.cast("请添加课程计划");
        }
        // 查询课程基本信息、课程营销信息以及课程计划信息插入到课程预发布表中
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfo, coursePublishPre);
        // 设置机构id
        coursePublishPre.setCompanyId(companyId);
        // 营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        // 转json
        String courseMarketJson = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarketJson);
        // 计划信息
        String teachPlanJson = JSON.toJSONString(teachPlanTree);
        coursePublishPre.setTeachplan(teachPlanJson);
        // 状态为已提交
        coursePublishPre.setStatus("202003");
        // 提交时间
        coursePublishPre.setCreateDate(LocalDateTime.now());
        // 查询课程预发布表中是否存在该课程的记录，如果存在，则更新，如果不存在，则插入
        CoursePublishPre coursePublishPreExist = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPreExist == null) {
            // 插入
            coursePublishPreMapper.insert(coursePublishPre);
        } else {
            // 更新
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        // 同时更新课程基本信息表的审核状态为已提交
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("202003");// 审核状态为已提交
        courseBaseMapper.updateById(courseBase);
    }

    @Transactional
    @Override
    public void publish(Long companyId, Long courseId) {
        // 课程如果审核不通过，则不允许发布
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if (courseBaseInfo == null) {
            // 课程不存在
            XueChengPlusException.cast("课程没有审核记录，无法发布");
        }
        if (!courseBaseInfo.getStatus().equals("202004")) {
            // 课程不存在
            XueChengPlusException.cast("课程未审核通过，不允许发布");
        }
        // 查询预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        // 判断机构id是否一致
        if (!companyId.equals(coursePublishPre.getCompanyId())) {
            XueChengPlusException.cast("只能发布本机构的课程");
        }
        // 向课程发布表写数据
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        // 要先查询课程发布表，有则更新，没有则插入
        CoursePublish coursePublishObj = coursePublishMapper.selectById(courseId);
        if (coursePublishObj == null) {
            // 插入
            coursePublishMapper.insert(coursePublish);
        } else {
            // 更新
            coursePublishMapper.updateById(coursePublish);
        }
        // 向消息表写数据
//        mqMessageService.addMessage("course-publish", String.valueOf(courseId), null, null);
        saveCoursePublishMessage(courseId);
        // 将预发布表的数据删除
        coursePublishPreMapper.deleteById(courseId);
    }

    @Override
    public File generateCourseHtml(Long courseId) {
        Configuration configuration = new Configuration(Configuration.getVersion());
        File tempFile = null;
        try {
            // 设置模板路径
            String classPath = this.getClass().getResource("/").getPath();
            // 模板目录
            configuration.setDirectoryForTemplateLoading(new File(classPath + "/templates/"));
            // 指定编码
            configuration.setDefaultEncoding("utf-8");
            // 得到模板
            Template template = configuration.getTemplate("course_template.ftl");
            // 准备数据
            CoursePreviewDto coursePreviewInfo = this.getCoursePreviewInfo(courseId);
            Map<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewInfo);
            // Template template, Object model
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            // 输入流
            InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
            // 创建临时文件
            tempFile = File.createTempFile(courseId + "/", ".html");
            // 输出文件
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            // 使用流将html写入文件
            IOUtils.copy(inputStream, fileOutputStream);
        } catch (Exception e) {
            log.error("页面静态化出现问题，课程id:{}", courseId);
            e.printStackTrace();
        }
        return tempFile;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        try {
            // 将file转为MultipartFile
            MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
            String html = mediaServiceClient.upload(multipartFile, "course/" + courseId + ".html");
            if (html == null) {
                log.debug("远程调用失败，上传课程详情页面失败，课程id:{}", courseId);
                XueChengPlusException.cast("上传静态文件页面过程中存在异常");
            }
        } catch (Exception e) {
            e.printStackTrace();
            XueChengPlusException.cast("上传静态文件页面过程中存在异常");
        }
    }

    /**
     * 根据课程的id查询课程的发布信息
     *
     * @param courseId 课程id
     * @return
     */
    @Override
    public CoursePublish getCoursePublish(Long courseId) {
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        return coursePublish;
    }

//    @Override
//    public CoursePublish getCoursePublishCache(Long courseId) {
//
//        //todo:方案一:解决缓冲穿透---> 查询布隆过滤器,如果返回0,则表示课程id一定不存在
//        // 解决缓存雪崩---> 1.加锁：synchronized (this) ，
//        //                2.给key设置过期时间,过期时间随机,防止同一时间大量的key过期
//        //                3.缓存预热：定时任务
//        //                4.集群部署，分布式锁
//        //                5.限流
//        //                6.熔断
//        //                7.降级
//        //                8.异步处理
//        // 解决缓存击穿---> 1.加锁：synchronized (this) ，
//        //                2.给key设置过期时间,要么永不过期，要么设置过期时间随机,防止同一时间大量的key过期
//
//        // 查询缓存
//        Object coursePublishStr = redisTemplate.opsForValue().get("course_publish_" + courseId);
//        if (coursePublishStr != null) {
//            if (coursePublishStr.toString().equals("null")) {
//                return null;
//            }
//            // 缓存中存在
//            CoursePublish coursePublish = JSON.parseObject(coursePublishStr.toString(), CoursePublish.class);
//            return coursePublish;
//        } else {
//            // 调用redis的方法，执行setnx命令，谁执行成功，谁拿到锁，value值是当前时间+超时时间,超时时间是30秒
//            // todo:不具有原子性，需要配合lua脚本使用
//            Boolean lock = redisTemplate.opsForValue().setIfAbsent("course_publish_lock_" + courseId, "01", 30, TimeUnit.SECONDS);
//            if (lock) {
//                // true表示设置成功，表示抢到锁
//                // 再次查询缓存
//                // 查询缓存
//                coursePublishStr = redisTemplate.opsForValue().get("course_publish_" + courseId);
//                if (coursePublishStr != null) {
//                    if (coursePublishStr.toString().equals("null")) {
//                        return null;
//                    }
//                    // 缓存中存在
//                    CoursePublish coursePublish = JSON.parseObject(coursePublishStr.toString(), CoursePublish.class);
//                    return coursePublish;
//                }
//                // 从数据库中查询
//                CoursePublish coursePublish = this.getCoursePublish(courseId);
//                // 将数据存入缓存
//                redisTemplate.opsForValue().set("course_publish_" + courseId, JSON.toJSONString(coursePublish), new Random().nextInt(100) + 300, TimeUnit.SECONDS);
//                return coursePublish;
//            }
//        }
//        return null;
//    }


//    @Override
//    public CoursePublish getCoursePublishCache(Long courseId) {
//
//        //todo:方案一:解决缓冲穿透---> 查询布隆过滤器,如果返回0,则表示课程id一定不存在
//        // 解决缓存雪崩---> 1.加锁：synchronized (this) ，
//        //                2.给key设置过期时间,过期时间随机,防止同一时间大量的key过期
//        //                3.缓存预热：定时任务
//        //                4.集群部署，分布式锁
//        //                5.限流
//        //                6.熔断
//        //                7.降级
//        //                8.异步处理
//        // 解决缓存击穿---> 1.加锁：synchronized (this) ，
//        //                2.给key设置过期时间,要么永不过期，要么设置过期时间随机,防止同一时间大量的key过期
//        // 自旋锁
////        ReentrantLock reentrantLock = new ReentrantLock();
////        reentrantLock.tryLock();
////        try {
////            // 获取锁之后的逻辑
////            // ...
////        } finally {
////            reentrantLock.unlock(); // 释放锁
////        }
//        // 查询缓存
//        Object coursePublishStr = redisTemplate.opsForValue().get("course_publish_" + courseId);
//        if (coursePublishStr != null) {
//            if (coursePublishStr.toString().equals("null")) {
//                return null;
//            }
//            // 缓存中存在
//            CoursePublish coursePublish = JSON.parseObject(coursePublishStr.toString(), CoursePublish.class);
//            return coursePublish;
//        } else {
//            synchronized (this) {
//                // 再次查询缓存
//                // 查询缓存
//                coursePublishStr = redisTemplate.opsForValue().get("course_publish_" + courseId);
//                if (coursePublishStr != null) {
//                    if (coursePublishStr.toString().equals("null")) {
//                        return null;
//                    }
//                    // 缓存中存在
//                    CoursePublish coursePublish = JSON.parseObject(coursePublishStr.toString(), CoursePublish.class);
//                    return coursePublish;
//                }
//                // 从数据库中查询
//                CoursePublish coursePublish = this.getCoursePublish(courseId);
//                // 将数据存入缓存
//                redisTemplate.opsForValue().set("course_publish_" + courseId, JSON.toJSONString(coursePublish), new Random().nextInt(100) + 300, TimeUnit.SECONDS);
//                return coursePublish;
//            }
//        }
//    }

    /**
     * 使用redisson实现分布式锁
     */
    @Override
    public CoursePublish getCoursePublishCache(Long courseId) {
        // 查询缓存
        Object coursePublishStr = redisTemplate.opsForValue().get("course_publish_" + courseId);
        if (coursePublishStr != null) {
            if (coursePublishStr.toString().equals("null")) {
                return null;
            }
            // 缓存中存在
            CoursePublish coursePublish = JSON.parseObject(coursePublishStr.toString(), CoursePublish.class);
            return coursePublish;
        } else {
            RLock lock = redissonClient.getLock("courseQueryLocke:" + courseId);
            // 获取分布式锁
            lock.lock();
            try {
                // 再次查询缓存
                // 查询缓存
                coursePublishStr = redisTemplate.opsForValue().get("course_publish_" + courseId);
                if (coursePublishStr != null) {
                    if (coursePublishStr.toString().equals("null")) {
                        return null;
                    }
                    // 缓存中存在
                    CoursePublish coursePublish = JSON.parseObject(coursePublishStr.toString(), CoursePublish.class);
                    return coursePublish;
                }
                // 测试redisson锁的续期功能,redisson默认续期时间是30s
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 从数据库中查询
                CoursePublish coursePublish = this.getCoursePublish(courseId);
                // 将数据存入缓存
                redisTemplate.opsForValue().set("course_publish_" + courseId, JSON.toJSONString(coursePublish), new Random().nextInt(100) + 300, TimeUnit.SECONDS);
                return coursePublish;
            } finally {
                // 手动去释放锁
                lock.unlock();
            }
        }
    }

    /**
     * @param courseId 课程id
     * @return void
     * @description 保存消息表记录
     */
    private void saveCoursePublishMessage(Long courseId) {
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if (mqMessage == null) {
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
    }
}
