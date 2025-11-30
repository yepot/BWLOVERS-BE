package com.capstone.bwlovers.ai.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FastApiResponse {
    private boolean success;
    private Object received_profile; // FastAPI에서 돌려준 profile 그대로 받기
}
