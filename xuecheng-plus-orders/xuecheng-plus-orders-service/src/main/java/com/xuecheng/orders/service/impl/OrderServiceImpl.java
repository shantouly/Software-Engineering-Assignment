package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.JsonObject;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.config.PayNotifyConfig;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.SuccessCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private XcOrdersMapper xcOrdersMapper;
    @Autowired
    private XcOrdersGoodsMapper xcOrdersGoodsMapper;
    @Autowired
    private XcPayRecordMapper xcPayRecordMapper;
    @Autowired
    private OrderService currencyProxy;
    @Autowired
    private MqMessageService mqMessageService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${pay.qrcodeurl}")
    private String qrcodeUrl;
    @Value("${pay.alipay.APP_ID}")
    private String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    private String APP_PRIVATE_KEY;
    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    private String ALIPAY_PUBLIC_KEY;

    @Override
    public PayRecordDto createOrders(String userId, AddOrderDto addOrderDto) {
        //生成订单
        XcOrders orders = savaXcOrders(userId, addOrderDto);
        if (orders == null) {
            XueChengPlusException.err("创建订单失败");
        }
        //生成支付记录
        XcPayRecord payRecord = createPayRecord(orders);
        //生成二维码
        String qrCode = null;
        String url = String.format(qrcodeUrl,payRecord.getPayNo());
        try {
            qrCode = new QRCodeUtil().createQRCode(url, 200, 200);
        } catch (IOException e) {
            XueChengPlusException.err("生成二维码错误");
            throw new RuntimeException(e);
        }

        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord,payRecordDto);
        payRecordDto.setQrcode(qrCode);
        return payRecordDto;
    }

    @Override
    public XcPayRecord getPayRecord(String payNo) {
        XcPayRecord xcPayRecord = xcPayRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>()
                .eq(XcPayRecord::getPayNo, payNo));
        return xcPayRecord;
    }

    @Override
    public PayRecordDto queryPayResult(String payNo) {
        XcPayRecord payRecord = getPayRecord(payNo);
        if (payRecord == null) {
            XueChengPlusException.err("请重新点击支付获取二维码");
        }
        if (payRecord.getStatus().equals("601002")) {
            PayRecordDto payRecordDto = new PayRecordDto();
            BeanUtils.copyProperties(payRecord,payRecordDto);
            return payRecordDto;
        }
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);
        if (payStatusDto == null) {
            XueChengPlusException.err("在支付宝查询不到流水号相关支付记录");
        }
        currencyProxy.saveAliPayStatus(payStatusDto);

        payRecord = getPayRecord(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord,payRecordDto);

        return payRecordDto;
    }

    public PayStatusDto queryPayResultFromAlipay(String payNo) {
        DefaultAlipayClient alipayClient = new DefaultAlipayClient
                (AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json",
                        AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE);

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JsonObject bizContent = new JsonObject();
        bizContent.addProperty("out_trade_no",payNo);
        request.setBizContent(bizContent.toString());

        AlipayTradeQueryResponse response = null;

        try {
            response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                XueChengPlusException.err("请求支付查询失败");
            }
        } catch (AlipayApiException e) {
            log.info("查询支付结果异常：{}",e.toString());
            throw new RuntimeException(e);
        }

        String body = response.getBody();
        Map map = JSON.parseObject(body, Map.class);
        Map alipay_trade_query_response =  (Map)map.get("alipay_trade_query_response");

        String trade_status =  (String) alipay_trade_query_response.get("trade_status");
        String total_amount = (String) alipay_trade_query_response.get("total_amount");
        String trade_no = (String) alipay_trade_query_response.get("trade_no");
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_status(trade_status);
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTrade_no(trade_no);
        payStatusDto.setTotal_amount(total_amount);
        return payStatusDto;
    }

    @Transactional
    public void saveAliPayStatus(PayStatusDto payStatusDto) {

        String payNo = payStatusDto.getOut_trade_no();
        XcPayRecord payRecord = getPayRecord(payNo);
        if (payRecord == null) {
            XueChengPlusException.err("支付记录找不到");
        }

        String tradeStatus = payStatusDto.getTrade_status();
        log.info("收到支付结果：{}，支付记录：{}",tradeStatus,payStatusDto.toString());

        if (tradeStatus.equals("TRADE_SUCCESS")) {
            Float totalPrice = payRecord.getTotalPrice()*100;

            Float totalAmount = Float.parseFloat(payStatusDto.getTotal_amount()) * 100;
            if (totalPrice.intValue() != totalAmount.intValue()) {
                XueChengPlusException.err("校验支付结果失败");

            }

            log.debug("更新支付结果，交易流水号：{}，支付结果：{}",payNo,tradeStatus);
            XcPayRecord xcPayRecord = new XcPayRecord();
            xcPayRecord.setStatus("601002");
            xcPayRecord.setOutPayChannel("Alipay");
            xcPayRecord.setOutPayNo(payStatusDto.getOut_trade_no());
            xcPayRecord.setPaySuccessTime(LocalDateTime.now());

            int update = xcPayRecordMapper.update(xcPayRecord, new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));

            if (update > 0) {
                log.info("更新支付记录成功：{}",payNo);
            } else {
                log.info("更新支付记录失败：{}",payNo);
            }

            Long orderId = payRecord.getOrderId();
            XcOrders xcOrders = xcOrdersMapper.selectById(orderId);
            if (xcOrders == null) {
                XueChengPlusException.err("根据支付记录找不到订单");
            }

            XcOrders xcOrders_new = new XcOrders();
            xcOrders_new.setStatus("600002");
            int update1 = xcOrdersMapper.update(xcOrders_new, new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getId, orderId));
            if (update1 > 0) {
                log.info("更新订单状态成功：{}",orderId);
            } else {
                log.error("更新订单状态失败：{}",orderId);
            }

         //保存消息记录
            MqMessage mqMessage = mqMessageService.addMessage("payresult_notify",
                    xcOrders.getOutBusinessId(), xcOrders.getOrderType(), null);
            notifyPayResult(mqMessage);
        }
    }

    @Override
    public void notifyPayResult(MqMessage mqMessage) {
        String msg = JSON.toJSONString(mqMessage);
        Message msgObj = MessageBuilder.withBody(msg.getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)

                .build();

        CorrelationData correlationData = new CorrelationData(mqMessage.getId().toString());
        correlationData.getFuture().addCallback(
                result -> {
                    if(result.isAck()){
                        // 3.1.ack，消息成功
                        log.debug("通知支付结果消息发送成功, ID:{}", correlationData.getId());
                        //删除消息表中的记录
                        mqMessageService.completed(mqMessage.getId());
                    }else{
                        // 3.2.nack，消息失败
                        log.error("通知支付结果消息发送失败, ID:{}, 原因{}",correlationData.getId(), result.getReason());
                    }
                },
                ex -> log.error("消息发送异常, ID:{}, 原因{}",correlationData.getId(),ex.getMessage())
        );

        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT,"",msgObj,correlationData);

    }

    public XcOrders savaXcOrders(String userId,AddOrderDto addOrderDto) {
        XcOrders orders = getXcOrdersByBusinessId(addOrderDto.getOutBusinessId());
        if (orders != null) {
            return orders;
        }

        orders = new XcOrders();
        //生成订单号
        long orderId = IdWorkerUtils.getInstance().nextId();
        orders.setId(orderId);
        orders.setTotalPrice(addOrderDto.getTotalPrice());
        orders.setCreateDate(LocalDateTime.now());
        orders.setStatus("600001");
        orders.setUserId(userId);
        orders.setOrderType(addOrderDto.getOrderType());
        orders.setOrderName(addOrderDto.getOrderName());
        orders.setOrderDetail(addOrderDto.getOrderDetail());
        orders.setOrderDescrip(addOrderDto.getOrderDescrip());
        orders.setOutBusinessId(addOrderDto.getOutBusinessId());
        xcOrdersMapper.insert(orders);

        String orderDetail = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoodsList = JSON.parseArray(orderDetail, XcOrdersGoods.class);

        xcOrdersGoodsList.forEach(goods -> {
            XcOrdersGoods xcOrdersGoods = new XcOrdersGoods();
            BeanUtils.copyProperties(goods,xcOrdersGoods);
            xcOrdersGoods.setId(orderId);
            xcOrdersGoods.setOrderId(orderId);
            xcOrdersGoodsMapper.insert(xcOrdersGoods);
        });
        return orders;
    }

    private XcOrders getXcOrdersByBusinessId(String businessId) {
        XcOrders xcOrders = xcOrdersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
        return xcOrders;
    }

    public XcPayRecord createPayRecord(XcOrders orders) {
        if (orders == null) {
            XueChengPlusException.err("订单不存在");
        }

        if (orders.getStatus().equals("600002")) {
            XueChengPlusException.err("订单已支付");
        }

        XcPayRecord xcPayRecord = new XcPayRecord();
        long payNo = IdWorkerUtils.getInstance().nextId();
        xcPayRecord.setPayNo(payNo);
        xcPayRecord.setOrderId(orders.getId());
        xcPayRecord.setOrderName(orders.getOrderName());
        xcPayRecord.setTotalPrice(orders.getTotalPrice());
        xcPayRecord.setCurrency("CNY");
        xcPayRecord.setCreateDate(LocalDateTime.now());
        xcPayRecord.setStatus("601001");
        xcPayRecord.setUserId(orders.getUserId());
        xcPayRecordMapper.insert(xcPayRecord);
        return xcPayRecord;
    }
}
