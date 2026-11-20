package com.cinx.learning.note.api.mapper;

import com.cinx.learning.note.api.dto.request.LessonNoteRequest;
import com.cinx.learning.note.api.dto.response.LessonNoteResponse;
import com.cinx.learning.note.application.command.LessonNoteCommand;
import com.cinx.learning.note.application.query.LessonNoteView;
import org.mapstruct.Mapper;

/** Converts the api's transport types to the application's commands and the application's views to transport types. */
@Mapper(componentModel = "spring")
public interface NoteApiMapper {
    LessonNoteCommand toCommand(LessonNoteRequest request);
    LessonNoteResponse toResponse(LessonNoteView view);
}
