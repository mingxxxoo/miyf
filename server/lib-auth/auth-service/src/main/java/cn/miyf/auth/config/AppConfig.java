package cn.miyf.auth.config;

import cn.miyf.auth.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 基础模块通用配置：密码编码器、JWT 属性绑定。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class})
public class AppConfig {

    /**
     * BCrypt 密码编码器。
     *
     * @return PasswordEncoder
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
