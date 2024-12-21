package com.xuecheng;

import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AuthTest {
    @Autowired
    private CheckCodeClient checkCodeClient;

    @Test
    public void testCheckCodeClient() {
        checkCodeClient.verify("dfdf","dfdf");
    }
}
