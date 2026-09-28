package com.lawground.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
    @Bean
    public OpenAPI lawGroundOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("LawGround API")
                                .version("v1")
                                .description("공인중개사 기출문제 근거 역추적 플랫폼. 현재는 개발 기반만 준비되어 있습니다."));
    }
}
