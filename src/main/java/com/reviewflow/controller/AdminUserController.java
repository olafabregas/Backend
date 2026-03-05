package com.reviewflow.controller;

import com.reviewflow.model.dto.request.CreateUserRequest;
import com.reviewflow.model.dto.response.ApiResponse;
import com.reviewflow.model.dto.response.AuthUserResponse;
import com.reviewflow.model.dto.response.UserDetailResponse;
import com.reviewflow.model.entity.User;
import com.reviewflow.model.entity.UserRole;
import com.reviewflow.security.ReviewFlowUserDetails;
import com.reviewflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuthUserResponse>>> list(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<User> page = userService.listUsersFiltered(role, active, search, pageable);
        Page<AuthUserResponse> data = page.map(this::toResponse);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getById(@PathVariable Long id) {
        UserDetailResponse user = userService.getUserByIdWithCounts(id);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AuthUserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(
                request.getEmail(), request.getPassword(),
                request.getFirstName(), request.getLastName(),
                request.getRole() != null ? request.getRole() : UserRole.STUDENT);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(toResponse(user)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AuthUserResponse>> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String firstName = (String) body.get("firstName");
        String lastName = (String) body.get("lastName");
        UserRole role = body.get("role") != null ? UserRole.valueOf(body.get("role").toString()) : null;
        User user = userService.updateUser(id, firstName, lastName, role);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(user)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal ReviewFlowUserDetails principal) {
        userService.deactivateUser(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "User deactivated",
                "isActive", false
        )));
    }
    
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reactivate(@PathVariable Long id) {
        userService.reactivateUser(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "User reactivated",
                "isActive", true
        )));
    }

    private AuthUserResponse toResponse(User u) {
        return AuthUserResponse.builder()
                .userId(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .role(u.getRole())
                .build();
    }
}
