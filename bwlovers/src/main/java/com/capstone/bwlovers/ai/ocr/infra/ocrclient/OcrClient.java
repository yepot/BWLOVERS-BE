package com.capstone.bwlovers.ai.ocr.infra.ocrclient;

public interface OcrClient {
    String extractText(byte[] imageBytes);
}
