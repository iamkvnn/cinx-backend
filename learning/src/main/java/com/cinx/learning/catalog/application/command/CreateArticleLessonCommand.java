package com.cinx.learning.catalog.application.command;

import lombok.Data;

@Data
public class CreateArticleLessonCommand {
    private String fileKey;
    private String fileName;
    private String fileType;
    private Long fileSize;
}

