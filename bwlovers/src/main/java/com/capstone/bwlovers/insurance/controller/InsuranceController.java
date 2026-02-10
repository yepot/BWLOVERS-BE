package com.capstone.bwlovers.insurance.controller;

import com.capstone.bwlovers.auth.domain.User;
import com.capstone.bwlovers.insurance.dto.request.InsuranceSelectionSaveRequest;
import com.capstone.bwlovers.insurance.service.InsuranceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/insurances")
public class InsuranceController {

    private final InsuranceService insuranceService;

    /**
     * 보험-특약 저장 POST /insurances/selected
     */
    @PostMapping("/selected")
    public ResponseEntity<Long> saveSelected(@AuthenticationPrincipal User user,
                                             @Valid @RequestBody InsuranceSelectionSaveRequest request) {
        return ResponseEntity.ok(insuranceService.saveSelected(user.getUserId(), request));
    }
}
