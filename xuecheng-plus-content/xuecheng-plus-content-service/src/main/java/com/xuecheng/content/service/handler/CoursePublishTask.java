package com.xuecheng.content.service.handler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author gushouye
 * @description 课程发布定时任务
 **/
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @Autowired
    private CoursePublishService coursePublishService;

    @XxlJob("coursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();// 执行器的序号 从0开始
        int shardTotal = XxlJobHelper.getShardTotal();// 执行器的总数
        // 调用抽象类的方法，处理任务
        process(shardIndex, shardTotal, "course_publish", 30, 60);
    }


    // 执行课程发布任务的逻辑，如果此方法抛出异常，说明任务执行失败
    @Override
    public boolean execute(MqMessage mqMessage) {
        // 从mqMessage拿到课程id
        Long courseId = Long.parseLong(mqMessage.getBusinessKey1());
        // 课程静态化上传到minio
        generateCourseHtml(mqMessage, courseId);
        // 向elasticsearch中写索引数据
        saveCourseIndex(mqMessage, courseId);
        // 向redis写缓存
        saveCourseCache(mqMessage, courseId);

        // 返回true表示任务处理成功
        return true;
    }

    //生成课程静态化页面并上传至文件系统
    private void generateCourseHtml(MqMessage mqMessage, Long courseId) {
        MqMessageService mqMessageService = this.getMqMessageService();
        // 任务幂等性处理
        // 查询数据库取出该阶段的执行状态
        int stageOne = mqMessageService.getStageOne(courseId);
        if (stageOne > 0) {
            // 已经执行过了，不再执行
            log.debug("课程静态化的任务已经执行过了，不再执行");
            return;
        }
        // 开始生成课程静态化页面，生成html页面
        File file = coursePublishService.generateCourseHtml(courseId);
        if (file == null) {
            // 生成失败
            log.error("生成课程静态化页面失败");
            XueChengPlusException.cast("生成课程静态化页面为空");
        }
        // 上传html页面到文件系统
        coursePublishService.uploadCourseHtml(courseId, file);
        // 任务执行成功，更新任务状态
        mqMessageService.completedStageOne(courseId);
    }


    //保存课程索引信息
    private void saveCourseIndex(MqMessage mqMessage, Long courseId) {
        MqMessageService mqMessageService = this.getMqMessageService();
        // 任务幂等性处理
        // 查询数据库取出该阶段的执行状态
        int stageTwo = mqMessageService.getStageTwo(courseId);
        if (stageTwo > 0) {
            // 已经执行过了，不再执行
            log.debug("课程的索引信息已写入，不再执行");
            return;
        }
        // 查询课程信息，调用搜索服务添加索引

        // 任务执行成功，更新任务状态
        mqMessageService.completedStageTwo(courseId);
    }

    //将课程信息缓存至redis
    private void saveCourseCache(MqMessage mqMessage, Long courseId) {
        MqMessageService mqMessageService = this.getMqMessageService();
        // 任务幂等性处理
        // 查询数据库取出该阶段的执行状态
        int stageThree = mqMessageService.getStageThree(courseId);
        if (stageThree > 0) {
            // 已经执行过了，不再执行
            log.debug("课程信息已缓存至redis，不再执行");
            return;
        }
        // 查询课程信息，调用搜索服务添加缓存

        // 任务执行成功，更新任务状态
        mqMessageService.completedStageThree(courseId);
    }
}
