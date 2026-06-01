package com.me.utils;

import com.me.vo.LoginVO;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKeyStr;

    @Value("${jwt.expiration}")
    private long ttlMillis;

    // 构建安全的 SecretKey
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretKeyStr.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT
     *
     * @param claims 自定义载荷（userId、role 等）
     * @return token
     */
    public String createJWT(Map<String, Object> claims) {
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(exp)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 JWT（统一异常）
     *
     * @param token JWT 字符串
     * @return Claims
     */
    public Claims parseJWT(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("token已过期");
        } catch (SignatureException e) {
            throw new RuntimeException("token签名错误");
        } catch (MalformedJwtException e) {
            throw new RuntimeException("token格式错误");
        } catch (Exception e) {
            throw new RuntimeException("token解析失败");
        }
    }

    // ---------- 常用便捷方法 ----------

    public Long getUserId(String token) {
        Claims claims = parseJWT(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 获取用户角色类型（数字版）
     * 0=管理员 1=志愿者 2=普通用户
     */
    public Integer getRole(String token) {
        Claims claims = parseJWT(token);
        return claims.get("role", Integer.class);
    }

    public boolean isExpired(String token) {
        try {
            Claims claims = parseJWT(token);
            return claims.getExpiration().before(new Date());
        } catch (RuntimeException e) {
            return true;
        }
    }

    // ---------- 新增：通用登录结果封装方法 ----------

    /**
     * 构建登录成功返回的LoginVO
     * 统一生成token和封装返回结果，消除三个Controller的重复代码
     */
    public LoginVO buildLoginVO(Long userId, Integer userType, String realName, String avatar) {
        // 生成JWT令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", userType);
        String token = createJWT(claims);

        // 封装返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setLoginId(userId);
        loginVO.setUserType(userType);
        loginVO.setRealName(realName);
        loginVO.setAvatar(avatar);

        return loginVO;
    }

    /**
     * 从 Authorization 头中提取用户 ID（自动处理 Bearer 前缀）
     */
    public Long getUserIdFromAuthHeader(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            throw new RuntimeException("Authorization 头为空");
        }
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        return getUserId(token);
    }

    /**
     * 从 Authorization 头中提取角色（自动处理 Bearer 前缀）
     */
    public Integer getRoleFromAuthHeader(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            throw new RuntimeException("Authorization 头为空");
        }
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        return getRole(token);
    }
}