package com.cinx.learning.instructor.application;

import com.cinx.learning.catalog.application.CatalogLookup;

import com.cinx.common.paging.PageQuery;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.learning.catalog.application.query.CourseInfo;
import com.cinx.learning.shared.domain.ActivityAction;
import com.cinx.learning.shared.domain.InboxKind;
import com.cinx.learning.instructor.application.query.ActivityView;
import com.cinx.learning.instructor.domain.InstructorActivity;
import com.cinx.learning.instructor.domain.InstructorActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstructorActivityService implements IInstructorActivityService {
    private final CatalogLookup catalogLookup;
    static final int MAX_ROWS = 500;
    static final int MAX_SUMMARY = 255;

    private final InstructorActivityRepository activityRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void record(String instructorId, String courseId, InboxKind kind, ActivityAction action,
                       String targetId, int itemCount, String summary) {
        if (itemCount <= 0) {
            return;
        }
        String text = summary == null ? "" : summary;
        activityRepository.save(InstructorActivity.builder()
                .instructorId(instructorId)
                .courseId(courseId)
                .kind(kind)
                .action(action)
                .targetId(targetId)
                .itemCount(itemCount)
                .summary(text.length() > MAX_SUMMARY ? text.substring(0, MAX_SUMMARY) : text)
                .createdAt(LocalDateTime.now(clock))
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityView> recent(String instructorId, InboxKind kind, String courseId, int days) {
        if (days < 1 || days > 90) {
            throw new BadRequestException(ErrorCode.DATE_RANGE_INVALID, "days must be between 1 and 90");
        }
        LocalDateTime since = LocalDateTime.now(clock).minusDays(days);
        List<InstructorActivity> rows = activityRepository.findRecent(
                instructorId, since, kind, blankToNull(courseId), PageQuery.first(MAX_ROWS));
        Map<String, String> titles = catalogLookup.courses(rows.stream().map(InstructorActivity::getCourseId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(CourseInfo::getId, CourseInfo::getTitle, (a, b) -> a));
        return rows.stream()
                .map(row -> new ActivityView(
                        row.getId(),
                        row.getKind(),
                        row.getAction(),
                        row.getCourseId(),
                        titles.get(row.getCourseId()),
                        row.getTargetId(),
                        row.getItemCount(),
                        row.getSummary(),
                        row.getCreatedAt().toInstant(ZoneOffset.UTC)))
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() || "all".equals(value) ? null : value;
    }
}
