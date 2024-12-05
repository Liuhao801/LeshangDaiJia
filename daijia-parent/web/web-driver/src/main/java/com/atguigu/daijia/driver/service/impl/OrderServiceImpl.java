package com.atguigu.daijia.driver.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.LocationUtil;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderFeeForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.form.rules.ProfitsharingRuleRequestForm;
import com.atguigu.daijia.model.form.rules.RewardRuleRequestForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponseVo;
import com.atguigu.daijia.model.vo.rules.RewardRuleResponseVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import com.atguigu.daijia.rules.client.ProfitsharingRuleFeignClient;
import com.atguigu.daijia.rules.client.RewardRuleFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @Autowired
    private NewOrderFeignClient newOrderFeignClient;

    @Autowired
    private MapFeignClient mapFeignClient;

    @Autowired
    private LocationFeignClient locationFeignClient;

    @Autowired
    private FeeRuleFeignClient feeRuleFeignClient;

    @Autowired
    private RewardRuleFeignClient rewardRuleFeignClient;

    @Autowired
    private ProfitsharingRuleFeignClient profitsharingRuleFeignClient;


    @Override
    public Integer getOrderStatus(Long orderId) {
        return orderInfoFeignClient.getOrderStatus(orderId).getData();
    }

    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        return newOrderFeignClient.findNewOrderQueueData(driverId).getData();
    }

    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        return orderInfoFeignClient.robNewOrder(driverId,orderId).getData();
    }

    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        return orderInfoFeignClient.searchCustomerCurrentOrder(customerId).getData();
    }

    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        return orderInfoFeignClient.searchDriverCurrentOrder(driverId).getData();
    }

    @Override
    public OrderInfoVo getOrderInfo(Long orderId, Long driverId) {
        //订单信息
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
        if(orderInfo.getDriverId().longValue() != driverId.longValue()) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        //封装订单信息
        OrderInfoVo orderInfoVo = BeanUtil.copyProperties(orderInfo, OrderInfoVo.class);
        orderInfoVo.setOrderId(orderId);
        return orderInfoVo;
    }

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        return mapFeignClient.calculateDrivingLine(calculateDrivingLineForm).getData();
    }

    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        //防止刷单，计算司机的经纬度与代驾的起始经纬度是否在1公里范围内
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
        OrderLocationVo orderLocationVo = locationFeignClient.getCacheOrderLocation(orderId).getData();
        //司机的位置与代驾起始点位置的距离
        double distance = LocationUtil.getDistance(orderInfo.getStartPointLatitude().doubleValue(), orderInfo.getStartPointLongitude().doubleValue(), orderLocationVo.getLatitude().doubleValue(), orderLocationVo.getLongitude().doubleValue());
        if(distance > SystemConstant.DRIVER_START_LOCATION_DISTION) {
            throw new GuiguException(ResultCodeEnum.DRIVER_START_LOCATION_DISTION_ERROR);
        }

        return orderInfoFeignClient.driverArriveStartLocation(orderId, driverId).getData();
    }

    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        return orderInfoFeignClient.updateOrderCart(updateOrderCartForm).getData();
    }

    @Override
    public Boolean startDrive(StartDriveForm startDriveForm) {
        return orderInfoFeignClient.startDrive(startDriveForm).getData();
    }

    @Override
    public Boolean endDrive(OrderFeeForm orderFeeForm) {
        //1.获取订单信息
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderFeeForm.getOrderId()).getData();
        if(orderInfo.getDriverId().longValue() != orderFeeForm.getDriverId().longValue()) {
            throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
        }

        //2.防止刷单，计算司机的经纬度与代驾的终点经纬度是否在2公里范围内
        OrderServiceLastLocationVo orderServiceLastLocationVo = locationFeignClient.getOrderServiceLastLocation(orderFeeForm.getOrderId()).getData();
        //司机的位置与代驾终点位置的距离
        double distance = LocationUtil.getDistance(orderInfo.getEndPointLatitude().doubleValue(), orderInfo.getEndPointLongitude().doubleValue(), orderServiceLastLocationVo.getLatitude().doubleValue(), orderServiceLastLocationVo.getLongitude().doubleValue());
        if(distance > SystemConstant.DRIVER_END_LOCATION_DISTION) {
            throw new GuiguException(ResultCodeEnum.DRIVER_END_LOCATION_DISTION_ERROR);
        }

        //2.计算订单实际里程
        BigDecimal realDistance = locationFeignClient.calculateOrderRealDistance(orderFeeForm.getOrderId()).getData();
        log.info("结束代驾，订单实际里程：{}", realDistance);

        //3.计算代驾实际费用
        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDistance(realDistance);
        feeRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
        //等候时间
        Integer waitMinute = Math.abs((int) ((orderInfo.getArriveTime().getTime() - orderInfo.getAcceptTime().getTime()) / (1000 * 60)));
        feeRuleRequestForm.setWaitMinute(waitMinute);
        log.info("结束代驾，费用参数：{}", JSONUtil.toJsonStr(feeRuleRequestForm));
        FeeRuleResponseVo feeRuleResponseVo = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm).getData();
        log.info("费用明细：{}", JSONUtil.toJsonStr(feeRuleResponseVo));
        //订单总金额 需加上 路桥费、停车费、其他费用、乘客好处费
        BigDecimal totalAmount = feeRuleResponseVo.getTotalAmount().add(orderFeeForm.getTollFee()).add(orderFeeForm.getParkingFee()).add(orderFeeForm.getOtherFee()).add(orderInfo.getFavourFee());
        feeRuleResponseVo.setTotalAmount(totalAmount);

        //4.计算系统奖励
        //4.1.获取订单数
        String startTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 00:00:00";
        String endTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 24:00:00";
        Long orderNum = orderInfoFeignClient.getOrderNumByTime(startTime, endTime, orderInfo.getDriverId()).getData();
        //4.2.封装参数
        RewardRuleRequestForm rewardRuleRequestForm = new RewardRuleRequestForm();
        rewardRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
        rewardRuleRequestForm.setOrderNum(orderNum);
        //4.3.执行
        RewardRuleResponseVo rewardRuleResponseVo = rewardRuleFeignClient.calculateOrderRewardFee(rewardRuleRequestForm).getData();
        log.info("结束代驾，系统奖励：{}", JSONUtil.toJsonStr(rewardRuleResponseVo));

        //5.计算分账信息
        ProfitsharingRuleRequestForm profitsharingRuleRequestForm = new ProfitsharingRuleRequestForm();
        profitsharingRuleRequestForm.setOrderAmount(feeRuleResponseVo.getTotalAmount());
        profitsharingRuleRequestForm.setOrderNum(orderNum);
        ProfitsharingRuleResponseVo profitsharingRuleResponseVo = profitsharingRuleFeignClient.calculateOrderProfitsharingFee(profitsharingRuleRequestForm).getData();
        log.info("结束代驾，分账信息：{}", JSONUtil.toJsonStr(profitsharingRuleResponseVo));

        //6.封装更新订单账单相关实体对象
        UpdateOrderBillForm updateOrderBillForm = new UpdateOrderBillForm();
        updateOrderBillForm.setOrderId(orderFeeForm.getOrderId());
        updateOrderBillForm.setDriverId(orderFeeForm.getDriverId());
        //路桥费、停车费、其他费用
        updateOrderBillForm.setTollFee(orderFeeForm.getTollFee());
        updateOrderBillForm.setParkingFee(orderFeeForm.getParkingFee());
        updateOrderBillForm.setOtherFee(orderFeeForm.getOtherFee());
        //乘客好处费
        updateOrderBillForm.setFavourFee(orderInfo.getFavourFee());

        //实际里程
        updateOrderBillForm.setRealDistance(realDistance);
        //订单奖励信息
        BeanUtil.copyProperties(rewardRuleResponseVo, updateOrderBillForm);

        //代驾费用信息
        BeanUtil.copyProperties(feeRuleResponseVo, updateOrderBillForm);

        //分账相关信息
        BeanUtil.copyProperties(profitsharingRuleResponseVo, updateOrderBillForm);
        updateOrderBillForm.setProfitsharingRuleId(profitsharingRuleResponseVo.getProfitsharingRuleId());
        log.info("结束代驾，更新账单信息：{}", JSONUtil.toJsonStr(updateOrderBillForm));

        //7.结束代驾更新账单
        orderInfoFeignClient.endDrive(updateOrderBillForm);
        return true;
    }
}
