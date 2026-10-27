package com.cinx.learning.instructor.application;

import com.cinx.learning.instructor.domain.CommentTemplateKind;
import com.cinx.learning.instructor.application.query.CommentTemplateDtos;

import java.util.List;

public interface ICommentTemplateService {
    List<CommentTemplateDtos.Response> list(String instructorId, CommentTemplateKind kind);

    CommentTemplateDtos.Response create(String instructorId, CommentTemplateDtos.CreateRequest request);

    void delete(String instructorId, String templateId);
}
