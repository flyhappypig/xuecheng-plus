package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * @author gushouye
 * @description 课程计划信息模型类
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class TeachPlanDto extends Teachplan {

    // 与媒资关联的信息
    private TeachplanMedia teachplanMedia;
    // 小章节列表
    private List<TeachPlanDto> teachPlanTreeNodes;
}
