package com.xuecheng.content.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author gushouye
 * @description 课程预览
 **/
@Controller
public class CoursePublishController {

    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView preview(@PathVariable(name = "courseId") Long courseId) {
        ModelAndView modelAndView = new ModelAndView();
        // 指定模板
        modelAndView.setViewName("course_template");// 根据视图名称添加.ftl找到模板
        // 指定数据
//        modelAndView.addObject("name", "黑马程序员");
        return modelAndView;
    }
}
