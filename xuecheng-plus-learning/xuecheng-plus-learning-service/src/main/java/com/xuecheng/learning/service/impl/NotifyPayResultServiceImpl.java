package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.service.MyCourseTableService;
import com.xuecheng.learning.service.NotifyPayResultService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.channels.Channel;

@Service
@Slf4j
public class NotifyPayResultServiceImpl implements NotifyPayResultService {

    @Autowired
    private MyCourseTableService myCourseTableService;
    @Autowired
    private MqMessageService mqMessageService;

    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    @Override
    public void receive(Message message) {
        MqMessage mqMessage = JSON.parseObject(message.getBody(), MqMessage.class);
        log.info("接收"+PayNotifyConfig.PAYNOTIFY_QUEUE+"队列消息成功：{}",mqMessage.toString());

        String messageType = mqMessage.getMessageType();
        String businessKey2 = mqMessage.getBusinessKey2();

        if (PayNotifyConfig.MESSAGE_TYPE.equals(messageType) && businessKey2.equals("60201")) {
            String chooseCourseId = mqMessage.getBusinessKey1();
            boolean b = myCourseTableService.saveChooseCourseSuccess(chooseCourseId);
            if (b) {
                log.info("接收消息后，添加选课记录表成功：{}",chooseCourseId);
                //删除数据库消息表；
//                mqMessageService.completed(mqMessage.getId());
            } else {
                XueChengPlusException.err("收到支付结果，添加选课失败");
            }

        }

    }
}
