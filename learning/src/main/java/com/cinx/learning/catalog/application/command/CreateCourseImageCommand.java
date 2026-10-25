package com.cinx.learning.catalog.application.command;

import lombok.Data;
import java.util.List;

@Data
public class CreateCourseImageCommand {
    private List<ImageDto> images;

    @Data
    public static class ImageDto {
        private String fileKey;
    }
}

