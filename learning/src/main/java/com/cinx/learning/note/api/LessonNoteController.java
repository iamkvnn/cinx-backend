package com.cinx.learning.note.api;

import com.cinx.learning.note.api.mapper.NoteApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.note.api.dto.request.LessonNoteRequest;
import com.cinx.learning.note.api.dto.response.LessonNoteResponse;
import com.cinx.learning.note.application.ILessonNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The learner's own notes on lessons of every type (replaces /video-notes). */
@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class LessonNoteController {
    private final ILessonNoteService noteService;
    private final NoteApiMapper noteApiMapper;

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/notes")
    @Operation(summary = "Add a note to a lesson", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<LessonNoteResponse>> create(@PathVariable String courseId, @PathVariable String lessonId,
                                                                  @Valid @RequestBody LessonNoteRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Note created successfully",
                noteApiMapper.toResponse(noteService.create(AuthenticationUtil.extractUserId(), courseId, lessonId, noteApiMapper.toCommand(request)))));
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}/notes")
    @Operation(summary = "My notes on a lesson", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<LessonNoteResponse>>> listByLesson(@PathVariable String courseId, @PathVariable String lessonId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Notes fetched successfully",
                noteService.listByLesson(AuthenticationUtil.extractUserId(), courseId, lessonId).stream().map(noteApiMapper::toResponse).toList()));
    }

    @GetMapping("/courses/{courseId}/notes")
    @Operation(summary = "My notes across a course", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<LessonNoteResponse>>> listByCourse(@PathVariable String courseId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Notes fetched successfully",
                noteService.listByCourse(AuthenticationUtil.extractUserId(), courseId).stream().map(noteApiMapper::toResponse).toList()));
    }

    @PutMapping("/notes/{noteId}")
    @Operation(summary = "Edit my note", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<LessonNoteResponse>> update(@PathVariable String noteId, @Valid @RequestBody LessonNoteRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Note updated successfully",
                noteApiMapper.toResponse(noteService.update(AuthenticationUtil.extractUserId(), noteId, noteApiMapper.toCommand(request)))));
    }

    @DeleteMapping("/notes/{noteId}")
    @Operation(summary = "Delete my note", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String noteId) {
        noteService.delete(AuthenticationUtil.extractUserId(), noteId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Note deleted successfully", null));
    }
}
