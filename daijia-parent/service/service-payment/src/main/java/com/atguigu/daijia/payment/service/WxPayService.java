package com.atguigu.daijia.payment.service;

import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import jakarta.servlet.http.HttpServletRequest;

public interface WxPayService {

    //创建微信支付
    WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm);

    //微信支付异步通知接口
    void wxnotify(HttpServletRequest request);

    //支付状态查询
    Boolean queryPayStatus(String orderNo);

    void handleOrder(String orderNo);
}
