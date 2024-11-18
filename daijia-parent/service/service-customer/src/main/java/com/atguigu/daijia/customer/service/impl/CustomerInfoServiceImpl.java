package com.atguigu.daijia.customer.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.mapper.CustomerInfoMapper;
import com.atguigu.daijia.customer.mapper.CustomerLoginLogMapper;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.entity.customer.CustomerLoginLog;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {

    @Autowired
    private WxMaService wxMaService;
    @Autowired
    private CustomerInfoMapper customerInfoMapper;
    @Autowired
    private CustomerLoginLogMapper customerLoginLogMapper;

    //小程序授权登录,获取用户的openId
    @Override
    public Long login(String code) {
        //1、根据code获取微信用户的openId
        String opedId = null;
        try {
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            opedId = sessionInfo.getOpenid();
        } catch (WxErrorException e) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        //2、根据openId查询用户是否存在
        LambdaQueryWrapper<CustomerInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomerInfo::getWxOpenId, opedId);
        CustomerInfo customerInfo = customerInfoMapper.selectOne(queryWrapper);

        //3、如果用户不存在，则注册用户
        if (customerInfo == null) {
            customerInfo = new CustomerInfo();
            customerInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            customerInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            customerInfo.setWxOpenId(opedId);
            customerInfoMapper.insert(customerInfo);
        }

        //4、更新用户的登录时间
        CustomerLoginLog customerLoginLog = new CustomerLoginLog();
        customerLoginLog.setCustomerId(customerInfo.getId());
        customerLoginLog.setMsg("小程序登录");
        customerLoginLogMapper.insert(customerLoginLog);

        //5、返回用户的id
        return customerInfo.getId();
    }

    //获取客户登录信息
    @Override
    public CustomerLoginVo getCustomerInfo(Long customerId) {
        //1、根据customerId查询用户信息
        CustomerInfo customerInfo = customerInfoMapper.selectById(customerId);
        if(customerInfo == null){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        //2、将用户信息转换为CustomerLoginVo对象
        CustomerLoginVo customerLoginVo = BeanUtil.copyProperties(customerInfo, CustomerLoginVo.class);

        //3、判断是否绑定手机号
        boolean isBindPhone = StrUtil.isNotBlank(customerInfo.getPhone());
        customerLoginVo.setIsBindPhone(isBindPhone);

        //4、返回CustomerLoginVo对象
        return customerLoginVo;
    }

    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
//        // 调用微信 API 获取用户的手机号
//        WxMaPhoneNumberInfo phoneInfo = null;
//        try {
//            phoneInfo = wxMaService.getUserService().getPhoneNoInfo(updateWxPhoneForm.getCode());
//        } catch (WxErrorException e) {
//            throw new RuntimeException(e);
//        }
//        String phoneNumber = phoneInfo.getPhoneNumber();
//        log.info("phoneInfo:{}", JSONUtil.toJsonStr(phoneInfo));

        String phoneNumber = "13776840962";
        CustomerInfo customerInfo = new CustomerInfo();
        customerInfo.setId(updateWxPhoneForm.getCustomerId());
        customerInfo.setPhone(phoneNumber);
        return this.updateById(customerInfo);
    }
}
