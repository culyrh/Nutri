package com.example.nutriuniv.domain.review.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.util.List;

@Getter
public class ReviewUpdateRequest {

    @NotNull(message = "전체 평점은 필수입니다.")
    @Min(value = 1, message = "평점은 1 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5 이하이어야 합니다.")
    private Integer scoreOverall;

    @Min(value = 1, message = "맛 평점은 1 이상이어야 합니다.")
    @Max(value = 5, message = "맛 평점은 5 이하이어야 합니다.")
    private Integer scoreTaste;

    @Min(value = 1, message = "가격 평점은 1 이상이어야 합니다.")
    @Max(value = 5, message = "가격 평점은 5 이하이어야 합니다.")
    private Integer scoreValue;

    @Size(max = 500, message = "리뷰 내용은 500자 이하여야 합니다.")
    private String content;

    @Size(max = 3, message = "이미지는 최대 3장까지 첨부할 수 있습니다.")
    private List<String> images;
}