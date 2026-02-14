package com.capstone.bwlovers.ai.ocr.controller;

import com.capstone.bwlovers.ai.ocr.dto.response.OcrJobCreateResponse;
import com.capstone.bwlovers.ai.ocr.dto.response.OcrJobStatusResponse;
import com.capstone.bwlovers.ai.ocr.service.OcrJobService;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ocr")
public class OcrController {

    private final OcrJobService ocrJobService;

    @PostMapping(value = "/jobs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OcrJobCreateResponse createJob(
            @RequestPart("images") @NotEmpty List<MultipartFile> images
    ) {
        return ocrJobService.createJob(images);
    }

    @GetMapping("/jobs/{jobId}")
    public OcrJobStatusResponse getJob(@PathVariable String jobId) {
        return ocrJobService.getJob(jobId);
    }
}
