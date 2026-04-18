package com.vn.backend.dto.response.sessionexam;

import com.vn.backend.enums.ExamMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionExamTeacherQueryDTO {

    private Long sessionExamId;
    private Long classroomId;
    private String classroomName;
    private String title;
    private Long duration;
    private ExamMode examMode;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
