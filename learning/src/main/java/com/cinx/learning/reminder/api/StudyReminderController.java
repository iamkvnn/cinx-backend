package com.cinx.learning.reminder.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.reminder.api.dto.request.StudyReminderRequest;
import com.cinx.learning.reminder.api.dto.response.StudyReminderResponse;
import com.cinx.learning.reminder.application.StudyReminderPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in learner's study reminder: on or off, and the hour (Vietnam time, 8 to 20). */
@RestController
@RequestMapping("/api/v1/learning/study-reminder")
@RequiredArgsConstructor
public class StudyReminderController {
    private final StudyReminderPreferenceService preferences;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_learning.progress.read.self')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<StudyReminderResponse>> get() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Study reminder fetched successfully",
                StudyReminderResponse.of(preferences.get(AuthenticationUtil.extractUserId()))));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('PERM_learning.progress.write.self')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<StudyReminderResponse>> update(@Valid @RequestBody StudyReminderRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Study reminder saved successfully",
                StudyReminderResponse.of(preferences.update(AuthenticationUtil.extractUserId(), request.enabled(), request.hour()))));
    }
}
