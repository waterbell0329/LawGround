package com.lawground.auth;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("local & !prod & !test")
@ConditionalOnProperty(name = "lawground.security.dev-auth-enabled", havingValue = "true")
public class LocalMemberConfiguration {
    @Bean
    ApplicationRunner initializeDevelopmentMember(JdbcTemplate jdbc) {
        return arguments ->
                jdbc.update(
                        "INSERT INTO members(id,display_name,status) VALUES (?, 'Local developer', 'ACTIVE') ON CONFLICT (id) DO NOTHING",
                        LocalDevelopmentAuth.MEMBER_ID);
    }
}
