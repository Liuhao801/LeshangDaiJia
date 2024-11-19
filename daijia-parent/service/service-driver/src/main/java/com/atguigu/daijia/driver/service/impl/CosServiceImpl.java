package com.atguigu.daijia.driver.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

    @Autowired
    private TencentCloudProperties tencentCloudProperties;
    @Autowired
    private COSClient cosClient;

    //上传文件到腾讯cos
    @Override
    public CosUploadVo upload(MultipartFile file, String path) {
        //文件上传
        //元数据信息
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(file.getSize());
        meta.setContentEncoding("UTF-8");
        meta.setContentType(file.getContentType());

        //向存储桶中保存文件
        String fileType = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")); //文件后缀名
        String uploadPath = "/driver/" + path + "/" + UUID.randomUUID().toString(true) + fileType;
        PutObjectRequest putObjectRequest = null;
        try {
            putObjectRequest = new PutObjectRequest(tencentCloudProperties.getBucketPrivate(),  //bucketName
                    uploadPath,  //上传路径
                    file.getInputStream(),
                    meta);
        } catch (IOException e) {
            throw new GuiguException(ResultCodeEnum.SERVICE_ERROR);
        }
        putObjectRequest.setStorageClass(StorageClass.Standard);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest); //上传文件

        //返回vo对象
        CosUploadVo cosUploadVo = new CosUploadVo();
        cosUploadVo.setUrl(uploadPath);
        //图片临时访问url，回显使用
        cosUploadVo.setShowUrl(this.getImageUrl(uploadPath));
        return cosUploadVo;
    }

    ////获取临时回显url
    @Override
    public String getImageUrl(String path) {
        if(StrUtil.isBlank(path)) return "";

        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(tencentCloudProperties.getBucketPrivate(), path, HttpMethodName.GET);
        //设置临时URL有效期为15分钟
        Date expiration = new DateTime().plusMinutes(15).toDate();
        request.setExpiration(expiration);
        URL url = cosClient.generatePresignedUrl(request);
        return url.toString();
    }
}
