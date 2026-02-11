package com.capstone.bwlovers.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        // Key는 일반 문자열로 저장
        template.setKeySerializer(new StringRedisSerializer());
        // Value는 JSON 구조로 저장 (객체 직렬화)
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
}