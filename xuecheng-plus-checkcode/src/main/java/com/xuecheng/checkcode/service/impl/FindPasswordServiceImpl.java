package com.xuecheng.checkcode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.checkcode.model.FindPasswordParamsDto;
import com.xuecheng.checkcode.service.FindPasswordService;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.po.XcUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FindPasswordServiceImpl implements FindPasswordService {
    @Autowired
    private XcUserMapper xcUserMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public RestResponse findpassword(FindPasswordParamsDto findPasswordParamsDto) {
        String checkcodekey = findPasswordParamsDto.getCheckcodekey();
        String checkcode = findPasswordParamsDto.getCheckcode();
        if (!checkcodekey.equals(checkcode)) {
            XueChengPlusException.err("验证码不一致");
        }
        String confirmpwd = findPasswordParamsDto.getConfirmpwd();
        String password = findPasswordParamsDto.getPassword();
        if (!confirmpwd.equals(password)) {
            XueChengPlusException.err("密码不一致");
        }
        String cellphone = findPasswordParamsDto.getCellphone();
        String email = findPasswordParamsDto.getEmail();

        RestResponse<Object> restResponse = new RestResponse<>();
        LambdaQueryWrapper<XcUser> xcUserLambdaQueryWrapper = new LambdaQueryWrapper<>();
        xcUserLambdaQueryWrapper.eq(XcUser::getCellphone,cellphone);
        XcUser xcUser = xcUserMapper.selectOne(xcUserLambdaQueryWrapper);
        if (xcUser == null) {
            XcUser xcUser1 = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getEmail, email));
            if (xcUser1 == null) XueChengPlusException.err("查询不到用户");
            xcUser1.setPassword(passwordEncoder.encode(password));
            xcUserMapper.updateById(xcUser1);
            restResponse.setCode(200);
            restResponse.setMsg("重新找回密码成功");
            return restResponse;
        }

        xcUser.setPassword(passwordEncoder.encode(password));
        xcUserMapper.updateById(xcUser);
        restResponse.setCode(200);
        restResponse.setMsg("重新找回密码成功");
        return restResponse;
    }
}
