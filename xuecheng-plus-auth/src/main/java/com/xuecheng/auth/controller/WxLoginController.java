package com.xuecheng.auth.controller;

import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
public class WxLoginController {
    @Autowired
    private WxAuthService wxAuthService;

    @RequestMapping("/wxLogin")
    public String wxLogin(String code,String state) {
        log.debug("微信回调，code：{}，state:{}", code, state);

        XcUser xcUser = wxAuthService.wxAuth(code);
        if (xcUser == null) {
            return "redirect:http://www.51xuecheng.cn/error.html";
        }

        String username = xcUser.getUsername();

        return "redirect:http://www.51xuecheng.cn/sign.html?username="+username+"&authType=wx";
    }
}


