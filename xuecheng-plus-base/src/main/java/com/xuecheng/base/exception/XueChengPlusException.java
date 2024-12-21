package com.xuecheng.base.exception;

import org.springframework.web.server.ResponseStatusException;

public class XueChengPlusException extends RuntimeException{
    private String errMessage;


    public XueChengPlusException() {
        super();
    }

    public XueChengPlusException(String errMessage) {
        super(errMessage);
        this.errMessage = errMessage;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public static void err(CommonError commonError) {
        throw new XueChengPlusException(commonError.getErrMessage());
    }

    public static void err(String errMessage) {
        throw new XueChengPlusException(errMessage);
    }
}
