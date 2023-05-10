package com.xuecheng.media.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理接口
 * @date 2022/9/6 11:29
 */
@Api(value = "媒资文件管理接口", tags = "媒资文件管理接口")
@RestController
public class MediaFilesController {


    @Autowired
    MediaFileService mediaFileService;


    @ApiOperation("媒资列表查询接口")
    @PostMapping("/files")
    public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto) {
        Long companyId = 1232141425L;
        return mediaFileService.queryMediaFiels(companyId, pageParams, queryMediaParamsDto);
    }

    @ApiOperation("上传图片")
    @GetMapping(value = "/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadFileResultDto upload(@RequestPart("filedata") MultipartFile filedata,
                                      @RequestParam(value = "objectName", required = false) String objectName) throws IOException {
        // 准备上传文件的信息
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        uploadFileParamsDto.setFilename(filedata.getOriginalFilename()); // 原始文件名称
        uploadFileParamsDto.setFileSize(filedata.getSize()); // 文件大小
        uploadFileParamsDto.setFileType("001001"); // 文件类型
        // 接收到文件
        // 创建一个临时文件
        File tempFile = File.createTempFile("minio", ".temp");
        filedata.transferTo(tempFile);
        // 文件路径
        String localFilepath = tempFile.getAbsolutePath();
        Long companyId = 1232141425L;
        // 调用service上传文件
        return mediaFileService.uploadFile(companyId, localFilepath, uploadFileParamsDto,objectName);
    }

}
