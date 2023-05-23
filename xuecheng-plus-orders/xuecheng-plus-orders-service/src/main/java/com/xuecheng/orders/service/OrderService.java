package com.xuecheng.orders.service;

import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;

/**
 * @author gushouye
 * @description 订单服务接口
 **/
public interface OrderService {

    /**
     * 生成支付二维码
     *
     * @param userId      用户id
     * @param addOrderDto 订单信息
     * @return 支付记录dto
     */
    PayRecordDto createOrder(String userId, AddOrderDto addOrderDto);
}
