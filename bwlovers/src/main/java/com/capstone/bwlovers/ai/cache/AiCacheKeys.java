package com.capstone.bwlovers.ai.cache;

public final class AiCacheKeys {
    private AiCacheKeys() {}

    private static final String PREFIX = "ai:v2:recommend:";

    public static String listKey(String resultId) {
        return PREFIX + "list:" + resultId;
    }

    public static String detailKey(String resultId, String itemId) {
        return PREFIX + "detail:" + resultId + ":" + itemId;
    }
}

