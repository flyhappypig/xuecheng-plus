package com.xuecheng.content.service;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gushouye
 * @description 测试freemarker页面静态化方法
 **/
@SpringBootTest
public class FreeMarkerTests {
    @Autowired
    private CoursePublishService coursePublishService;

    @Test
    public void testCourseCategoryMapper() throws IOException, TemplateException {
        Configuration configuration = new Configuration(Configuration.getVersion());
        // 设置模板路径
        String classPath = this.getClass().getResource("/").getPath();
        // 模板目录
        configuration.setDirectoryForTemplateLoading(new File(classPath + "/templates/"));
        // 指定编码
        configuration.setDefaultEncoding("utf-8");
        // 得到模板
        Template template = configuration.getTemplate("course_template.ftl");
        // 准备数据
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(120L);
        Map<String, Object> map = new HashMap<>();
        map.put("model", coursePreviewInfo);
        // Template template, Object model
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
        // 输入流
        InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
        // 输出文件
        FileOutputStream fileOutputStream = new FileOutputStream(new File("D:/course.html"));
        // 使用流将html写入文件
        IOUtils.copy(inputStream, fileOutputStream);
    }

}
