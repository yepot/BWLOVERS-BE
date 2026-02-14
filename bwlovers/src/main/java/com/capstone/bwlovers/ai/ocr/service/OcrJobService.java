package com.capstone.bwlovers.ai.ocr.service;

import com.capstone.bwlovers.ai.ocr.domain.*;
import com.capstone.bwlovers.ai.ocr.dto.response.OcrJobCreateResponse;
import com.capstone.bwlovers.ai.ocr.dto.response.OcrJobStatusResponse;
import com.capstone.bwlovers.ai.ocr.infra.cache.OcrJobCacheRepository;
import com.capstone.bwlovers.ai.ocr.infra.storage.FileStorage;
import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OcrJobService {

    private final OcrJobCacheRepository cacheRepository;
    private final FileStorage fileStorage;
    private final OcrWorkerService workerService;

    @Value("${analysis.ocr.max-pages:10}")
    private int maxPages;

    @Value("${analysis.ocr.max-file-size-bytes:10485760}")
    private long maxFileSizeBytes;

    public OcrJobCreateResponse createJob(List<MultipartFile> images) {
        validate(images);

        String jobId = UUID.randomUUID().toString();

        List<OcrPageFileRef> refs = fileStorage.saveJobFiles(jobId, images);

        OcrJobCache cache = OcrJobCache.builder()
                .jobId(jobId)
                .status(OcrJobStatus.PENDING)
                .totalPages(refs.size())
                .donePages(0)
                .files(refs)
                .result(null)
                .error(null)
                .createdAt(Instant.now())
                .build();

        // Redis 저장 실패도 CustomException으로 올리려면 Repository에서도 처리하도록(권장함)
        cacheRepository.save(cache);

        workerService.processAsync(jobId);

        return OcrJobCreateResponse.builder()
                .jobId(jobId)
                .status(cache.getStatus())
                .build();
    }

    public OcrJobStatusResponse getJob(String jobId) {
        OcrJobCache cache = cacheRepository.find(jobId)
                .orElseThrow(() -> new CustomException(ExceptionCode.OCR_JOB_NOT_FOUND));

        return OcrJobStatusResponse.builder()
                .jobId(cache.getJobId())
                .status(cache.getStatus())
                .progress(OcrJobStatusResponse.Progress.builder()
                        .donePages(cache.getDonePages())
                        .totalPages(cache.getTotalPages())
                        .build())
                .result(cache.getResult())
                .error(cache.getError())
                .build();
    }

    private void validate(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw new CustomException(ExceptionCode.OCR_INVALID_REQUEST);
        }

        if (images.size() > maxPages) {
            throw new CustomException(ExceptionCode.OCR_TOO_MANY_PAGES);
        }

        for (MultipartFile f : images) {
            if (f == null || f.isEmpty()) {
                throw new CustomException(ExceptionCode.OCR_EMPTY_FILE);
            }

            if (f.getSize() > maxFileSizeBytes) {
                throw new CustomException(ExceptionCode.OCR_FILE_TOO_LARGE);
            }

            String ct = f.getContentType();
            if (ct == null || !ct.startsWith("image/")) {
                throw new CustomException(ExceptionCode.OCR_UNSUPPORTED_FILE_TYPE);
            }
        }
    }
}
