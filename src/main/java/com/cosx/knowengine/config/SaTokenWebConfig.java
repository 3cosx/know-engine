package com.cosx.knowengine.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.cosx.knowengine.user.security.CurrentUserInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class SaTokenWebConfig implements WebMvcConfigurer {

    private final CurrentUserInterceptor currentUserInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> SaRouter.match("/**")
                        .notMatch("/v1/auth/login", "/v1/auth/bootstrap", "/error")
                        .check(route -> StpUtil.checkLogin())))
                .addPathPatterns("/**");
        registry.addInterceptor(currentUserInterceptor)
                .addPathPatterns("/**")
                .order(1);
    }
}
