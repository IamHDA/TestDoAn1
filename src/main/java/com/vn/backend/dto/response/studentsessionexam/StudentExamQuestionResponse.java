package com.vn.backend.dto.response.studentsessionexam;

import com.vn.backend.dto.redis.ExamQuestionDTO;
import com.vn.backend.dto.response.examquestionsnapshot.ExamQuestionSnapshotResponse;
import com.vn.backend.enums.QuestionType;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentExamQuestionResponse {

  private Long id;
  private Long examId;
  private String questionContent;
  private String questionImageUrl;
  private QuestionType questionType;
  private Integer difficultyLevel;
  private Long topicId;
  private Double score;
  private Integer orderIndex;
  private List<ExamQuestionAnswerResponse> answers;

  public static StudentExamQuestionResponse mapToStudentExamQuestionResponse(
      ExamQuestionSnapshotResponse response) {
    if (response == null) {
      return null;
    }
    return StudentExamQuestionResponse.builder()
        .id(response.getId())
        .examId(response.getExamId())
        .questionContent(response.getQuestionContent())
        .questionImageUrl(response.getQuestionImageUrl())
        .questionType(response.getQuestionType())
        .difficultyLevel(response.getDifficultyLevel())
        .topicId(response.getTopicId())
        .score(response.getScore())
        .orderIndex(response.getOrderIndex())
        .answers(
            response.getAnswers().stream().map(
                e -> ExamQuestionAnswerResponse.builder()
                    .id(e.getId())
                    .answerContent(e.getAnswerContent())
                    .displayOrder(e.getDisplayOrder())
                    .isCorrect(e.getIsCorrect())
                    .build()
            ).toList()
        )
        .build();
  }

  public static StudentExamQuestionResponse fromDTO(
      ExamQuestionDTO dto) {
    if (dto == null) {
      return null;
    }

    return StudentExamQuestionResponse.builder()
        .id(dto.getId())
        .examId(dto.getExamId())
        .questionContent(dto.getQuestionContent())
        .questionImageUrl(dto.getQuestionImageUrl())
        .questionType(dto.getQuestionType())
        .difficultyLevel(dto.getDifficultyLevel())
        .topicId(dto.getTopicId())
        .score(dto.getScore())
        .orderIndex(dto.getOrderIndex())
        .answers(dto.getAnswers() != null ?
            dto.getAnswers().stream()
                .map(ExamQuestionAnswerResponse::mapToExamQuestionAnswerResponse
                )
                .collect(Collectors.toList())
            : null)
        .build();
  }
}
