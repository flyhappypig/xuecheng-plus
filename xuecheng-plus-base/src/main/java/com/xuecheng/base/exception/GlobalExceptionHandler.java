package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * @author gushouye
 * @description 异常处理器 @RestControllerAdvice->增强
 **/
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 对项目的自定义异常类型进行处理
    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse customException(XueChengPlusException e) {

        // 记录异常
        log.error("系统异常{}", e.getErrMessage(), e);
        // 解析异常信息
        String errMessage = e.getErrMessage();
        RestErrorResponse restErrorResponse = new RestErrorResponse(errMessage);
        return restErrorResponse;
    }

    @ExceptionHandler(Exception.class)
    public RestErrorResponse exception(Exception e) {

        // 记录异常
        log.error("系统异常{}", e.getMessage(),e);
        // 解析异常信息
        RestErrorResponse restErrorResponse = new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());
        return restErrorResponse;
    }
}
