package com.xuecheng.orders.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.msg.AlipayMsgClient;
import com.alipay.api.request.AlipayTradeCreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeCreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;

import com.xuecheng.OrdersApiApplication;
import com.xuecheng.orders.config.AlipayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest()
public class ALiPayTest {

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;
    @Value("${pay.alipay.BUYER_ID}")
    String BUYER_ID;

    @Test
    public void payTest() throws AlipayApiException {
        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi-sandbox.dl.alipaydev.com/gateway.do",APP_ID,APP_PRIVATE_KEY,"json","GBK",ALIPAY_PUBLIC_KEY,"RSA2");
        AlipayTradeCreateRequest request = new AlipayTradeCreateRequest();
        request.setNotifyUrl("http://4o9zwwl7x.shenzhuo.vip:22767/orders/paynotify");
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", "20250817010101003X04");
        bizContent.put("total_amount", 0.01);
        bizContent.put("subject", "测试商品");
        bizContent.put("buyer_id", BUYER_ID);
        bizContent.put("timeout_express", "10m");
        bizContent.put("product_code", "JSAPI_PAY");
        request.setBizContent(bizContent.toString());
        AlipayTradeCreateResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }
    }


        @Test
        public void queryPayResult() throws AlipayApiException {
            AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json", AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE); //获得初始化的AlipayClient
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", "20250817010101003X03");
            //bizContent.put("trade_no", "2014112611001004680073956707");
            request.setBizContent(bizContent.toString());
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                System.out.println("调用成功");
                String resultJson = response.getBody();
                //转map
                Map resultMap = JSON.parseObject(resultJson, Map.class);
                Map alipay_trade_query_response = (Map) resultMap.get("alipay_trade_query_response");
                //支付结果
                String trade_status = (String) alipay_trade_query_response.get("trade_status");
                System.out.println(trade_status);
            } else {
                System.out.println("调用失败");
            }
        }


    @Test
    public void websocketTest() throws Exception {

        // 目标支付宝服务端地址，线上环境为 openchannel.alipay.com
        String serverHost =  "openchannel.alipay.com" ;
        // 数据签名方式，请与应用设置的默认签名方式保持一致
        String signType =  "RSA2" ;
        // 应用私钥
        String appPrivateKey =  "your app private key" ;
        // 支付宝公钥
        String alipayPublicKey =  "alipay public key" ;
        // 获取client对象，一个appId对应一个实例
        final  AlipayMsgClient alipayMsgClient = AlipayMsgClient.getInstance(APP_ID);
        alipayMsgClient.setConnector(serverHost);
        alipayMsgClient.setSecurityConfig(signType, APP_PRIVATE_KEY, ALIPAY_PUBLIC_KEY);

        alipayMsgClient.connect();

    }

}
