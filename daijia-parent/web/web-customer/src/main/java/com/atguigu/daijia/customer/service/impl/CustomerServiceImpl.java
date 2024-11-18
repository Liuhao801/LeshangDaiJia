package com.atguigu.daijia.customer.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerServiceImpl implements CustomerService {
    @Autowired
    private CustomerInfoFeignClient customerInfoFeignClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String login(String code) {
        //1、远程调用接口，获取用户openid
        Result<Long> loginResult = customerInfoFeignClient.login(code);

        //2、判断接口调用是否成功
        Integer resultCode = loginResult.getCode();
        if (resultCode != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        //3、判断openid是否为空，如果为空，返回错误信息
        Long userId = loginResult.getData();
        if (userId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        //4、生成token，将token和userId存入redis,并设置过期时间
        String token = UUID.randomUUID().toString(true);
        stringRedisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX+token,
                                                    userId.toString(),
                                                    RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                                                    TimeUnit.SECONDS);
        //5、返回token
        return token;
    }

    @Override
    public CustomerLoginVo getCustomerLoginInfo(Long userId) {

        //3、远程调用接口，获取用户信息
        Result<CustomerLoginVo> customerLoginInfo = customerInfoFeignClient.getCustomerLoginInfo(userId);

        //4、判断接口调用是否成功
        if(customerLoginInfo.getCode()!=200){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        //5、返回用户信息
        CustomerLoginVo customerLoginVo = customerLoginInfo.getData();
        if(customerLoginVo==null){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return customerLoginVo;
    }

    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        customerInfoFeignClient.updateWxPhoneNumber(updateWxPhoneForm);
        return true;
    }
}
