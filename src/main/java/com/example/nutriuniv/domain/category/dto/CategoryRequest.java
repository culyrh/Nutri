package com.example.nutriuniv.domain.category.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CategoryRequest {

    private String name;
    private Integer depth;
    private Long parentId;
    private int displayOrder = 0;
}