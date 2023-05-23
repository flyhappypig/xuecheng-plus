package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.GoodsDetail;
import com.alipay.api.domain.OrderDetail;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
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
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author gushouye
 * @description 订单服务实现类
 **/
@Service
public class OrderServiceImpl implements OrderService {

    //http://172.16.5.102:63030/orders/alipaytest?payNo=%S
    @Value("${pay.qrCodeUrl}")
    private String qrCodeUrl;
    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;
    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    @Autowired
    private XcOrdersMapper ordersMapper;
    @Autowired
    private XcOrdersGoodsMapper ordersGoodsService;
    @Autowired
    private XcPayRecordMapper payRecordMapper;
    @Autowired
    private OrderServiceImpl orderService;


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

    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        XcPayRecord payRecord = payRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
        return payRecord;
    }

    @Override
    public PayRecordDto queryPayResult(String payNo) {
        // 调用支付宝接口查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);
        // 拿到支付结果后，更新支付记录表和订单表的支付状态
        orderService.saveAliPayStatus(payStatusDto);
        // 返回最新的支付记录信息
        XcPayRecord payRecord = orderService.getPayRecordByPayno(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        return payRecordDto;
    }

    /**
     * 请求支付宝查询支付结果
     *
     * @param payNo 支付交易号
     * @return 支付结果
     */
    public PayStatusDto queryPayResultFromAlipay(String payNo) {
        // 调用支付宝接口查询支付结果

        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, AlipayConfig.FORMAT, AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE); //获得初始化的AlipayClient
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();// 创建API对应的request类
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);// out_trade_no和trade_no二选一
        request.setBizContent(bizContent.toString());
        String body = null;
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);// 通过alipayClient调用API，获得对应的response类
            if (!response.isSuccess()) {
                // 交易不成功
                XueChengPlusException.cast("请求支付宝查询支付结果失败");
            }
            body = response.getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
            XueChengPlusException.cast("调用支付宝查询支付结果失败");
        }
        // 解析支付宝返回的结果
        Map map = JSONObject.parseObject(body, Map.class);
        Map respMap = (Map) map.get("alipay_trade_query_response");
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_no((String) respMap.get("trade_no"));// 支付宝交易号
        payStatusDto.setTotal_amount((String) respMap.get("total_amount")); // 交易金额
        payStatusDto.setTrade_status((String) respMap.get("trade_status")); // 交易状态
        payStatusDto.setApp_id(APP_ID); // 应用id
        return payStatusDto;
    }

    /**
     * @param payStatusDto 支付结果信息，是从支付宝查询支付结果返回的
     * @description 保存支付宝支付结果
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveAliPayStatus(PayStatusDto payStatusDto) {

        // 支付交易号
        String payNo = payStatusDto.getOut_trade_no();
        XcPayRecord payRecordByPayno = getPayRecordByPayno(payNo);
        if (payRecordByPayno == null) {
            XueChengPlusException.cast("支付记录不存在");
        }
        // 拿到相关的订单id
        Long orderId = payRecordByPayno.getOrderId();
        XcOrders xcOrders = ordersMapper.selectById(orderId);
        if (xcOrders == null) {
            XueChengPlusException.cast("订单不存在");
        }
        // 支付状态
        String statusFrom = payRecordByPayno.getStatus();
        // 如果数据库支付状态为支付成功，直接返回
        if (statusFrom.equals("601002")) {
            // 已经支付成功了
            return;
        }
        // 如果支付成功
        String trade_status = payStatusDto.getTrade_status();// 从支付宝查询支付结果返回的支付状态
        if (trade_status.equals("TRADE_SUCCESS")) {
            // 更新支付记录表的支付状态为支付成功
            payRecordByPayno.setStatus("601002");
            payRecordByPayno.setOutPayNo(payStatusDto.getTrade_no());// 支付宝订单号
            payRecordByPayno.setOutPayChannel("alipay");// 第三方支付渠道
            payRecordByPayno.setPaySuccessTime(LocalDateTime.now());
            payRecordMapper.updateById(payRecordByPayno);
            // 更新订单表的支付状态为支付成功
            xcOrders.setStatus("600002");
            ordersMapper.updateById(xcOrders);
            // 发送消息给mq，完成添加选课记录
        }
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
