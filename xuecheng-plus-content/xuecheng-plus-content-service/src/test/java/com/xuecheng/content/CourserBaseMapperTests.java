package com.xuecheng.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @author gushouye
 * @description
 **/
@SpringBootTest
public class CourserBaseMapperTests {

    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Test
    public void testCourserBaseMapper() {
        CourseBase courseBase = courseBaseMapper.selectById(18);
        Assertions.assertNotNull(courseBase);

        // 详细进行分页查询的单元测试
        // 查询条件
        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");// 课程名称
        // 拼装查询条件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        // 根据名称模糊查询，在sql中拼接 course_base.name like '%值%'
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()), CourseBase::getName, queryCourseParamsDto.getCourseName());
        // 根据课程的审核状态查询 course_base.audit_status = ?
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()), CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus());
        // 根据课程的发布状态查询 course_base.publish_status = ?
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()), CourseBase::getStatus, queryCourseParamsDto.getPublishStatus());
        // 分页参数对象
        PageParams pageParams = new PageParams();
        pageParams.setPageNo(1L);
        pageParams.setPageSize(2L);
        // 创建page分页参数对象，参数：当前页码，每页记录数
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 开始进行分页查询
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        // 数据列表
        List<CourseBase> items = page.getRecords();
        // 总记录数
        long total = pageResult.getTotal();
        PageResult<CourseBase> basePageResult = new PageResult<>(items, total, pageParams.getPageNo(), pageParams.getPageSize());
        System.out.println("basePageResult = " + basePageResult);


    }
}
