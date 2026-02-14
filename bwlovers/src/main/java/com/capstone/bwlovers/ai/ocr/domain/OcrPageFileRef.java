package com.capstone.bwlovers.ai.ocr.domain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrPageFileRef {
    private int pageIndex;
    private String uri; // local file path, or s3 uri
}
