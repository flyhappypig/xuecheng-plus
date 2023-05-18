package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author gushouye
 * @description 选课相关接口实现
 **/
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    @Autowired
    private XcChooseCourseMapper chooseCourseMapper;
    @Autowired
    private XcCourseTablesMapper courseTablesMapper;
    @Autowired
    private ContentServiceClient contentServiceClient;

    @Override
    @Transactional
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        // 远程调用内容管理服务查询课程的收费规则
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            // 课程不存在
            XueChengPlusException.cast("课程不存在");
        }
        // 收费规则
        String charge = coursepublish.getCharge();
        if (charge.equals("201000")) {
            // 如果是免费课程，会向选课表中添加选课记录，我的课程表中添加课程记录
            XcChooseCourse xcChooseCourse = addFreeCoruse(userId, coursepublish);// 选课记录表
            addCourseTabls(xcChooseCourse);// 我的课程表
        } else {
            // 如果是收费课程，会向选课表中添加选课记录
            addChargeCoruse(userId, coursepublish);
        }
        // 判断学生的学习资格

        return null;
    }

    //添加免费课程,免费课程加入选课记录表、我的课程表
    public XcChooseCourse addFreeCoruse(String userId, CoursePublish coursepublish) {
        // 课程id
        Long courseId = coursepublish.getId();
        // 判断，如果存在免费的选课记录且选课状态为成功，则不允许再次添加，直接返回
        new LambdaQueryWrapper<XcChooseCourse>().eq(XcChooseCourse::getUserId, userId).eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getCourseId, courseId)
                .eq(XcChooseCourse::getOrderType, "700001") // 免费课程
                .eq(XcChooseCourse::getStatus, "701002"); // 选课成功
        return null;
    }

    //添加到我的课程表
    public XcCourseTables addCourseTabls(XcChooseCourse xcChooseCourse) {
        return null;
    }


    //添加收费课程
    public XcChooseCourse addChargeCoruse(String userId, CoursePublish coursepublish) {

        return null;
    }

}
