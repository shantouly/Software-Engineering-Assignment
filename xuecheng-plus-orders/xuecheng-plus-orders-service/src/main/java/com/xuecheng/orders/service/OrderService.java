package com.xuecheng.orders.service;

import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcPayRecord;

public interface OrderService {
    public PayRecordDto createOrders(String userId, AddOrderDto addOrderDto);

    public XcPayRecord getPayRecord(String payNo);

    public PayRecordDto queryPayResult(String payNo);
    public void saveAliPayStatus(PayStatusDto payStatusDto);

    public void notifyPayResult(MqMessage mqMessage);

}
