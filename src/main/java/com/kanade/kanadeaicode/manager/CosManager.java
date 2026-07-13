package com.kanade.kanadeaicode.manager;

import com.kanade.kanadeaicode.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;

/**
 * COS对象存储管理器
 *
 * @author Kanade
 */
@Component
@Slf4j
public class CosManager {

        @Resource
        private CosClientConfig cosClientConfig;

        @Resource
        private COSClient cosClient;

        /**
         * 上传对象
         *
         * @param key  唯一键
         * @param file 文件
         * @return 上传结果
         */
        public PutObjectResult putObject(String key, File file) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
            return cosClient.putObject(putObjectRequest);
        }

        /**
         * 上传对象（通过 InputStream）
         *
         * @param key         唯一键
         * @param inputStream 输入流
         * @param metadata    对象元数据（需设置 contentLength 和 contentType）
         * @return 上传结果
         */
        public PutObjectResult putObject(String key, InputStream inputStream, ObjectMetadata metadata) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, inputStream, metadata);
            return cosClient.putObject(putObjectRequest);
        }

        /**
         * 上传文件到 COS 并返回访问 URL
         *
         * @param key  COS对象键（完整路径）
         * @param file 要上传的文件
         * @return 文件的访问URL，失败返回null
         */
        public String uploadFile(String key, File file) {
            PutObjectResult result = putObject(key, file);
            if (result != null) {
                String url = String.format("%s%s", cosClientConfig.getHost(), key);
                log.info("文件上传COS成功: {} -> {}", file.getName(), url);
                return url;
            } else {
                log.error("文件上传COS失败，返回结果为空");
                return null;
            }
        }

        /**
         * 上传文件到 COS 并返回访问 URL（通过 InputStream）
         *
         * @param key          COS对象键（完整路径）
         * @param inputStream  输入流
         * @param metadata     对象元数据
         * @return 文件的访问URL，失败返回null
         */
        public String uploadFile(String key, InputStream inputStream, ObjectMetadata metadata) {
            PutObjectResult result = putObject(key, inputStream, metadata);
            if (result != null) {
                String url = String.format("%s%s", cosClientConfig.getHost(), key);
                log.info("文件上传COS成功: {} -> {}", key, url);
                return url;
            } else {
                log.error("文件上传COS失败，返回结果为空");
                return null;
            }
        }


}
