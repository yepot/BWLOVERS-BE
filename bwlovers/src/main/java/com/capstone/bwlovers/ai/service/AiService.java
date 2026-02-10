package com.capstone.bwlovers.ai.service;

import com.capstone.bwlovers.ai.dto.request.FastApiRequest;
import com.capstone.bwlovers.ai.dto.response.AiRecommendationListResponse;
import com.capstone.bwlovers.ai.dto.response.FastApiResponse;
import com.capstone.bwlovers.ai.dto.response.AiRecommendationResponse;
import com.capstone.bwlovers.ai.dto.request.AiCallbackRequest;
import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.auth.repository.UserRepository;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import com.capstone.bwlovers.health.domain.HealthStatus;
import com.capstone.bwlovers.health.dto.request.HealthStatusRequest;
import com.capstone.bwlovers.health.repository.HealthStatusRepository;
import com.capstone.bwlovers.insurance.repository.InsuranceProductRepository;
import com.capstone.bwlovers.pregnancy.domain.PregnancyInfo;
import com.capstone.bwlovers.pregnancy.dto.request.PregnancyInfoRequest;
import com.capstone.bwlovers.pregnancy.repository.PregnancyInfoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final UserRepository userRepository;
    private final PregnancyInfoRepository pregnancyInfoRepository;
    private final HealthStatusRepository healthStatusRepository;
    private final WebClient aiWebClient;
    private final AiResultCacheService aiResultCacheService;

    private final InsuranceProductRepository insuranceProductRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * (기존 A안) 결과 전체를 바로 받는 방식 - 유지함(선택)
     */
    public FastApiResponse requestAiRecommendation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        PregnancyInfo pregnancyInfo = pregnancyInfoRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.PREGNANCY_INFO_NOT_FOUND));
        HealthStatus healthStatus = healthStatusRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.HEALTH_STATUS_NOT_FOUND));

        FastApiRequest dto = toFastApiRequest(pregnancyInfo, healthStatus);

        return aiWebClient.post()
                .uri("/ai/recommend")
                .bodyValue(dto)
                .retrieve()
                .onStatus(s -> s.value() == 400 || s.value() == 422,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_INVALID_REQUEST)))
                .onStatus(s -> s.value() == 409,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_PROCESSING_FAILED)))
                .onStatus(s -> s.is5xxServerError(),
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_SERVER_5XX)))
                .bodyToMono(FastApiResponse.class)
                .timeout(Duration.ofSeconds(25))
                .block();
    }

    // =========================================================
    // 리스트 → 상세 보기 → 선택 저장
    // =========================================================

    /**
     * 추천 리스트 조회
     */
    public AiRecommendationListResponse requestAiRecommendationList(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        PregnancyInfo pregnancyInfo = pregnancyInfoRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.PREGNANCY_INFO_NOT_FOUND));
        HealthStatus healthStatus = healthStatusRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ExceptionCode.HEALTH_STATUS_NOT_FOUND));

        FastApiRequest dto = toFastApiRequest(pregnancyInfo, healthStatus);

        String raw = aiWebClient.post()
                .uri("/ai/recommend") // FastAPI: 리스트 + resultId 반환해야 함
                .bodyValue(dto)
                .retrieve()
                .onStatus(s -> s.value() == 400 || s.value() == 422,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_INVALID_REQUEST)))
                .onStatus(s -> s.value() == 409,
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_PROCESSING_FAILED)))
                .onStatus(s -> s.is5xxServerError(),
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_SERVER_5XX)))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(25))
                .block();

        log.info("[AI /ai/recommend RAW RESPONSE] {}", raw);

        AiRecommendationListResponse list;
        try {
            list = objectMapper.readValue(raw, AiRecommendationListResponse.class);
        } catch (Exception e) {
            log.error("[AI /ai/recommend PARSE ERROR] raw={}", raw, e);
            throw new CustomException(ExceptionCode.AI_SERVER_5XX);
        }
        return list;
    }

    /**
     * 결과 상세 보기
     * GET /ai/results/{resultId}/items/{itemId}
     */
    public AiRecommendationResponse fetchAiResultDetail(Long userId, String resultId, String itemId) {

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));

        if (isBlank(resultId) || isBlank(itemId)) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        // Redis에서 상세 조회
        AiRecommendationResponse cached = aiResultCacheService.getDetail(resultId, itemId);
        if (cached == null) {
            throw new CustomException(ExceptionCode.AI_RESULT_NOT_FOUND);
        }

        return cached;
    }

    /**
     * AI callback 결과를 Redis에 저장함
     * - listKey(resultId) : 리스트 요약
     * - detailKey(resultId, itemId) : itemId별 상세
     */
    public void cacheCallbackResult(AiCallbackRequest callback) {

        if (callback == null || isBlank(callback.getResultId())) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        long ttlSec = (callback.getExpiresInSec() == null ? 600L : callback.getExpiresInSec());

        // 1) 리스트 요약 생성 후 저장
        AiRecommendationListResponse list = AiRecommendationListResponse.fromCallback(callback);
        aiResultCacheService.saveList(callback.getResultId(), list, ttlSec);

        // 2) 상세(itemId별) 저장
        if (callback.getItems() != null) {
            for (var item : callback.getItems()) {
                if (item == null || isBlank(item.getItemId())) continue;

                AiRecommendationResponse detail = AiRecommendationResponse.fromCallbackItem(item);
                aiResultCacheService.saveDetail(callback.getResultId(), item.getItemId(), detail, ttlSec);
            }
        }
    }



    // =========================================================
    // private
    // =========================================================

    private FastApiRequest toFastApiRequest(PregnancyInfo pregnancyInfo, HealthStatus healthStatus) {
        PregnancyInfoRequest pregnancyInfoRequest = PregnancyInfoRequest.from(pregnancyInfo);
        HealthStatusRequest healthStatusRequest = HealthStatusRequest.from(healthStatus);
        return new FastApiRequest(pregnancyInfoRequest, healthStatusRequest);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new CustomException(ExceptionCode.JSON_SERIALIZATION_FAILED);
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
