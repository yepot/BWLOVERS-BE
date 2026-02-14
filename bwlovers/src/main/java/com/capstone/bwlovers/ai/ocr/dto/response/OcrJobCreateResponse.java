package com.capstone.bwlovers.ai.ocr.dto.response;

import com.capstone.bwlovers.ai.ocr.domain.OcrJobStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrJobCreateResponse {
    private String jobId;
    private OcrJobStatus status;
}