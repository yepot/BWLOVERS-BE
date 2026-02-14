package com.capstone.bwlovers.ai.ocr.service;

import com.capstone.bwlovers.ai.ocr.domain.OcrResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OcrSummarizer {

    public OcrResult summarize(String mergedText, List<String> pageTexts) {
        return OcrResult.builder()
                .overview("약관 OCR 결과 요약(더미)임")
                .keyPoints(List.of("핵심 1(더미)임", "핵심 2(더미)임"))
                .exclusions(List.of("면책/주의 1(더미)임"))
                .claimSteps(List.of("청구 단계 1(더미)임"))
                .citations(List.of(OcrResult.Citation.builder()
                        .page(1)
                        .excerpt("근거(더미)임")
                        .build()))
                .build();
    }
}
