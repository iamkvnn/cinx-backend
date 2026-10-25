package com.cinx.learning.catalog.application.command;

import lombok.Data;

import java.util.List;

@Data
public class ReorderCourseImagesCommand {
    private List<String> imageIds;
}
