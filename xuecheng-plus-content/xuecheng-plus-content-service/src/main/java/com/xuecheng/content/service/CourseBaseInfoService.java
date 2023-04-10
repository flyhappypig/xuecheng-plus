package com.xuecheng.content.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;

/**
 * @author gushouye
 * @description 课程信息管理接口
 **/
public interface CourseBaseInfoService {

    /**
     * 课程分页查询
     * @param pageParams 分页查询参数
     * @param courseParamsDto 查询条件
     * @return 返回的查询结果
     */
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto courseParamsDto);
}
