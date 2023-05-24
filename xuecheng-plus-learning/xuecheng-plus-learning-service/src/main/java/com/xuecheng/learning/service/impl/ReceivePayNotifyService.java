package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author gushouye
 * @description 支付结果通知服务
 **/
@Service
@Slf4j
public class ReceivePayNotifyService {

    @Autowired
    private MyCourseTablesService courseTablesService;

    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 消息体
        byte[] body = message.getBody();
        // 发送过来的json字符串
        String msg = new String(body);
        // 转换为消息对象
        MqMessage mqMessage = JSON.parseObject(msg, MqMessage.class);
        // 解析消息内容
        String chooseCourseId = mqMessage.getBusinessKey1(); // chooseCourseId选课id
        String orderType = mqMessage.getBusinessKey2();// 订单类型
        // 判断订单类型，学习中心服务只要购买课程的订单的结果
        if (orderType.equals("60201")) {
            // 根据消息的内容，更新选课记录表，向我的课程表插入记录
            boolean b = courseTablesService.saveChooseCourseSuccess(chooseCourseId);
            if (!b){
                log.error("更新选课记录表失败，chooseCourseId：{}",chooseCourseId);
                XueChengPlusException.cast("保存选课记录表失败");
            }
        }
    }


}
