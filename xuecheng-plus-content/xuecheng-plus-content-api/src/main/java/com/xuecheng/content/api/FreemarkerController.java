package com.xuecheng.content.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author gushouye
 * @description Freemarker入门程序
 **/
@Controller
public class FreemarkerController {

    @GetMapping("/testfreemarker")
    public ModelAndView test() {
        ModelAndView modelAndView = new ModelAndView();
        // 指定模板
        modelAndView.setViewName("test");// 根据视图名称添加.ftl找到模板
        // 指定数据
        modelAndView.addObject("name", "黑马程序员");
        return modelAndView;
    }
}
