package com.example.nutriuniv.domain.brand.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.domain.brand.dto.BrandRequest;
import com.example.nutriuniv.domain.brand.dto.BrandResponse;
import com.example.nutriuniv.domain.brand.entity.Brand;
import com.example.nutriuniv.domain.brand.repository.BrandRepository;
import com.example.nutriuniv.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    // ── GET /brands ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BrandResponse> getBrands() {
        return productRepository.findBrandCountAll().stream()
                .map(row -> BrandResponse.builder()
                        .id(((Brand) row[0]).getId())
                        .name(((Brand) row[0]).getName())
                        .productCount((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    // ── GET /admin/brands ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BrandResponse.BrandPageResponse getAdminBrands(String keyword, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new CustomException(ErrorCode.INVALID_QUERY_PARAM);
        }

        Map<Long, Long> productCountMap = productRepository.findBrandCountAll().stream()
                .collect(Collectors.toMap(
                        row -> ((Brand) row[0]).getId(),
                        row -> (Long) row[1]
                ));

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Brand> brandPage = (keyword == null || keyword.isBlank())
                ? brandRepository.findAll(pageable)
                : brandRepository.findByNameContaining(keyword, pageable);

        List<BrandResponse> items = brandPage.getContent().stream()
                .map(b -> BrandResponse.builder()
                        .id(b.getId())
                        .name(b.getName())
                        .productCount(productCountMap.getOrDefault(b.getId(), 0L))
                        .build())
                .collect(Collectors.toList());

        return BrandResponse.BrandPageResponse.builder()
                .total(brandPage.getTotalElements())
                .page(page)
                .size(size)
                .items(items)
                .build();
    }

    // ── POST /admin/brands ────────────────────────────────────────────────────────

    @Transactional
    public Long createBrand(BrandRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "브랜드명은 필수입니다.");
        }
        if (brandRepository.findByName(request.getName().trim()).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE, "이미 존재하는 브랜드명입니다.");
        }

        return brandRepository.save(Brand.create(request.getName().trim())).getId();
    }

    // ── PATCH /admin/brands/{brandId} ─────────────────────────────────────────────

    @Transactional
    public void updateBrand(Long brandId, BrandRequest request) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 브랜드입니다."));

        if (request.getName() == null || request.getName().isBlank()) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "브랜드명은 필수입니다.");
        }

        String newName = request.getName().trim();
        if (!brand.getName().equals(newName) && brandRepository.findByName(newName).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE, "이미 존재하는 브랜드명입니다.");
        }

        brand.updateName(newName);
    }

    // ── DELETE /admin/brands/{brandId} ────────────────────────────────────────────

    @Transactional
    public void deleteBrand(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 브랜드입니다."));

        if (productRepository.existsByBrandAndIsActiveTrue(brand)) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "해당 브랜드에 상품이 존재하여 삭제할 수 없습니다.");
        }

        brand.deactivate();
    }
}