package com.assignment.InshortAssignment.config;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class AppConfig {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Bean
    public OpenAiService openAiService() {
        return new OpenAiService(openaiApiKey, Duration.ofSeconds(30));
    }
}
