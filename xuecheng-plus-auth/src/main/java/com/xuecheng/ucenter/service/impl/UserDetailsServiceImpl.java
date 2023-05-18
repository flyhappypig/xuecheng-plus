package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
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

import java.util.ArrayList;
import java.util.List;

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
    @Autowired
    private XcMenuMapper xcMenuMapper;


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
        XcUserExt xcuser = applicationContext.getBean(beanName, AuthService.class).execute(authParamsDto);
        // 封装xcUser用户信息为UserDetails
        // 最终根据userDetails中的信息，生成jwt令牌
        UserDetails userPrincipal = getUserPrincipal(xcuser);
        return userPrincipal;
    }

    /**
     * @param user 用户id，主键
     * @return com.xuecheng.ucenter.model.po.XcUser 用户信息
     * @description 查询用户信息
     */
    public UserDetails getUserPrincipal(XcUserExt user) {
        String password = user.getPassword();
        // 从数据库获取权限
        String[] authorities = {"test"};
        // 根据用户id查询用户信息
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(user.getId());
        if (xcMenus.size() > 0) {
//            authorities = new String[xcMenus.size()];
//            for (int i = 0; i < xcMenus.size(); i++) {
//                authorities[i] = xcMenus.get(i).getCode();
//            }
            List<String> permissions = new ArrayList<>();
            // 拿到用户拥有的权限标识符
            xcMenus.forEach(xcMenu -> permissions.add(xcMenu.getCode()));
            // 转成数组
            authorities = permissions.toArray(new String[0]);
        }
        user.setPassword(null);
        // 用户信息转json
        String userJson = JSON.toJSONString(user);
        UserDetails userDetails = User.withUsername(userJson).password(password).authorities(authorities).build();
        return userDetails;
    }
}
