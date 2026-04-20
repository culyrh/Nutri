package com.example.nutriuniv.domain.logging.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.domain.logging.dto.SearchLogRequest;
import com.example.nutriuniv.domain.logging.dto.ViewLogRequest;
import com.example.nutriuniv.domain.logging.service.LoggingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Logging", description = "로그 API")
@RestController
@RequiredArgsConstructor
public class LoggingController {

    private final LoggingService loggingService;

    // POST /logging/view
    @Operation(summary = "상품 조회 로그 기록",
            description = "상품 상세 조회 시 클라이언트가 호출합니다. " +
                    "로그 저장 실패 시에도 200을 반환합니다. " +
                    "비로그인 시 user_id는 null로 저장됩니다.")
    @PostMapping("/logging/view")
    public ResponseEntity<CommonResponse<Void>> logView(
            @RequestBody ViewLogRequest request,
            HttpServletRequest httpServletRequest) {

        Long userId = null;   // TODO: 인증 구현 후 SecurityContext에서 꺼낼 예정
        String ipAddress = resolveClientIp(httpServletRequest);
        loggingService.logProductView(request, userId, ipAddress);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // POST /logging/search
    @Operation(summary = "검색 로그 기록",
            description = "검색 실행 시 클라이언트가 호출합니다. " +
                    "keyword가 빈 문자열이면 400을 반환합니다. " +
                    "로그 저장 실패 시에도 200을 반환합니다. " +
                    "비로그인 시 user_id는 null로 저장됩니다.")
    @PostMapping("/logging/search")
    public ResponseEntity<CommonResponse<Void>> logSearch(
            @RequestBody SearchLogRequest request) {

        Long userId = null;   // TODO: 인증 구현 후 SecurityContext에서 꺼낼 예정
        loggingService.logSearch(request, userId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            return request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
