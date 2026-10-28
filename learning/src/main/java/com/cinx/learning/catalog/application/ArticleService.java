package com.cinx.learning.catalog.application;

import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.command.CreateArticleLessonCommand;
import com.cinx.learning.catalog.application.command.UpdateArticleLessonCommand;
import com.cinx.learning.catalog.application.query.ArticleLessonView;
import com.cinx.learning.catalog.application.mapper.ArticleMapper;
import com.cinx.learning.catalog.domain.ArticleLessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleService implements IArticleService {
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final ArticleLessonRepository articleLessonRepository;
    private final ArticleMapper articleLessonMapper;
    private final ILessonService lessonService;

    @Value("${aws.s3.cdn-url}")
    private String cdnUrl;

    @Override
    @Transactional(readOnly = true)
    public ArticleLessonView getArticleByLessonId(String currentUserId, String courseId, String lessonId) {
        lessonService.ensureCanReadLessonContent(currentUserId, courseId, lessonId, LessonType.ARTICLE);
        return getArticleOrThrow(lessonId);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleLessonView getReadableArticleByLessonId(String currentUserId, String courseId, String lessonId) {
        lessonService.ensureCanReadLessonContent(currentUserId, courseId, lessonId, LessonType.ARTICLE);
        return getArticleOrThrow(lessonId);
    }

    private ArticleLessonView getArticleOrThrow(String lessonId) {
        return articleLessonRepository.findByLessonId(lessonId)
                .map(articleLessonMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Article not found for lessonId: " + lessonId));
    }

    @Override
    @Transactional
    public void createArticle(String currentUserId, String courseId, String lessonId, CreateArticleLessonCommand request) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.ARTICLE);
        validatePdf(request.getFileType());
        articleLessonRepository.findByLessonId(lessonId).ifPresentOrElse(existing -> {
            throw new AlreadyExistException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Article already exists for lessonId: " + lessonId);
        },() -> {
            var articleLesson = articleLessonMapper.toModel(request);
            articleLesson.setLessonId(lessonId);
            articleLesson.setArticleUrl(publicUrl(request.getFileKey()));
            articleLessonRepository.save(articleLesson);
        });
    }

    @Override
    @Transactional
    public void updateArticle(String currentUserId, String courseId, String lessonId, UpdateArticleLessonCommand request) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.ARTICLE);
        validatePdf(request.getFileType());
        articleLessonRepository.findByLessonId(lessonId).ifPresentOrElse(existing -> {
            articleLessonMapper.partialUpdate(existing, request);
            if (request.getFileKey() != null) {
                existing.setArticleUrl(publicUrl(request.getFileKey()));
            }
            articleLessonRepository.save(existing);
        },() -> {
            throw new NotFoundException("Article not found for lessonId: " + lessonId);
        });
    }

    private void validatePdf(String fileType) {
        if (fileType == null || !PDF_CONTENT_TYPE.equalsIgnoreCase(fileType)) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Article lesson file must be a PDF");
        }
    }

    private String publicUrl(String fileKey) {
        return cdnUrl.endsWith("/") ? cdnUrl + fileKey : cdnUrl + "/" + fileKey;
    }
}

