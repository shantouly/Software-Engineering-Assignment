package com.xuecheng.checkcode.service;


import com.xuecheng.base.model.RestResponse;
import com.xuecheng.checkcode.model.FindPasswordParamsDto;

public interface FindPasswordService {
    public RestResponse findpassword(FindPasswordParamsDto findPasswordParamsDto);
}
