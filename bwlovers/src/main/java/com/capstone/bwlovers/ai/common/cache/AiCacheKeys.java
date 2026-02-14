package com.capstone.bwlovers.ai.common.cache;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AiCacheKeys {

    private static final String PREFIX = "ai:v2:recommend:";

    /**
     * 추천 리스트 키 생성
     * 결과: ai:v2:recommend:list:{resultId}
     */
    public static String listKey(String resultId) {
        return PREFIX + "list:" + resultId;
    }

    /**
     * 추천 아이템 상세 키 생성
     * 결과: ai:v2:recommend:detail:{resultId}:{itemId}
     */
    public static String detailKey(String resultId, String itemId) {
        return PREFIX + "detail:" + resultId + ":" + itemId;
    }

    // OCR Job
    public static final String OCR_JOB_PREFIX = "analysis:ocr:job:";
}