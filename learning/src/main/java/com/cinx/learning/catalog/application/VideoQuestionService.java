package com.cinx.learning.catalog.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.LessonType;
import com.cinx.learning.catalog.application.command.CreateVideoQuestionCommand;
import com.cinx.learning.catalog.application.command.UpdateVideoQuestionCommand;
import com.cinx.learning.catalog.application.query.VideoOptionView;
import com.cinx.learning.catalog.application.query.VideoQuestionView;
import com.cinx.learning.catalog.application.mapper.VideoOptionMapper;
import com.cinx.learning.catalog.application.mapper.VideoQuestionMapper;
import com.cinx.learning.catalog.domain.VideoLesson;
import com.cinx.learning.catalog.domain.VideoOption;
import com.cinx.learning.catalog.domain.VideoQuestion;
import com.cinx.learning.catalog.domain.VideoLessonRepository;
import com.cinx.learning.catalog.domain.VideoOptionRepository;
import com.cinx.learning.catalog.domain.VideoQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoQuestionService implements IVideoQuestionService {

    private final VideoQuestionRepository videoQuestionRepository;
    private final VideoOptionRepository videoOptionRepository;
    private final VideoLessonRepository videoLessonRepository;
    private final VideoQuestionMapper videoQuestionMapper;
    private final VideoOptionMapper videoOptionMapper;
    private final ILessonService lessonService;

    @Override
    @Transactional(readOnly = true)
    public List<VideoQuestionView> getQuestionsByLessonId(String currentUserId, String courseId, String lessonId) {
        lessonService.ensureCanReadLessonContent(currentUserId, courseId, lessonId, LessonType.VIDEO);
        videoLessonRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new NotFoundException("Video lesson not found"));
        
        List<VideoQuestion> questions = videoQuestionRepository.findByVideoLessonLessonIdOrderByTimestampSecondsAsc(lessonId);
        
        boolean isInstructor = lessonService.isLessonInstructor(lessonId, currentUserId);

        if (isInstructor) {
            return questions.stream().map(videoQuestionMapper::toDto).collect(Collectors.toList());
        } else {
            return questions.stream()
                    .map(videoQuestionMapper::toDto)
                    .map(q -> new VideoQuestionView(
                            q.id(),
                            q.questionText(),
                            q.questionType(),
                            q.timestampSeconds(),
                            q.options().stream()
                                    .map(o -> new VideoOptionView(o.id(), o.optionText(), null))
                                    .toList()
                    ))
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VideoQuestionView getQuestionById(String currentUserId, String courseId, String lessonId, String id) {
        lessonService.ensureCanReadLessonContent(currentUserId, courseId, lessonId, LessonType.VIDEO);
        VideoQuestion question = videoQuestionRepository.findByIdAndVideoLessonLessonId(id, lessonId)
                .orElseThrow(() -> new NotFoundException("Video question not found"));

        VideoQuestionView q = videoQuestionMapper.toDto(question);
        boolean isInstructor = lessonService.isLessonInstructor(lessonId, currentUserId);
        if (isInstructor) {
            return q;
        }
        return new VideoQuestionView(
                q.id(),
                q.questionText(),
                q.questionType(),
                q.timestampSeconds(),
                q.options().stream()
                        .map(o -> new VideoOptionView(o.id(), o.optionText(), null))
                        .toList()
        );
    }

    @Override
    @Transactional
    public VideoQuestionView createQuestion(String currentUserId, String courseId, String lessonId, CreateVideoQuestionCommand request) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.VIDEO);
        VideoLesson videoLesson = videoLessonRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new NotFoundException("Video lesson not found"));

        if (request.timestampSeconds() < 0 || request.timestampSeconds() > videoLesson.getDuration()) {
            throw new BadRequestException(ErrorCode.VIDEO_QUESTION_TIMESTAMP_INVALID, "Timestamp must be between 0 and video duration (" + videoLesson.getDuration() + "s)");
        }

        VideoQuestion question = videoQuestionMapper.toModel(request);
        question.setVideoLessonId(videoLesson.getLessonId());
        
        List<VideoOption> options = request.options().stream().map(optionRequest -> {
            VideoOption option = videoOptionMapper.toModel(optionRequest);
            option.setVideoQuestionId(question.getId());
            return option;
        }).toList();
        
        question.setOptions(options);

        VideoQuestion savedQuestion = videoQuestionRepository.save(question);
        return videoQuestionMapper.toDto(savedQuestion);
    }

    @Override
    @Transactional
    public VideoQuestionView updateQuestion(String currentUserId, String courseId, String lessonId, String id, UpdateVideoQuestionCommand request) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.VIDEO);
        VideoQuestion question = videoQuestionRepository.findByIdAndVideoLessonLessonId(id, lessonId)
                .orElseThrow(() -> new NotFoundException("Video question not found"));

        VideoLesson videoLesson = videoLessonRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new NotFoundException("Video lesson not found for lessonId: " + lessonId));
        if (request.timestampSeconds() < 0 || request.timestampSeconds() > videoLesson.getDuration()) {
            throw new BadRequestException(ErrorCode.VIDEO_QUESTION_TIMESTAMP_INVALID, "Timestamp must be between 0 and video duration (" + videoLesson.getDuration() + "s)");
        }

        videoQuestionMapper.partialUpdate(question, request);
        videoOptionRepository.deleteAll(question.getOptions());
        question.getOptions().clear();

        List<VideoOption> newOptions = request.options().stream().map(optionRequest -> {
            VideoOption option = videoOptionMapper.toModel(optionRequest);
            option.setVideoQuestionId(question.getId());
            return option;
        }).toList();

        question.getOptions().addAll(newOptions);

        VideoQuestion updatedQuestion = videoQuestionRepository.save(question);
        return videoQuestionMapper.toDto(updatedQuestion);
    }

    @Override
    @Transactional
    public void deleteQuestion(String currentUserId, String courseId, String lessonId, String id) {
        lessonService.ensureLessonInstructor(currentUserId, courseId, lessonId, LessonType.VIDEO);
        VideoQuestion question = videoQuestionRepository.findByIdAndVideoLessonLessonId(id, lessonId)
                .orElseThrow(() -> new NotFoundException("Video question not found"));
        videoQuestionRepository.delete(question);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkAnswer(String questionId, String userAnswer) {
        VideoQuestion question = videoQuestionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Video question not found"));
                
        List<VideoOption> correctOptions = question.getOptions().stream()
                .filter(VideoOption::getIsCorrect)
                .toList();
                
        if (correctOptions.isEmpty()) {
            return false;
        }
        
        String correctStr = correctOptions.stream()
                .map(VideoOption::getId)
                .sorted()
                .toList()
                .toString();

        return userAnswer.equals(correctStr);
    }
}

