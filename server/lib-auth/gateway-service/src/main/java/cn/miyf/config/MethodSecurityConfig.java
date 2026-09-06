package cn.miyf.config;

import cn.miyf.security.MiyfPermission;
import cn.miyf.security.MiyfPermissionAuthorizationManager;
import cn.miyf.security.RequirePermission;
import cn.miyf.security.RequirePermissionAuthorizationManager;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Spring Security 方法级权限：将 {@link RequirePermission}/{@link MiyfPermission} 接入 AuthorizationManager。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

    /**
     * {@link RequirePermission} 前置鉴权拦截器。
     *
     * @param manager 鉴权管理器
     * @return Advisor
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    Advisor requirePermissionAdvisor(RequirePermissionAuthorizationManager manager) {
        AnnotationMatchingPointcut pointcut = new AnnotationMatchingPointcut(null, RequirePermission.class, true);
        return new AuthorizationManagerBeforeMethodInterceptor(pointcut, manager);
    }

    /**
     * {@link MiyfPermission} 前置鉴权拦截器（无 RequirePermission 时生效）。
     *
     * @param manager 鉴权管理器
     * @return Advisor
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    Advisor miyfPermissionAdvisor(MiyfPermissionAuthorizationManager manager) {
        AnnotationMatchingPointcut pointcut = new AnnotationMatchingPointcut(null, MiyfPermission.class, true);
        return new AuthorizationManagerBeforeMethodInterceptor(pointcut, manager);
    }
}
