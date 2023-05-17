package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author gushouye
 * @description 用户名密码认证
 **/
@Service("password_authService")
@Slf4j
public class PasswordAuthServiceImpl implements AuthService {

    @Autowired
    private XcUserMapper xcUserMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        // 账号
        String username = authParamsDto.getUsername();
        //todo 校验验证码

        // 校验账号是否存在
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if (xcUser == null) {
            throw new RuntimeException("账号不存在~");
        }
        // 校验密码是否正确
        String password = xcUser.getPassword();
        boolean matches = passwordEncoder.matches(authParamsDto.getPassword(), password);
        if (!matches) {
            throw new RuntimeException("账号或密码错误~");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser, xcUserExt);
        return xcUserExt;
    }
}
