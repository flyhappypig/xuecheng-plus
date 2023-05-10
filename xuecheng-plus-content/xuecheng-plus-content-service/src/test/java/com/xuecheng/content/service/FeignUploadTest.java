package com.xuecheng.content.service;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @author gushouye
 * @description 测试远程调用媒资服务进行远程上传
 **/
@SpringBootTest
public class FeignUploadTest {

    @Autowired
    private MediaServiceClient mediaServiceClient;

    @Test
    public void testFeignUpload() throws IOException {
        // 将file转为MultipartFile
        File file = new File("D:/course.html");
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String html = mediaServiceClient.upload(multipartFile, "course/120.html");
        System.out.println("html = " + html);
    }
}
