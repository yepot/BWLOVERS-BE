package com.capstone.bwlovers.ai.ocr.dto.response;

import com.capstone.bwlovers.ai.ocr.domain.OcrJobStatus;
import com.capstone.bwlovers.ai.ocr.domain.OcrResult;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrJobStatusResponse {

    private String jobId;
    private OcrJobStatus status;
    private Progress progress;

    private OcrResult result; // DONE이면 포함
    private String error; // FAILED이면 포함

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Progress {
        private int donePages;
        private int totalPages;
    }
}
