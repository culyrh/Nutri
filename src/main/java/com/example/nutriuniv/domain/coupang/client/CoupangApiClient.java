package com.example.nutriuniv.domain.coupang.client;

import com.example.nutriuniv.domain.coupang.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Component
public class CoupangApiClient {

    @Value("${coupang.access-key}")
    private String accessKey;

    @Value("${coupang.secret-key}")
    private String secretKey;

    private static final String BASE_URL    = "https://api-gateway.coupang.com";
    private static final String SEARCH_PATH = "/v2/providers/affiliate_open_api/apis/openapi/v1/products/search";
    private static final String REPORT_PATH = "/v2/providers/affiliate_open_api/apis/openapi/v1/reports";

    private final RestTemplate restTemplate = new RestTemplate();

    // ── HMAC 서명 ─────────────────────────────────────────────────────────────────

    private HttpHeaders createHeaders(String method, String path, String queryString) {
        try {
            String datetime = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
            String message = datetime + "\n" + method + "\n" + path + "\n" + queryString;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization",
                    "CEA algorithm=HmacSHA256, access-key=" + accessKey +
                    ", signed-date=" + datetime + ", signature=" + signature);
            return headers;
        } catch (Exception e) {
            throw new RuntimeException("HMAC 서명 생성 실패", e);
        }
    }

    // ── 상품 검색 ─────────────────────────────────────────────────────────────────

    public CoupangSearchResponse.SearchData searchProduct(String keyword) {
        String queryString = "keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8) + "&limit=10";

        ResponseEntity<CoupangSearchResponse> response = restTemplate.exchange(
                BASE_URL + SEARCH_PATH + "?" + queryString,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders("GET", SEARCH_PATH, queryString)),
                CoupangSearchResponse.class
        );

        CoupangSearchResponse body = response.getBody();
        if (body == null || !"0".equals(body.getRCode()) ||
                body.getData() == null || body.getData().getProductData() == null ||
                body.getData().getProductData().isEmpty()) {
            return null;
        }
        return body.getData();
    }

    // ── 리포트 조회 ───────────────────────────────────────────────────────────────

    public List<CoupangClickData> getClickReport(String startDate, String endDate) {
        String path = REPORT_PATH + "/clicks";
        String queryString = "startDate=" + startDate + "&endDate=" + endDate;
        ResponseEntity<CoupangClickResponse> response = restTemplate.exchange(
                BASE_URL + path + "?" + queryString,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders("GET", path, queryString)),
                CoupangClickResponse.class
        );
        CoupangClickResponse body = response.getBody();
        return body != null && body.getData() != null ? body.getData() : List.of();
    }

    public List<CoupangOrderData> getOrderReport(String startDate, String endDate) {
        String path = REPORT_PATH + "/orders";
        String queryString = "startDate=" + startDate + "&endDate=" + endDate;
        ResponseEntity<CoupangOrderResponse> response = restTemplate.exchange(
                BASE_URL + path + "?" + queryString,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders("GET", path, queryString)),
                CoupangOrderResponse.class
        );
        CoupangOrderResponse body = response.getBody();
        return body != null && body.getData() != null ? body.getData() : List.of();
    }

    public List<CoupangCancelData> getCancelReport(String startDate, String endDate) {
        String path = REPORT_PATH + "/cancels";
        String queryString = "startDate=" + startDate + "&endDate=" + endDate;
        ResponseEntity<CoupangCancelResponse> response = restTemplate.exchange(
                BASE_URL + path + "?" + queryString,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders("GET", path, queryString)),
                CoupangCancelResponse.class
        );
        CoupangCancelResponse body = response.getBody();
        return body != null && body.getData() != null ? body.getData() : List.of();
    }

    public List<CoupangCommissionData> getCommissionReport(String startDate, String endDate) {
        String path = REPORT_PATH + "/commission";
        String queryString = "startDate=" + startDate + "&endDate=" + endDate;
        ResponseEntity<CoupangCommissionResponse> response = restTemplate.exchange(
                BASE_URL + path + "?" + queryString,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders("GET", path, queryString)),
                CoupangCommissionResponse.class
        );
        CoupangCommissionResponse body = response.getBody();
        return body != null && body.getData() != null ? body.getData() : List.of();
    }
}
