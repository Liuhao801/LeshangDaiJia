package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import org.springframework.web.multipart.MultipartFile;

public interface CosService {

    //上传文件到腾讯cos
    CosUploadVo upload(MultipartFile file, String path);

    //获取临时回显url
    String getImageUrl(String path);
}
