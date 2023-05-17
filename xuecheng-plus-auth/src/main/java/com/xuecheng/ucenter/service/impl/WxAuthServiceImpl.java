package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcRoleMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcRole;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import sun.net.www.protocol.http.HttpURLConnection;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * @author gushouye
 * @description 微信扫码认证
 **/

@Service("wx_authService")
@Slf4j
public class WxAuthServiceImpl implements AuthService, WxAuthService {

    @Autowired
    private XcUserMapper xcUserMapper;
    @Value("${wx.appid}")
    private String appid;
    @Value("${wx.appsecret}")
    private String appsecret;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private XcUserRoleMapper xcUserRoleMapper;
    @Autowired
    private WxAuthServiceImpl proxy;


    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        String username = authParamsDto.getUsername();
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if (xcUser == null) {
            throw new RuntimeException("用户不存在");
        }
        log.info("微信扫码认证");
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser, xcUserExt);
        return xcUserExt;
    }

    @Override
    public XcUser wxAuth(String code) {
//        申请令牌，携带令牌查询用户信息，保存用户信息到数据库
        // 申请令牌
        Map<String, String> access_token = getAccess_token(code);
        // 携带令牌查询用户信息
        Map<String, String> userinfo = getUserinfo(access_token.get("access_token"), access_token.get("openid"));
        // 保存用户信息到数据库
        XcUser xcUser = proxy.addWxUser(userinfo);
        return xcUser;
    }

    /**
     * 申请访问令牌,响应示例
     {
     "access_token":"ACCESS_TOKEN",
     "expires_in":7200,
     "refresh_token":"REFRESH_TOKEN",
     "openid":"OPENID",
     "scope":"SCOPE",
     "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
     }
     */
    /**
     * 申请令牌
     *
     * @param code 微信授权码
     * @return
     */
    private Map<String, String> getAccess_token(String code) {
        // 拼接url
        String wxUrl_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        String wxUrl = String.format(wxUrl_template, appid, appsecret, code);
        // 发送请求
        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.GET, null, String.class);
        // 解析响应
        String body = exchange.getBody();
        log.info("微信响应:{}", body);
        // 将body转为map
        Map<String, String> map = JSON.parseObject(body, Map.class);
        return map;
    }

    /**
     * 获取用户信息，示例如下：
     * {
     * "openid":"OPENID",
     * "nickname":"NICKNAME",
     * "sex":1,
     * "province":"PROVINCE",
     * "city":"CITY",
     * "country":"COUNTRY",
     * "headimgurl": "https://thirdwx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
     * "privilege":[
     * "PRIVILEGE1",
     * "PRIVILEGE2"
     * ],
     * "unionid": " o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * }
     */
    private Map<String, String> getUserinfo(String access_token, String openid) {

        String wxUrl_template = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        //请求微信地址
        String wxUrl = String.format(wxUrl_template, access_token, openid);
        //发送请求
        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.GET, null, String.class);
        //解析响应,微信返回的数据是ISO-8859-1编码，需要转换为utf-8
        String body = new String(exchange.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        log.info("微信响应:{}", body);
        //将body转为map
        Map<String, String> map = JSON.parseObject(body, Map.class);
        return map;
    }

    @Transactional
    public XcUser addWxUser(Map<String, String> userInfo_map) {
        // 根据unionid查询用户信息
        String unionid = userInfo_map.get("unionid");
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        if (xcUser != null) {
            return xcUser;
        }
        // 向数据库新增用户信息
        xcUser = new XcUser();
        xcUser.setId(UUID.randomUUID().toString());// 主键
        xcUser.setUsername(unionid);
        xcUser.setWxUnionid(unionid);
        xcUser.setPassword(unionid);
        xcUser.setNickname(userInfo_map.get("nickname"));
        xcUser.setName(userInfo_map.get("nickname"));
        xcUser.setUtype("101001");// 学生类型
        xcUser.setStatus("1");// 用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        // 插入
        xcUserMapper.insert(xcUser);

        // 向用户角色关系表中插入数据
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(xcUser.getId());
        xcUserRole.setRoleId("17");// 学生角色
        xcUserRole.setCreateTime(LocalDateTime.now());
        // 插入
        xcUserRoleMapper.insert(xcUserRole);
        return xcUser;
    }
}
