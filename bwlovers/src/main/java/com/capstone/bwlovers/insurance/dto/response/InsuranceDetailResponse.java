package com.capstone.bwlovers.insurance.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class InsuranceDetailResponse {
    private Long insuranceId;
    private String resultId;
    private String itemId;
    private String insuranceCompany;
    private String productName;
    private boolean isLongTerm;
    private Long monthlyCost;
    private String insuranceRecommendationReason;
    private String memo;
    private List<SpecialContractDetailDto> specialContracts;

    @Getter
    @Builder
    public static class SpecialContractDetailDto {
        private Long contractId;
        private String contractName;
        private String contractDescription;
        private String contractRecommendationReason;
        private List<String> keyFeatures;
        private Long pageNumber;
    }
}