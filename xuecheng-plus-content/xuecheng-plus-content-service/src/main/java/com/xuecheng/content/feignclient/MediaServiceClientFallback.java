package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author gushouye
 * @description
 **/
@Component
public class MediaServiceClientFallback implements FallbackFactory<MediaServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(MediaServiceClientFallback.class);

    @Override
    public MediaServiceClient create(Throwable throwable) {
        log.error("服务调用失败:{}", throwable.getMessage());
        return new MediaServiceClient() {
            @Override
            public String upload(MultipartFile filedata, String objectName) throws IOException {
                //降级方法
                log.error("调用媒资管理服务上传文件时发生熔断，异常信息:{}",throwable.toString(),throwable);
                return "服务错误信息:" + throwable.getMessage();
            }
        };
    }
}
