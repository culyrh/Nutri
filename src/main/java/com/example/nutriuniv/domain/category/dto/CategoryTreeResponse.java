package com.example.nutriuniv.domain.category.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CategoryTreeResponse {

    private Long id;
    private String name;
    private int depth;
    private int displayOrder;
    private List<CategoryTreeResponse> children;
}