package com.atguigu.daijia.common.login;

import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.Claims;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import lombok.experimental.Accessors;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.condition.RequestConditionHolder;

@Component
@Aspect
public class GuiguLoginAspect {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Around("execution(* com.atguigu.daijia.*.controller.*.*(..)) && @annotation(guiguLogin)")
     public Object around(ProceedingJoinPoint joinPoint, GuiguLogin guiguLogin) throws Throwable {
        //1、获取token
        String token = null;
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) attributes;
        if (sra != null) {
            token = sra.getRequest().getHeader("token");
        }

        //2、判断token是否为空，如果为空，返回错误信息
        if (token == null || StrUtil.isBlank(token)) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        //3、根据token从redis中获取userId
        String userId = stringRedisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
        //4、判断userId是否为空，如果为空，返回错误信息
        if(userId==null||StrUtil.isBlank(userId)){
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        //5、将userId存入ThreadLocal中
        AuthContextHolder.setUserId(Long.parseLong(userId));

        //6、执行目标方法
        return joinPoint.proceed();
     }
}
