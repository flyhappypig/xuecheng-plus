package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseCategory;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author gushouye
 * @description
 **/
@Data
public class CourseCategoryTreeDto extends CourseCategory implements Serializable {

    /**
     * 子节点
     */
    List<CourseCategoryTreeDto> childrenTreeNodes;
}
