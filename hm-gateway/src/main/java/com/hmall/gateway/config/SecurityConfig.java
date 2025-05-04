package com.hmall.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;

import java.security.KeyPair;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /*
     *  作用：注册一个 PasswordEncoder 的 Bean，使用 BCrypt 加密算法（业界推荐的安全加密方式）。
     *  目的：用于用户密码的加密与验证，在登录验证时对比密码是否一致。
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public KeyPair keyPair(JwtProperties properties){
        // 获取秘钥工厂
        KeyStoreKeyFactory keyStoreKeyFactory =
                new KeyStoreKeyFactory(
                        properties.getLocation(), // 密钥库文件路径，存放了一对公密钥
                        properties.getPassword().toCharArray());  // 密码（保护整个 keystore 文件）
        //读取钥匙对
        return keyStoreKeyFactory.getKeyPair(
                properties.getAlias(), // 证书别名
                properties.getPassword().toCharArray()); // 密钥条目的访问密码（通常与 keystore 密码相同）
    }
}