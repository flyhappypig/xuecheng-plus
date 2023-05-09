package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
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
import com.xuecheng.messagesdk.service.impl.MqMessageServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author gushouye
 * @description 课程发布服务实现类
 **/
@Service
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
