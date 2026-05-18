package com.me.controller.user;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@RestController
@RequestMapping("/api/elder/ai")
public class UserAIController {

    // 阿里云通义千问API-KEY（从配置文件读取，不要硬编码）
    @Value("${aliyun.dashscope.api-key}")
    private String ALIYUN_API_KEY;

    @Value("${aliyun.dashscope.api-url}")
    private String ALIYUN_API_URL;
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> requestBody) {
        RestTemplate restTemplate = new RestTemplate();

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + ALIYUN_API_KEY);

        // 转发请求到阿里云
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                ALIYUN_API_URL,
                HttpMethod.POST,
                entity,
                String.class
        );

        return ResponseEntity.ok(response.getBody());
    }
}