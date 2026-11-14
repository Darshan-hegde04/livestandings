package com.phinixlabs.livestandings.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final OrganiserAuthInterceptor organiserAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(organiserAuthInterceptor)
                .addPathPatterns(
                        "/api/leagues/*/results",
                        "/api/leagues/*/results/**"
                );
    }
}
