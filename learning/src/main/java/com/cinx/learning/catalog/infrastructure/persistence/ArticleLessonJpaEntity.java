package com.cinx.learning.catalog.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

@Entity(name = "ArticleLesson")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleLessonJpaEntity {
    @Id
    private String lessonId;

    private String articleUrl;
    private String fileKey;
    private String fileName;
    private String fileType;
    private Long fileSize;
}

