package com.xuecheng.checkcode.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class FindPasswordParamsDto implements Serializable {
    String cellphone;
    String email;
    String checkcodekey;
    String checkcode;
    String confirmpwd;
    String password;
}
