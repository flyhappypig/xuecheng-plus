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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
            XcChooseCourse xcChooseCourse = addChargeCoruse(userId, coursepublish);
        }
        // 判断学生的学习资格

        return null;
    }

    //添加免费课程,免费课程加入选课记录表、我的课程表
    public XcChooseCourse addFreeCoruse(String userId, CoursePublish coursepublish) {
        // 课程id
        Long courseId = coursepublish.getId();
        // 判断，如果存在免费的选课记录且选课状态为成功，则不允许再次添加，直接返回
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<XcChooseCourse>().eq(XcChooseCourse::getUserId, userId).eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getCourseId, courseId)
                .eq(XcChooseCourse::getOrderType, "700001") // 免费课程
                .eq(XcChooseCourse::getStatus, "701001");// 选课成功
        List<XcChooseCourse> xcChooseCourses = chooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0);
        }
        // 向选课记录表中添加选课记录
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCourseId(courseId);
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");// 免费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setValidDays(365);// 有效期365天
        xcChooseCourse.setStatus("701001");// 选课成功
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());// 有效期开始时间
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));// 有效期结束时间
        int insert = chooseCourseMapper.insert(xcChooseCourse);
        if (insert <= 0) {
            XueChengPlusException.cast("添加选课记录失败");
        }
        return xcChooseCourse;
    }

    //添加到我的课程表
    public XcCourseTables addCourseTabls(XcChooseCourse xcChooseCourse) {
        // 选课成功了，向我的课程表中添加课程记录
        String status = xcChooseCourse.getStatus();
        if (!status.equals("701001")) {
            // 选课失败，不添加
            XueChengPlusException.cast("选课失败，不添加到我的课程表");
        }
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if (xcCourseTables != null) {
            // 课程已经存在，不添加
            return xcCourseTables;
        }
        // 课程不存在，添加到我的课程表
        xcCourseTables = new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse, xcCourseTables);
        xcCourseTables.setCourseId(xcChooseCourse.getCourseId()); // 记录选课表当中的主键
        xcCourseTables.setCourseType(xcChooseCourse.getOrderType()); // 课程类型
        xcCourseTables.setUpdateDate(LocalDateTime.now());
        int insert = courseTablesMapper.insert(xcCourseTables);
        if (insert <= 0) {
            XueChengPlusException.cast("添加到我的课程表失败");
        }
        return xcCourseTables;
    }


    //添加收费课程
    public XcChooseCourse addChargeCoruse(String userId, CoursePublish coursepublish) {
        // 课程id
        Long courseId = coursepublish.getId();
        // 判断，如果存在收费的选课记录且选课状态为待支付，则不允许再次添加，直接返回
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<XcChooseCourse>().eq(XcChooseCourse::getUserId, userId).eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getCourseId, courseId)
                .eq(XcChooseCourse::getOrderType, "700002") // 收费课程
                .eq(XcChooseCourse::getStatus, "701002");// 待支付
        List<XcChooseCourse> xcChooseCourses = chooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0);
        }
        // 向选课记录表中添加选课记录
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCourseId(courseId);
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");// 收费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setValidDays(365);// 有效期365天
        xcChooseCourse.setStatus("701002");// 待支付
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());// 有效期开始时间
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));// 有效期结束时间
        int insert = chooseCourseMapper.insert(xcChooseCourse);
        if (insert <= 0) {
            XueChengPlusException.cast("添加选课记录失败");
        }
        return xcChooseCourse;
    }

    /**
     * @param userId   用户id
     * @param courseId 课程id
     * @return com.xuecheng.learning.model.po.XcCourseTables
     * @description 根据课程和用户查询我的课程表中某一门课程
     */
    public XcCourseTables getXcCourseTables(String userId, Long courseId) {
        XcCourseTables xcCourseTables = courseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId).eq(XcCourseTables::getCourseId, courseId));
        return xcCourseTables;
    }

}
