package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.domain.GoodsDetail;
import com.alipay.api.domain.OrderDetail;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author gushouye
 * @description 订单服务实现类
 **/
@Service
public class OrderServiceImpl implements OrderService {

    //http://172.16.5.102:63030/orders/alipaytest?payNo=%S
    @Value("${pay.qrCodeUrl}")
    private String qrCodeUrl;

    @Autowired
    private XcOrdersMapper ordersMapper;
    @Autowired
    private XcOrdersGoodsMapper ordersGoodsService;
    @Autowired
    private XcPayRecordMapper payRecordMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {
        // 1.插入订单信息,订单主表，订单明细表
        XcOrders xcOrders = saveXcOrders(userId, addOrderDto);
        // 2.插入支付记录信息
        XcPayRecord payRecord = createPayRecord(xcOrders);
        Long payNo = payRecord.getPayNo();
        String qrCode = "";
        // 3.生成支付二维码
        QRCodeUtil qrCodeUtil = new QRCodeUtil();
        // 支付二维码的url
        String url = String.format(qrCodeUrl, payNo);
        try {
            qrCode = qrCodeUtil.createQRCode(url, 200, 200);
        } catch (IOException e) {
            XueChengPlusException.cast("生成支付二维码失败");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        payRecordDto.setQrcode(qrCode);
        return payRecordDto;
    }

    /**
     * 保存订单信息
     *
     * @param userId      用户id
     * @param addOrderDto 订单信息
     * @return
     */
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto) {
        // 1.插入订单信息,订单主表，订单明细表
        // 幂等性校验,同一个选课记录，只能有一条订单记录
        XcOrders orders = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if (orders != null) {
            return orders;
        }
        // 订单主表
        orders = new XcOrders();
        // 使用雪花算法生成订单id
        orders.setId(IdWorkerUtils.getInstance().nextId());
        orders.setTotalPrice(addOrderDto.getTotalPrice());
        orders.setUserId(userId);
        orders.setCreateDate(LocalDateTime.now());
        orders.setStatus("600001");// 未支付
        orders.setOrderType("60201");// 订单类型：购买课程
        orders.setOrderName(addOrderDto.getOrderName());
        orders.setOrderDescrip(addOrderDto.getOrderDescrip());
        orders.setOrderDetail(addOrderDto.getOrderDetail());
        orders.setOutBusinessId(addOrderDto.getOutBusinessId());// 如果是选课就记录了选课表的主键id
        int insert = ordersMapper.insert(orders);
        if (insert <= 0) {
            XueChengPlusException.cast("保存订单信息失败");
        }
        // 订单明细表
        // 将前端传入的明细的json字符串转换为对象
        List<XcOrdersGoods> xcOrdersGoods = JSON.parseArray(addOrderDto.getOrderDetail(), XcOrdersGoods.class);
        // 遍历订单明细，保存到数据库
        for (XcOrdersGoods xcOrdersGood : xcOrdersGoods) {
            xcOrdersGood.setOrderId(orders.getId());
            int result = ordersGoodsService.insert(xcOrdersGood);
            if (result <= 0) {
                XueChengPlusException.cast("保存订单明细信息失败");
            }
        }
        return orders;
    }

    /**
     * 根据业务id查询订单,业务id是选课记录表的主键
     *
     * @param businessId
     * @return
     */
    public XcOrders getOrderByBusinessId(String businessId) {
        XcOrders orders = ordersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
        return orders;
    }

    /**
     * 生成支付记录
     *
     * @param orders 订单信息
     * @return
     */
    public XcPayRecord createPayRecord(XcOrders orders) {
        Long id = orders.getId();
        XcOrders xcOrders = ordersMapper.selectById(id);
        // 如果订单已经支付，不再生成支付记录
        if (xcOrders != null) {
            XueChengPlusException.cast("订单不存在");
        }
        String status = orders.getStatus();
        // 如果此订单支付结果为成功，不再生成支付记录，避免重复支付
        if ("601002".equals(status)) {
            XueChengPlusException.cast("订单已支付");
        }
        // 生成支付记录
        XcPayRecord payRecord = new XcPayRecord();
        payRecord.setPayNo(IdWorkerUtils.getInstance().nextId());// 支付记录号，将来要传给支付宝
        payRecord.setOrderId(id);
        payRecord.setOrderName(orders.getOrderName());
        payRecord.setTotalPrice(orders.getTotalPrice());
        payRecord.setCurrency("CNY");
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setStatus("601001");// 支付状态：未支付
        payRecord.setUserId(orders.getUserId());
        int insert = payRecordMapper.insert(payRecord);
        if (insert <= 0) {
            XueChengPlusException.cast("生成支付记录失败");
        }
        return payRecord;
    }
}
