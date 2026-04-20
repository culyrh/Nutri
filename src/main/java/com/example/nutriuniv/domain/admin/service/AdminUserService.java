package com.example.nutriuniv.domain.admin.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.domain.admin.dto.AdminUserPageResponse;
import com.example.nutriuniv.domain.admin.dto.AdminUserResponse;
import com.example.nutriuniv.domain.admin.dto.AdminUserRoleRequest;
import com.example.nutriuniv.domain.user.entity.User;
import com.example.nutriuniv.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "ADMIN");

    private final UserRepository userRepository;

    // ── GET /admin/users ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminUserPageResponse getUsers(String keyword, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new CustomException(ErrorCode.INVALID_QUERY_PARAM, "page는 0 이상, size는 1 이상이어야 합니다.");
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.searchUsers(keyword, pageable);

        List<AdminUserResponse> items = userPage.getContent().stream()
                .map(AdminUserResponse::from)
                .collect(Collectors.toList());

        return AdminUserPageResponse.builder()
                .total(userPage.getTotalElements())
                .page(page)
                .size(size)
                .items(items)
                .build();
    }

    // ── PATCH /admin/users/{userId}/role ──────────────────────────────────────────

    @Transactional
    public void updateUserRole(Long adminId, Long userId, AdminUserRoleRequest request) {
        if (request.getRole() == null || !ALLOWED_ROLES.contains(request.getRole().toUpperCase())) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "허용되지 않는 role 값입니다. (USER / ADMIN)");
        }
        if (adminId.equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "본인의 role은 변경할 수 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateRole(request.getRole().toUpperCase());
    }
}
