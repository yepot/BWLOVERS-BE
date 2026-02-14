package com.capstone.bwlovers.ai.recommendation.service;

import com.capstone.bwlovers.ai.recommendation.dto.request.AiCallbackRequest;
import com.capstone.bwlovers.ai.recommendation.dto.request.FastApiRequest;
import com.capstone.bwlovers.ai.recommendation.dto.response.AiRecommendationListResponse;
import com.capstone.bwlovers.ai.recommendation.dto.response.AiRecommendationResponse;
import com.capstone.bwlovers.ai.recommendation.dto.response.FastApiResponse;
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

    private static final long DEFAULT_TTL_SEC = 600L; // 10분

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
    // 리스트 → 상세 보기
    // =========================================================

    /**
     * 추천 리스트 조회
     * - FastAPI 응답(list)을 Redis에 저장
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

        if (list == null || isBlank(list.getResultId())) {
            log.warn("[AI /ai/recommend INVALID] resultId is blank. raw={}", raw);
            throw new CustomException(ExceptionCode.AI_SERVER_5XX);
        }

        list.normalizeAllCounts();

        long ttlSec = (list.getExpiresInSec() == null ? DEFAULT_TTL_SEC : list.getExpiresInSec());

        // 1) 리스트 저장
        aiResultCacheService.saveList(list.getResultId(), list, ttlSec);

        // 2) itemId별 상세 캐시 저장
        //    (현재는 list 응답에 상세 필드가 포함되어 있으므로 fromListItem이 제대로 채워줄 수 있음)
        if (list.getItems() != null) {
            for (var item : list.getItems()) {
                if (item == null || isBlank(item.getItemId())) continue;

                AiRecommendationResponse detail = AiRecommendationResponse.fromListItem(item);
                aiResultCacheService.saveDetail(list.getResultId(), item.getItemId(), detail, ttlSec);
            }
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

        log.info("[FETCH DETAIL] Request ResultId: {}, ItemId: {}", resultId, itemId);

        // 1) Redis 캐시 먼저 확인
        AiRecommendationResponse cached = aiResultCacheService.getDetail(resultId, itemId);
        if (cached != null) {
            log.info("[FETCH DETAIL] Redis cache HIT. itemId={}", itemId);
            return cached;
        }

        // 2) Redis에 없으면 FastAPI로 fallback
        log.info("[FETCH DETAIL] Redis cache MISS -> try FastAPI. resultId={}", resultId);

        AiRecommendationResponse fresh = aiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ai/results/{resultId}/items/{itemId}")
                        .build(resultId, itemId))
                .retrieve()
                .onStatus(s -> s.is4xxClientError(),
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_RESULT_NOT_FOUND)))
                .onStatus(s -> s.is5xxServerError(),
                        resp -> Mono.error(new CustomException(ExceptionCode.AI_SERVER_5XX)))
                .bodyToMono(AiRecommendationResponse.class)
                .timeout(Duration.ofSeconds(10))
                .block();

        // 3) FastAPI에서 가져온 데이터 캐시에 저장
        if (fresh != null) {
            aiResultCacheService.saveDetail(resultId, itemId, fresh, DEFAULT_TTL_SEC);
        }

        return fresh;
    }

    /**
     * AI callback 결과를 Redis에 저장함
     */
    public void cacheCallbackResult(AiCallbackRequest callback) {

        if (callback == null || isBlank(callback.getResultId())) {
            throw new CustomException(ExceptionCode.AI_INVALID_REQUEST);
        }

        long ttlSec = (callback.getExpiresInSec() == null ? DEFAULT_TTL_SEC : callback.getExpiresInSec());

        // 1) 리스트 생성 후 저장
        AiRecommendationListResponse list = AiRecommendationListResponse.fromCallback(callback);
        // fromCallback 안에서 이미 normalizeCounts 호출됨(안전)
        aiResultCacheService.saveList(callback.getResultId(), list, ttlSec);

        // 2) 상세(itemId별) 저장
        if (callback.getItems() != null) {
            for (var item : callback.getItems()) {
                if (item == null || isBlank(item.getItemId())) continue;

                AiRecommendationResponse detail = AiRecommendationResponse.fromCallbackItem(item);
                aiResultCacheService.saveDetail(callback.getResultId(), item.getItemId(), detail, ttlSec);
            }
        }

        log.info("[AI CALLBACK CACHED] resultId={}, ttlSec={}, items={}",
                callback.getResultId(),
                ttlSec,
                callback.getItems() == null ? 0 : callback.getItems().size());
    }

    // =========================================================
    // private
    // =========================================================

    private FastApiRequest toFastApiRequest(PregnancyInfo pregnancyInfo, HealthStatus healthStatus) {
        PregnancyInfoRequest pregnancyInfoRequest = PregnancyInfoRequest.from(pregnancyInfo);
        HealthStatusRequest healthStatusRequest = HealthStatusRequest.from(healthStatus);
        return new FastApiRequest(pregnancyInfoRequest, healthStatusRequest);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
