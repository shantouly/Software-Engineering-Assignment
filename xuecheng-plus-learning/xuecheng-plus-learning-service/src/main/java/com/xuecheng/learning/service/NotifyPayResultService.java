package com.xuecheng.learning.service;

import org.springframework.amqp.core.Message;

import java.nio.channels.Channel;

public interface NotifyPayResultService {
    public void receive(Message message);
}
