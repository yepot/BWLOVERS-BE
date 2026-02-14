package com.capstone.bwlovers.ai.ocr.infra.ocrclient;

import org.springframework.stereotype.Component;

@Component
public class DummyOcrClient implements OcrClient {

    @Override
    public String extractText(byte[] imageBytes) {
        return "DUMMY_OCR_TEXT";
    }
}