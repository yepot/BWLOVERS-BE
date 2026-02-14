package com.capstone.bwlovers.ai.ocr.domain;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrResult {

    private String overview;
    private List<String> keyPoints;
    private List<String> exclusions;
    private List<String> claimSteps;
    private List<Citation> citations;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Citation {
        private int page;
        private String excerpt;
    }
}
