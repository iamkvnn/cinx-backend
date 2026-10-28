package com.cinx.learning.catalog.application;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.learning.catalog.domain.SubtitleFormat;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SubtitleFileProcessor {
    public static final long MAX_SUBTITLE_FILE_SIZE = 5 * 1024 * 1024;
    public static final String NORMALIZED_CONTENT_TYPE = "text/vtt";

    private static final Pattern VTT_TIMESTAMP = Pattern.compile(
            "\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+-->\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*"
    );
    private static final Pattern SRT_TIMESTAMP = Pattern.compile(
            "\\d{2}:\\d{2}:\\d{2},\\d{3}\\s+-->\\s+\\d{2}:\\d{2}:\\d{2},\\d{3}.*"
    );
    private static final Pattern VTT_TIMING = Pattern.compile(
            "(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+-->\\s+(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})"
    );

    public String normalizeLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            throw new BadRequestException(ErrorCode.SUBTITLE_INVALID, "Subtitle language code is required");
        }
        try {
            Locale locale = new Locale.Builder().setLanguageTag(languageCode.trim()).build();
            String normalized = locale.toLanguageTag();
            if (normalized == null || normalized.isBlank() || "und".equalsIgnoreCase(normalized)) {
                throw new BadRequestException(ErrorCode.SUBTITLE_INVALID, "Invalid subtitle language code");
            }
            return normalized;
        } catch (RuntimeException ex) {
            throw new BadRequestException(ErrorCode.SUBTITLE_INVALID, "Invalid subtitle language code");
        }
    }

    public SubtitleFormat detectFormat(String fileName, String contentType) {
        String lowerFileName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        String lowerContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);

        if (lowerFileName.endsWith(".vtt") || lowerContentType.equals("text/vtt")) {
            return SubtitleFormat.VTT;
        }
        if (lowerFileName.endsWith(".srt")
                || lowerContentType.equals("application/x-subrip")
                || lowerContentType.equals("text/srt")) {
            return SubtitleFormat.SRT;
        }
        throw new BadRequestException(ErrorCode.SUBTITLE_FILE_UNSUPPORTED, "Only .vtt and .srt subtitle files are supported");
    }

    public void validateFileSize(Long fileSize) {
        if (fileSize == null || fileSize < 1) {
            throw new BadRequestException(ErrorCode.SUBTITLE_FILE_INVALID, "Subtitle file size must be greater than 0");
        }
        if (fileSize > MAX_SUBTITLE_FILE_SIZE) {
            throw new BadRequestException(ErrorCode.SUBTITLE_FILE_TOO_LARGE, "Subtitle file size must not exceed 5MB");
        }
    }

    public String normalizeToWebVtt(SubtitleFormat format, String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException(ErrorCode.SUBTITLE_FILE_INVALID, "Subtitle file is empty");
        }
        String normalized = stripBom(content).replace("\r\n", "\n").replace('\r', '\n').trim();
        return switch (format) {
            case VTT -> validateWebVtt(normalized);
            case SRT -> convertSrtToWebVtt(normalized);
        };
    }

    /** Each cue of a WebVTT file as "start --> end|text", for telling which cues an edit left untouched. */
    public List<String> cueSignatures(String webVtt) {
        List<String> signatures = new ArrayList<>();
        if (webVtt == null) {
            return signatures;
        }
        String[] lines = stripBom(webVtt).replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (int index = 0; index < lines.length; index++) {
            Matcher timing = VTT_TIMING.matcher(lines[index].trim());
            if (!timing.lookingAt()) {
                continue;
            }
            StringBuilder text = new StringBuilder();
            while (index + 1 < lines.length && !lines[index + 1].isBlank()) {
                index++;
                text.append(text.isEmpty() ? "" : "\n").append(lines[index].trim());
            }
            signatures.add(timing.group(1) + " --> " + timing.group(2) + "|" + text);
        }
        return signatures;
    }

    private String validateWebVtt(String content) {
        if (!content.startsWith("WEBVTT")) {
            throw new BadRequestException(ErrorCode.SUBTITLE_FILE_INVALID, "WebVTT subtitle must start with WEBVTT");
        }
        boolean hasTiming = content.lines().anyMatch(line -> VTT_TIMESTAMP.matcher(line.trim()).matches());
        if (!hasTiming) {
            throw new BadRequestException(ErrorCode.SUBTITLE_FILE_INVALID, "Subtitle file must include at least one valid cue timestamp");
        }
        return content.endsWith("\n") ? content : content + "\n";
    }

    private String convertSrtToWebVtt(String content) {
        StringBuilder builder = new StringBuilder("WEBVTT\n\n");
        boolean hasTiming = false;
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (SRT_TIMESTAMP.matcher(trimmed).matches()) {
                hasTiming = true;
                builder.append(trimmed.replace(',', '.')).append('\n');
            } else if (!trimmed.matches("\\d+")) {
                builder.append(line).append('\n');
            }
        }
        if (!hasTiming) {
            throw new BadRequestException(ErrorCode.SUBTITLE_FILE_INVALID, "SRT subtitle must include at least one valid cue timestamp");
        }
        return builder.toString();
    }

    private String stripBom(String content) {
        return content.startsWith("\uFEFF") ? content.substring(1) : content;
    }
}

