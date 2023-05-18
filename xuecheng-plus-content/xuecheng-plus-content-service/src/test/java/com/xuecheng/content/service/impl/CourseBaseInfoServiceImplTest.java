package com.xuecheng.content.service.impl;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CourseBaseInfoServiceImplTest {
    @Autowired
    private CourseBaseInfoService courseBaseInfoService;

    @Test
    public void testCourserBaseMapper() {
        // 查询条件
        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");// 课程名称
        queryCourseParamsDto.setAuditStatus("202004");// 课程审核状态--202004课程审核通过
        // 分页参数对象
        PageParams pageParams = new PageParams();
        pageParams.setPageNo(2L);
        pageParams.setPageSize(2L);
        PageResult<CourseBase> courseBasePageResult = courseBaseInfoService.queryCourseBaseList(null,pageParams, queryCourseParamsDto);
        System.out.println("courseBasePageResult = " + courseBasePageResult);
    }
}
