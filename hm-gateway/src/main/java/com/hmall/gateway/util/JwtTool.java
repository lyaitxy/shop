package com.hmall.gateway.util;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.hmall.common.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtTool {
    // 定义一个JWT签名器，用于对JWT进行签名和验证签名
    private final JWTSigner jwtSigner;

    // 构造方法，接收一个RSA密钥对，使用Hutool工具类创建RS256算法的签名器
    public JwtTool(KeyPair keyPair) {
        this.jwtSigner = JWTSignerUtil.createSigner("rs256", keyPair);
    }

    /**
     * 创建 access-token
     *
     * @param userId 用户信息（用户ID）
     * @param ttl 有效时间（存活时间）
     * @return access-token 字符串
     */
    public String createToken(Long userId, Duration ttl) {
        // 创建一个JWT对象，设置载荷（用户ID）、过期时间，并用签名器签名生成Token字符串
        return JWT.create()
                .setPayload("user", userId)  // 设置用户ID作为Payload的一部分
                .setExpiresAt(new Date(System.currentTimeMillis() + ttl.toMillis())) // 设置过期时间
                .setSigner(jwtSigner)        // 设置签名器
                .sign();                     // 生成签名后的Token字符串
    }

    /**
     * 解析token
     *
     * @param token token字符串
     * @return 用户ID
     */
    public Long parseToken(String token) {
        // 校验token是否为空
        if (token == null) {
            throw new UnauthorizedException("未登录");
        }

        // 定义jwt变量
        JWT jwt;
        try {
            // 尝试将传入的token转换为JWT对象，并设置签名器用于验证
            jwt = JWT.of(token).setSigner(jwtSigner);
        } catch (Exception e) {
            // 若解析失败，说明token格式不合法
            throw new UnauthorizedException("无效的token", e);
        }

        // 校验token的签名是否合法
        if (!jwt.verify()) {
            // 如果验证失败，则抛出无效token异常
            throw new UnauthorizedException("无效的token");
        }

        try {
            // 验证token是否在有效期内（过期检查）
            JWTValidator.of(jwt).validateDate();
        } catch (ValidateException e) {
            // token已过期
            throw new UnauthorizedException("token已经过期");
        }

        // 从payload中获取用户ID字段
        Object userPayload = jwt.getPayload("user");
        if (userPayload == null) {
            // 如果没有用户信息，说明token不合法
            throw new UnauthorizedException("无效的token");
        }

        try {
            // 尝试将用户信息转换为Long类型（用户ID）
            return Long.valueOf(userPayload.toString());
        } catch (RuntimeException e) {
            // 转换失败说明格式不正确
            throw new UnauthorizedException("无效的token");
        }
    }
}
