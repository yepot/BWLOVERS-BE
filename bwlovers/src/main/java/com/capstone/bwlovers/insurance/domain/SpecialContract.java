package com.capstone.bwlovers.insurance.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "special_contracts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SpecialContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long contractId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_id", nullable = false)
    private InsuranceProduct insuranceProduct;

    @Column(name = "contract_name", nullable = false)
    private String contractName;

    @Column(name = "contract_description", columnDefinition = "text")
    private String contractDescription;

    @Column(name = "contract_recommendation_reason", columnDefinition = "text")
    private String contractRecommendationReason;

    @Column(name = "key_features", columnDefinition = "text")
    private String keyFeatures;

    @Column(name = "page_number")
    private Long pageNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void setInsuranceProduct(InsuranceProduct insuranceProduct) {
        this.insuranceProduct = insuranceProduct;
    }
}