package com.cinx.learning.catalog.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.catalog.application.command.CreateCourseImageCommand;
import com.cinx.learning.catalog.application.command.ReorderCourseImagesCommand;
import com.cinx.learning.catalog.application.command.UpdateCourseImageCommand;
import com.cinx.learning.catalog.application.query.CourseImageView;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseImage;
import com.cinx.learning.catalog.domain.CourseImageRepository;
import com.cinx.learning.catalog.domain.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseImageService implements ICourseImageService{
    private final CourseImageRepository courseImageRepository;
    private final CourseRepository courseRepository;
    private final ICourseAccessService courseAccessService;

    @Value("${aws.s3.cdn-url}")
    private String cdnUrl;

    @Override
    @Transactional
    public void saveCourseImages(String currentUserId, String courseId, CreateCourseImageCommand request) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureEditableCourse(currentUserId, course);
        int nextOrder = course.getImages().stream().mapToInt(CourseImage::getSortOrder).max().orElse(-1) + 1;
        for (CreateCourseImageCommand.ImageDto dto : request.getImages()) {
            course.getImages().add(
                    CourseImage.builder()
                            .courseId(course.getId())
                            .imageUrl(cdnUrl + "/" + dto.getFileKey())
                            .publicId(dto.getFileKey())
                            .sortOrder(nextOrder++)
                            .build()
            );
        }
        courseImageRepository.saveAll(course.getImages());
    }

    @Override
    @Transactional
    public void updateCourseImage(String currentUserId, String courseId, String imageId, UpdateCourseImageCommand request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureEditableCourse(currentUserId, course);
        CourseImage image = courseImageRepository.findById(imageId)
                .filter(candidate -> courseId.equals(candidate.getCourseId()))
                .orElseThrow(() -> new NotFoundException("Course image not found"));
        image.setImageUrl(cdnUrl + "/" + request.getFileKey());
        image.setPublicId(request.getFileKey());
        courseImageRepository.save(image);
    }

    @Override
    @Transactional
    public void deleteImage(String currentUserId, String courseId, String imageId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureEditableCourse(currentUserId, course);
        CourseImage image = courseImageRepository.findById(imageId)
                .filter(candidate -> courseId.equals(candidate.getCourseId()))
                .orElseThrow(() -> new NotFoundException("Course image not found"));
        courseImageRepository.delete(image);
    }

    @Override
    @Transactional
    public List<CourseImageView> reorderImages(String currentUserId, String courseId, ReorderCourseImagesCommand request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureEditableCourse(currentUserId, course);

        List<String> requested = request.getImageIds();
        Map<String, CourseImage> byId = course.getImages().stream()
                .collect(Collectors.toMap(CourseImage::getId, Function.identity()));
        if (requested.size() != byId.size() || !byId.keySet().equals(new HashSet<>(requested))) {
            throw new BadRequestException("Image ids must list every image of the course exactly once");
        }

        for (int position = 0; position < requested.size(); position++) {
            byId.get(requested.get(position)).setSortOrder(position);
        }
        courseImageRepository.saveAll(byId.values());
        return requested.stream()
                .map(id -> new CourseImageView(id, byId.get(id).getImageUrl()))
                .toList();
    }
}

