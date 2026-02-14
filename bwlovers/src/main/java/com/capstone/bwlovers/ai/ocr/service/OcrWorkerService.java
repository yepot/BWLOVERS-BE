package com.capstone.bwlovers.ai.ocr.service;

import com.capstone.bwlovers.ai.ocr.domain.*;
import com.capstone.bwlovers.ai.ocr.infra.cache.OcrJobCacheRepository;
import com.capstone.bwlovers.ai.ocr.infra.ocrclient.OcrClient;
import com.capstone.bwlovers.ai.ocr.infra.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrWorkerService {

    private final OcrJobCacheRepository cacheRepository;
    private final FileStorage fileStorage;
    private final OcrClient ocrClient;

    private final OcrTextProcessor textProcessor;
    private final OcrSummarizer summarizer;

    @Async("ocrExecutor")
    public void processAsync(String jobId) {
        OcrJobCache cache = cacheRepository.find(jobId).orElse(null);
        if (cache == null) return;

        try {
            cache.setStatus(OcrJobStatus.OCR_RUNNING);
            cacheRepository.save(cache);

            List<String> pageTexts = new ArrayList<>();

            for (OcrPageFileRef ref : cache.getFiles()) {
                byte[] bytes = fileStorage.read(ref.getUri());
                String text = ocrClient.extractText(bytes);
                pageTexts.add(text);

                cache.setDonePages(cache.getDonePages() + 1);
                cacheRepository.save(cache);
            }

            cache.setStatus(OcrJobStatus.SUMMARIZING);
            cacheRepository.save(cache);

            String merged = textProcessor.normalizeAndMerge(pageTexts);
            OcrResult result = summarizer.summarize(merged, pageTexts);

            cache.setResult(result);
            cache.setStatus(OcrJobStatus.DONE);
            cacheRepository.save(cache);

        } catch (Exception e) {
            log.error("OCR job failed. jobId={}", jobId, e);
            cache.setStatus(OcrJobStatus.FAILED);
            cache.setError(e.getMessage());
            cacheRepository.save(cache);
        } finally {
            // 영구 저장 안 하므로 처리 후 파일 삭제함
            try {
                OcrJobCache latest = cacheRepository.find(jobId).orElse(null);
                if (latest != null) fileStorage.deleteJobFiles(jobId, latest.getFiles());
            } catch (Exception ignore) {}
        }
    }
}
