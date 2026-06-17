package com.me.util;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class OssUtil {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.domain}")
    private String domain;

    @Value("${aliyun.oss.max-file-size}")
    private Long maxFileSize;

    private OSS ossClient;

    @PostConstruct
    public void init() {
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        log.info("OSS Client initialized, bucket: {}", bucketName);
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS Client shutdown");
        }
    }

    public String uploadFile(byte[] fileContent, String originalFilename, String identityType) {
        validateFile(originalFilename, fileContent.length);

        String extension = getFileExtension(originalFilename);
        String objectKey = generateObjectKey(identityType, extension);

        try {
            ossClient.putObject(bucketName, objectKey, new ByteArrayInputStream(fileContent));
            String url = domain + "/" + objectKey;
            log.info("File uploaded successfully: {}", url);
            return url;
        } catch (OSSException | ClientException e) {
            log.error("Failed to upload file to OSS", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            String objectKey = extractObjectKey(fileUrl);
            if (objectKey != null) {
                ossClient.deleteObject(bucketName, objectKey);
                log.info("File deleted from OSS: {}", objectKey);
            }
        } catch (Exception e) {
            log.warn("Failed to delete file from OSS: {}, error: {}", fileUrl, e.getMessage());
        }
    }

    private void validateFile(String filename, long fileSize) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件格式: " + extension);
        }

        if (fileSize > maxFileSize) {
            throw new IllegalArgumentException("文件大小超过限制: " + (maxFileSize / 1024 / 1024) + "MB");
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new IllegalArgumentException("文件名缺少扩展名");
        }
        return filename.substring(lastDotIndex + 1);
    }

    private String generateObjectKey(String identityType, String extension) {
        String datePath = LocalDate.now().format(DATE_FORMATTER);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return datePath + "/" + identityType + "/" + uuid + "." + extension;
    }

    private String extractObjectKey(String fileUrl) {
        if (fileUrl.startsWith(domain)) {
            return fileUrl.substring(domain.length() + 1);
        }
        return null;
    }
}
