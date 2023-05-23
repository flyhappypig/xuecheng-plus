package com.xuecheng.orders.service;

import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.po.XcPayRecord;

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


    /**
     * @description 查询支付记录
     * @param payNo  交易记录号
     * @return com.xuecheng.orders.model.po.XcPayRecord
     * @author Mr.M
     * @date 2022/10/20 23:38
     */
    public XcPayRecord getPayRecordByPayno(String payNo);

    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付记录id
     * @return 支付记录信息
     */
    public PayRecordDto queryPayResult(String payNo);
}
