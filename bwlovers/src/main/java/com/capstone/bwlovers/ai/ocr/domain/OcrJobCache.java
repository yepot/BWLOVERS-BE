package com.capstone.bwlovers.ai.ocr.domain;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrJobCache {

    private String jobId;
    private OcrJobStatus status;

    private int totalPages;
    private int donePages;

    private List<OcrPageFileRef> files;

    private OcrResult result; // DONE 시
    private String error; // FAILED 시

    private Instant createdAt;
}
