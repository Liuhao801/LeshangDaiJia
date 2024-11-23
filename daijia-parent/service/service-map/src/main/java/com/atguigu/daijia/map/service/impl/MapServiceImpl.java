package com.atguigu.daijia.map.service.impl;

import cn.hutool.json.JSONObject;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.map.service.MapService;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapServiceImpl implements MapService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${tencent.map.key}")
    private String tencentMapKey;

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        //1、使用RestTemplate调用腾讯地图API计算驾驶线路
        String url = "https://apis.map.qq.com/ws/direction/v1/driving/?from={from}&to={to}&key={key}";
        //封装参数
        Map<String,String> map = new HashMap<>();
        map.put("key",tencentMapKey);
        map.put("from",calculateDrivingLineForm.getStartPointLatitude()+","+calculateDrivingLineForm.getStartPointLongitude());
        map.put("to",calculateDrivingLineForm.getEndPointLatitude()+","+calculateDrivingLineForm.getEndPointLongitude());
        //发送请求
        JSONObject result = restTemplate.getForObject(url, JSONObject.class, map);

        //2、解析腾讯地图API返回的数据
        if(result == null || result.getInt("status")!=0){
            throw new GuiguException(ResultCodeEnum.MAP_FAIL);
        }
        //获取第一条路线信息
        JSONObject route = result.getJSONObject("result").getJSONArray("routes").getJSONObject(0);

        //3、封装数据返回
        DrivingLineVo drivingLineVo = new DrivingLineVo();
        //单位：千米
        drivingLineVo.setDistance(route.getBigDecimal("distance").divide(new BigDecimal(1000)).setScale(2, RoundingMode.HALF_UP));
        drivingLineVo.setDuration(route.getBigDecimal("duration"));
        drivingLineVo.setPolyline(route.getJSONArray("polyline"));
        return drivingLineVo;
    }
}
