package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import org.springframework.web.multipart.MultipartFile;

public interface CosService {


    //上传文件到腾讯cos
    CosUploadVo uploadFile(MultipartFile file, String path);
}
