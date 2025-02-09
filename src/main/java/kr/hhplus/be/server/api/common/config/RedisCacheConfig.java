package kr.hhplus.be.server.api.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.time.LocalDate;

@Configuration
@EnableCaching // Spring Boot의 캐싱 설정을 활성화
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 날짜/시간(LocalDate, LocalDateTime) 직렬화를 위한 ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule()) // Java 8 날짜 지원
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-8601 형식 유지

        // ✅ GenericJackson2JsonRedisSerializer로 변경 (LocalDate 지원)
        RedisSerializer<Object> valueSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())) // Key 직렬화
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)) // Value 직렬화
                .entryTtl(Duration.ofMinutes(5)); // TTL 5분 설정

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration)
                .build();
    }
}

