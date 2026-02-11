package com.capstone.bwlovers.insurance.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InsuranceListResponse {
    private Long insuranceId;
    private String insuranceCompany;
    private String productName;
    private LocalDateTime createdAt;
}
