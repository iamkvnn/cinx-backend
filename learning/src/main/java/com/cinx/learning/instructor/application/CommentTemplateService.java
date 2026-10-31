package com.cinx.learning.instructor.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.instructor.domain.CommentTemplateKind;
import com.cinx.learning.instructor.application.query.CommentTemplateDtos;
import com.cinx.learning.instructor.domain.InstructorCommentTemplate;
import com.cinx.learning.instructor.domain.InstructorCommentTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;

/** An instructor's own saved comments; nobody else reads or deletes them. */
@Service
@RequiredArgsConstructor
public class CommentTemplateService implements ICommentTemplateService {
    static final int MAX_TEMPLATES = 200;

    private final InstructorCommentTemplateRepository templateRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CommentTemplateDtos.Response> list(String instructorId, CommentTemplateKind kind) {
        return templateRepository.findAllByInstructorIdAndKindOrderByCreatedAtDesc(instructorId, kind).stream()
                .map(CommentTemplateService::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CommentTemplateDtos.Response create(String instructorId, CommentTemplateDtos.CreateRequest request) {
        if (templateRepository.countByInstructorId(instructorId) >= MAX_TEMPLATES) {
            throw new BadRequestException(ErrorCode.QUOTA_EXCEEDED, "At most " + MAX_TEMPLATES + " saved comments");
        }
        return toResponse(templateRepository.save(InstructorCommentTemplate.builder()
                .instructorId(instructorId)
                .kind(request.kind())
                .content(request.content().trim())
                .build()));
    }

    @Override
    @Transactional
    public void delete(String instructorId, String templateId) {
        InstructorCommentTemplate template = templateRepository.findById(templateId)
                .filter(t -> t.getInstructorId().equals(instructorId))
                .orElseThrow(() -> new NotFoundException("Saved comment not found"));
        templateRepository.delete(template);
    }

    private static CommentTemplateDtos.Response toResponse(InstructorCommentTemplate t) {
        return new CommentTemplateDtos.Response(t.getId(), t.getKind(), t.getContent(),
                t.getCreatedAt() == null ? null : t.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
    }
}
