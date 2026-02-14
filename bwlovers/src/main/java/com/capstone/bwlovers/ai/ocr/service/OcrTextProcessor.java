package com.capstone.bwlovers.ai.ocr.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OcrTextProcessor {

    public String normalizeAndMerge(List<String> pageTexts) {
        String merged = String.join("\n\n---PAGE---\n\n", pageTexts);
        merged = merged.replaceAll("[ \\t]+", " ");
        merged = merged.replaceAll("\\n{3,}", "\n\n");
        return merged.trim();
    }
}
