package com.example.nutriuniv.domain.product.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.domain.brand.entity.Brand;
import com.example.nutriuniv.domain.brand.repository.BrandRepository;
import com.example.nutriuniv.domain.category.entity.Category;
import com.example.nutriuniv.domain.category.repository.CategoryRepository;
import com.example.nutriuniv.domain.coupang.client.CoupangApiClient;
import com.example.nutriuniv.domain.coupang.dto.CoupangProductData;
import com.example.nutriuniv.domain.coupang.dto.CoupangSearchResponse;
import com.example.nutriuniv.domain.coupang.entity.CoupangLink;
import com.example.nutriuniv.domain.coupang.repository.CoupangLinkRepository;
import com.example.nutriuniv.domain.product.dto.ProductUploadResponse;
import com.example.nutriuniv.domain.product.entity.Product;
import com.example.nutriuniv.domain.product.entity.ProductNutrient;
import com.example.nutriuniv.domain.product.repository.ProductNutrientRepository;
import com.example.nutriuniv.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductExcelService {

    private final ProductRepository productRepository;
    private final ProductNutrientRepository productNutrientRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final CoupangApiClient coupangApiClient;
    private final CoupangLinkRepository coupangLinkRepository;

    // 헤더명 -> 필드 키 매핑 테이블
    // normalize() 처리된 헤더명을 키로 사용 -> 컬럼 순서 무관, 단위 포함 표기 허용
    private static final Map<String, String> HEADER_MAP = Map.ofEntries(
            Map.entry("제품명",    "name"),
            Map.entry("브랜드",    "brand"),
            Map.entry("대분류",    "categoryDepth1"),
            Map.entry("중분류",    "categoryDepth2"),
            Map.entry("서빙사이즈", "servingSize"),
            Map.entry("열량_kcal", "calories"),
            Map.entry("열량kcal",  "calories"),
            // TODO: 열량_kJ는 HEADER_MAP에 없으므로 자동 무시됨
            Map.entry("탄수화물",  "carbohydrate"),
            Map.entry("설탕",      "sugar"),
            Map.entry("단백질",    "protein"),
            Map.entry("지방",      "fat"),
            Map.entry("포화지방",  "saturatedFat"),
            Map.entry("트랜스지방", "transFat"),
            Map.entry("콜레스테롤", "cholesterol"),
            Map.entry("나트륨",    "sodium"),
            Map.entry("식이섬유",  "fiber")
    );

    private static final Set<String> REQUIRED_KEYS = Set.of(
            "name", "brand", "categoryDepth1", "categoryDepth2"
    );

    // ── 엑셀 업로드 메인 ──────────────────────────────────────────────────────────

    @Transactional
    public ProductUploadResponse upload(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "파일이 없습니다.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new CustomException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "xlsx 파일만 업로드 가능합니다.");
        }

        Workbook workbook;
        try {
            workbook = new XSSFWorkbook(file.getInputStream());
        } catch (IOException e) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "파일을 읽을 수 없습니다.");
        }

        Sheet sheet = workbook.getSheetAt(0);
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new CustomException(ErrorCode.UNPROCESSABLE_ENTITY, "헤더 행이 없습니다.");
        }

        // 헤더 파싱: { 컬럼 인덱스 → 필드 키 }
        Map<Integer, String> colIndexToKey = parseHeader(headerRow);
        validateRequiredColumns(colIndexToKey);

        int totalCount = 0;
        int successCount = 0;
        List<ProductUploadResponse.FailItem> failItems = new ArrayList<>();

        // 브랜드/카테고리 캐시 (행마다 DB 조회 방지)
        Map<String, Brand> brandCache  = new HashMap<>();
        Map<String, Category> catCache = new HashMap<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowBlank(row)) continue;

            totalCount++;
            int rowNumber = i + 1; // 사람 기준 행 번호 (헤더=1, 데이터 시작=2)

            try {
                processRow(row, colIndexToKey, brandCache, catCache);
                successCount++;
            } catch (Exception e) {
                String productName = getCellString(row, getColIndex(colIndexToKey, "name"));
                log.warn("[ExcelUpload] row={} name={} error={}", rowNumber, productName, e.getMessage());
                failItems.add(ProductUploadResponse.FailItem.builder()
                        .rowNumber(rowNumber)
                        .productName(productName)
                        .reason(e.getMessage())
                        .build());
            }
        }

        return ProductUploadResponse.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failCount(failItems.size())
                .failItems(failItems)
                .build();
    }

    // ── 행 처리 ───────────────────────────────────────────────────────────────────

    private void processRow(Row row,
                            Map<Integer, String> colIndexToKey,
                            Map<String, Brand> brandCache,
                            Map<String, Category> catCache) {

        Map<String, String> values = extractValues(row, colIndexToKey);

        String productName = values.getOrDefault("name", "").trim();
        String brandName   = values.getOrDefault("brand", "").trim();
        String depth1Name  = values.getOrDefault("categoryDepth1", "").trim();
        String depth2Name  = values.getOrDefault("categoryDepth2", "").trim();

        // RuntimeException 대신 CustomException 사용 (5번 이슈 수정)
        if (productName.isEmpty()) throw new CustomException(ErrorCode.BAD_REQUEST, "제품명이 비어있습니다.");
        if (brandName.isEmpty())   throw new CustomException(ErrorCode.BAD_REQUEST, "브랜드가 비어있습니다.");
        if (depth1Name.isEmpty())  throw new CustomException(ErrorCode.BAD_REQUEST, "대분류가 비어있습니다.");
        if (depth2Name.isEmpty())  throw new CustomException(ErrorCode.BAD_REQUEST, "중분류가 비어있습니다.");

        Brand brand = brandCache.computeIfAbsent(brandName, n ->
                brandRepository.findByName(n).orElseGet(() -> brandRepository.save(Brand.create(n))));

        Category depth1 = catCache.computeIfAbsent(depth1Name, n ->
                categoryRepository.findByNameAndDepth(n, 1)
                        .orElseGet(() -> categoryRepository.save(Category.createDepth1(n))));

        String depth2Key = depth1Name + "::" + depth2Name;
        Category depth2 = catCache.computeIfAbsent(depth2Key, k ->
                categoryRepository.findByNameAndDepthAndParent(depth2Name, 2, depth1)
                        .orElseGet(() -> categoryRepository.save(Category.createDepth2(depth2Name, depth1))));

        // TODO: 중복 상품명: 덮어쓰기 정책
        Product product = productRepository.findByName(productName)
                .orElseGet(() -> productRepository.save(Product.create(productName, depth2, brand)));
        product.update(depth2, brand);

        // TODO: 영양소 파싱 (빈 값 -> 0)
        ProductNutrient nutrient = productNutrientRepository.findById(product.getId())
                .orElseGet(() -> ProductNutrient.create(product));

        nutrient.update(
                values.getOrDefault("servingSize", ""),
                parseNutrient(values.get("calories")),
                parseNutrient(values.get("carbohydrate")),
                parseNutrient(values.get("sugar")),
                parseNutrient(values.get("protein")),
                parseNutrient(values.get("fat")),
                parseNutrient(values.get("saturatedFat")),
                parseNutrient(values.get("transFat")),
                parseNutrient(values.get("cholesterol")),
                parseNutrient(values.get("sodium")),
                parseNutrient(values.get("fiber"))
        );
        productNutrientRepository.save(nutrient);

        mapCoupangLink(product, productName);
    }

    private void mapCoupangLink(Product product, String keyword) {
        CoupangLink link = coupangLinkRepository.findByProduct(product)
                .orElseGet(() -> coupangLinkRepository.save(CoupangLink.createDefault(product)));
        try {
            CoupangSearchResponse.SearchData searchData = coupangApiClient.searchProduct(keyword);
            if (searchData == null) {
                link.syncFailed();
                return;
            }
            CoupangProductData data = searchData.getProductData().stream()
                    .filter(p -> p.getProductName() != null && p.getProductName().contains(keyword))
                    .findFirst()
                    .orElse(null);

            if (data == null) {
                link.syncFailed();
            } else {
                link.syncSuccess(
                        String.valueOf(data.getProductId()),
                        data.getProductName(),
                        data.getProductUrl(),
                        searchData.getLandingUrl(),
                        data.getProductImage(),
                        data.getProductPrice(),
                        data.getIsRocket(),
                        data.getIsFreeShipping()
                );
            }
        } catch (Exception e) {
            log.warn("[ExcelUpload] 쿠팡 매핑 실패 - keyword: {}, error: {}", keyword, e.getMessage());
            link.syncFailed();
        }
    }

    // ── 헤더 파싱 ─────────────────────────────────────────────────────────────────

    private Map<Integer, String> parseHeader(Row headerRow) {
        Map<Integer, String> map = new HashMap<>();
        for (Cell cell : headerRow) {
            String normalized = normalize(getCellString(headerRow, cell.getColumnIndex()));
            for (Map.Entry<String, String> entry : HEADER_MAP.entrySet()) {
                if (normalize(entry.getKey()).equals(normalized)) {
                    map.put(cell.getColumnIndex(), entry.getValue());
                    break;
                }
            }
        }
        return map;
    }

    /**
     * TODO: 헤더명 정규화: 공백 제거 + 괄호 및 괄호 내용 제거 + 소문자화
     * ex) "열량 (kcal)" -> "열량kcal", "포화 지방" → "포화지방"
     */
    private String normalize(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\(.*?\\)", "")
                .replaceAll("\\s+", "")
                .toLowerCase();
    }

    private void validateRequiredColumns(Map<Integer, String> colIndexToKey) {
        Set<String> found = new HashSet<>(colIndexToKey.values());
        for (String required : REQUIRED_KEYS) {
            if (!found.contains(required)) {
                throw new CustomException(ErrorCode.UNPROCESSABLE_ENTITY,
                        "필수 컬럼이 누락되었습니다: " + required);
            }
        }
    }

    // ── 셀 값 추출 ────────────────────────────────────────────────────────────────

    private Map<String, String> extractValues(Row row, Map<Integer, String> colIndexToKey) {
        Map<String, String> values = new HashMap<>();
        for (Map.Entry<Integer, String> entry : colIndexToKey.entrySet()) {
            values.put(entry.getValue(), getCellString(row, entry.getKey()));
        }
        return values;
    }

    private String getCellString(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                yield val == Math.floor(val) ? String.valueOf((long) val) : String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.NUMERIC
                    ? String.valueOf(cell.getNumericCellValue())
                    : cell.getStringCellValue().trim();
            default -> "";
        };
    }

    private Integer getColIndex(Map<Integer, String> colIndexToKey, String key) {
        return colIndexToKey.entrySet().stream()
                .filter(e -> key.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);
    }

    /**
     * "40g", "300mg", "160 kcal" 등에서 숫자만 추출.
     * TODO: 빈 값 또는 파싱 실패 시 0 반환.
     */
    private BigDecimal parseNutrient(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        String numOnly = raw.replaceAll("[^0-9.]", "").trim();
        if (numOnly.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(numOnly);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private boolean isRowBlank(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }
}