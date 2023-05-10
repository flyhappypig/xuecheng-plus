package com.xuecheng.content.feignclient;

import com.xuecheng.content.config.MultipartSupportConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author gushouye
 * @description 远程调用媒资服务的接口
 **/
@FeignClient(contextId = "mediaServiceClient", value = "media-api", configuration = {MultipartSupportConfig.class},
fallbackFactory = MediaServiceClientFallback.class)
public interface MediaServiceClient {

    @ApiOperation("上传图片")
    @GetMapping(value = "media/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestPart("filedata") MultipartFile filedata,
                         @RequestParam(value = "objectName", required = false) String objectName) throws IOException;
}
