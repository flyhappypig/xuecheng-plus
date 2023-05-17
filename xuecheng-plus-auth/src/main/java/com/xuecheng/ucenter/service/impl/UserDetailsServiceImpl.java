package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @author gushouye
 * @description
 **/
@Slf4j
@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private XcUserMapper xcUserMapper;
    @Autowired
    private ApplicationContext applicationContext;


    // 传入的请求认证的参数就是AuthParamsDto模型类
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        // 将传入的json字符串转换为AuthParamsDto对象
        AuthParamsDto authParamsDto = null;
        try {
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("请求认证参数不符合要求~");
        }
        // 认证类型
        String authType = authParamsDto.getAuthType();
        // 根据认证类型，调用不同的认证方法
        String beanName = authType + "_authService";
        applicationContext.getBean(beanName, AuthService.class).execute(authParamsDto);
        // 账号
        String username = authParamsDto.getUsername();
        // 根据用户名查询数据库用户信息
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        // 查询到用户不存在，返回null即可，spring security会抛出异常用户不存在
        if (xcUser == null) {
            return null;
        }
        // 查询到用户存在，拿到正确的密码，最终封装成UserDetails返回给spring security，由框架进行密码比对
        String password = xcUser.getPassword();
        // 从数据库获取权限
        String[] permissions = {"test"};
        xcUser.setPassword(null);
        // 用户信息转json
        String userJson = JSON.toJSONString(xcUser);
        UserDetails userDetails = User.withUsername(userJson).password(password).authorities(permissions).build();
        return userDetails;
    }
}
