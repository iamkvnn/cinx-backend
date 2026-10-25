package com.cinx.learning.catalog.application.command;

import lombok.Data;

@Data
public class CreateVideoLessonCommand {
    private String fileKey;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer duration;
}

