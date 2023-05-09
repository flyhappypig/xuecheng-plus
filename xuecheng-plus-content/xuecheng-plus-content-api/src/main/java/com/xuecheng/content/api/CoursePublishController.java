package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author gushouye
 * @description 课程预览
 **/
@Controller
public class CoursePublishController {

    @Autowired
    private CoursePublishService coursePublishService;

    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView preview(@PathVariable(name = "courseId") Long courseId) {
        ModelAndView modelAndView = new ModelAndView();
        // 指定模板
        modelAndView.setViewName("course_template");// 根据视图名称添加.ftl找到模板
        // 指定数据
        // 查询课程信息作为模型数据
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(courseId);
        modelAndView.addObject("model", coursePreviewInfo);
        return modelAndView;
    }
}
