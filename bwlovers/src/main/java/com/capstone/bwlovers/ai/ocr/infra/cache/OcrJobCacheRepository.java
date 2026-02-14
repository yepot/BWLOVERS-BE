package com.capstone.bwlovers.ai.ocr.infra.cache;

import com.capstone.bwlovers.ai.common.cache.AiCacheKeys;
import com.capstone.bwlovers.ai.ocr.domain.OcrJobCache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OcrJobCacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${analysis.ocr.job-ttl-seconds:1800}")
    private long ttlSeconds;

    private String key(String jobId) {
        return AiCacheKeys.OCR_JOB_PREFIX + jobId;
    }

    public void save(OcrJobCache cache) {
        redisTemplate.opsForValue().set(key(cache.getJobId()), cache, Duration.ofSeconds(ttlSeconds));
    }

    public Optional<OcrJobCache> find(String jobId) {
        Object v = redisTemplate.opsForValue().get(key(jobId));
        if (v == null) return Optional.empty();
        return Optional.of((OcrJobCache) v);
    }

    public void delete(String jobId) {
        redisTemplate.delete(key(jobId));
    }
}
