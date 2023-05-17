package com.xuecheng.ucenter.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.po.XcUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        // 账号
        String username = s;
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
