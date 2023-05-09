package com.xuecheng.media.api;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author gushouye
 * @description 媒资开放接口
 **/
@Api(value = "媒资开放接口", tags = "媒资开放接口")
@RestController
@RequestMapping("/open")
public class MediaOpenController {

    @Autowired
    MediaFileService mediaFileService;

    @ApiOperation("预览文件")
    @GetMapping("/preview/{mediaId}")
    public RestResponse<String> getPlayUrlByMediaId(@PathVariable String mediaId) {
        MediaFiles mediaFiles = mediaFileService.getFileById(mediaId);
        if (mediaFiles == null) {
            return RestResponse.validfail("该视频正在处理中，请稍后再试！");
        }
        // 取出视频播放地址
        String url = mediaFiles.getUrl();
        if (StringUtils.isEmpty(url)) {
            return RestResponse.validfail("该视频正在处理中，请稍后再试！");
        }
        return RestResponse.success(url);
    }
}
