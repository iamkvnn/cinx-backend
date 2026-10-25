package com.cinx.learning.catalog.application.query;

public record ArticleLessonView (
        String articleUrl,
        String fileName,
        String fileType,
        Long fileSize
){
}

