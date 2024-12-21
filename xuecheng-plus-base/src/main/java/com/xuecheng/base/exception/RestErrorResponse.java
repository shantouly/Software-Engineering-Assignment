package com.xuecheng.base.exception;

import lombok.Data;

import java.io.Serializable;

@Data
public class RestErrorResponse implements Serializable {
    private String errMessage;

//    private String code;

    public RestErrorResponse(String errMessage) {
        this.errMessage = errMessage;
    }

//    public RestErrorResponse(String errMessage,String code) {
//        this.errMessage = errMessage;
//        this.code = code;
//    }
}
