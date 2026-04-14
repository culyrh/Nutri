package com.example.nutriuniv.domain.category.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "int default 1")
    private int depth = 1;

    @Column(name = "display_order", nullable = false, columnDefinition = "int default 0")
    private int displayOrder = 0;

    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    private boolean isActive = true;

    public static Category createDepth1(String name) {
        Category c = new Category();
        c.name = name;
        c.depth = 1;
        return c;
    }

    public static Category createDepth2(String name, Category parent) {
        Category c = new Category();
        c.name = name;
        c.depth = 2;
        c.parent = parent;
        return c;
    }

    // 관리자 등록용 (depth 1~3 공통)
    public static Category create(String name, int depth, Category parent, int displayOrder) {
        Category c = new Category();
        c.name = name;
        c.depth = depth;
        c.parent = parent;
        c.displayOrder = displayOrder;
        return c;
    }

    // 관리자 수정용
    public void update(String name, Integer displayOrder) {
        if (name != null)         this.name         = name;
        if (displayOrder != null) this.displayOrder = displayOrder;
    }

    public void deactivate() {
        this.isActive = false;
    }
}