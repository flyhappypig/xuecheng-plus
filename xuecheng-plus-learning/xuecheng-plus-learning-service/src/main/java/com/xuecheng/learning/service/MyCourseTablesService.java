package com.xuecheng.learning.service;

import com.xuecheng.learning.model.dto.XcChooseCourseDto;

/**
 * @author gushouye
 * @description 选课相关接口
 **/
public interface MyCourseTablesService {

    /**
     * @param userId   用户id
     * @param courseId 课程id
     * @return com.xuecheng.learning.model.dto.XcChooseCourseDto
     * @description 添加选课
     */
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId);
}
