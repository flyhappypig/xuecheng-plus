package com.xuecheng.content.model.dto;

import lombok.Data;

import java.util.List;

/**
 * @author gushouye
 * @description 课程预览模型类
 **/
@Data
public class CoursePreviewDto {

    // 课程基本信息、课程营销信息
    private CourseBaseInfoDto courseBase;

    // 课程计划信息
    private List<TeachPlanDto> teachPlans;

    // 课程师资信息


}
