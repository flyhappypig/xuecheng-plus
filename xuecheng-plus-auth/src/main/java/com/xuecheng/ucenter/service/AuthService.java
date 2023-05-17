package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;

/**
 * @author gushouye
 * @description 统一的认证接口
 **/
public interface AuthService {

    /**
     * @param authParamsDto 认证参数
     * @return com.xuecheng.ucenter.model.po.XcUser 用户信息
     * @description 认证方法
     */
    XcUserExt execute(AuthParamsDto authParamsDto);
}
