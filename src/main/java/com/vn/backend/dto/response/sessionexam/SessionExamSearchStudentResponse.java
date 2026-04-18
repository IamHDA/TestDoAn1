package com.vn.backend.dto.response.sessionexam;

import com.vn.backend.dto.response.classroom.ClassroomResponse;
import com.vn.backend.enums.ExamMode;
import com.vn.backend.enums.ExamSubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SessionExamSearchStudentResponse {
    private Long sessionExamId;
    private ClassroomResponse classroom;
    private String title;
    private Long duration;
    private ExamMode examMode;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ExamSubmissionStatus submissionStatus;

    public static SessionExamSearchStudentResponse fromDTO(SessionExamStudentQueryDTO dto) {
        return SessionExamSearchStudentResponse.builder()
                .sessionExamId(dto.getSessionExamId())
                .classroom(
                        ClassroomResponse.builder().classroomId(dto.getClassroomId()).className(
                                dto.getClassroomName()).build()
                )
                .title(dto.getTitle())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .duration(dto.getDuration())
                .examMode(dto.getExamMode())
                .submissionStatus(dto.getSubmissionStatus())
                .build();
    }
}
