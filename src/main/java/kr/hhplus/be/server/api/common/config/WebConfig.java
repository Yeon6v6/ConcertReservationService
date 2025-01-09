package kr.hhplus.be.server.api.common.config;

import kr.hhplus.be.server.api.token.infrastructure.TokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final TokenInterceptor tokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/concerts/**, /reservations/**") // 경로 지정
                .excludePathPatterns("/public/**") // 특정 경로 제외
                ;
    }
}
