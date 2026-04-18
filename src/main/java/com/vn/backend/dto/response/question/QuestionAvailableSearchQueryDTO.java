package com.vn.backend.dto.response.question;

import com.vn.backend.entities.Answer;
import com.vn.backend.entities.Topic;
import com.vn.backend.enums.QuestionType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAvailableSearchQueryDTO {

  private Long id;
  private String content;
  private QuestionType type;
  private Integer difficultyLevel;
  private Topic topic;
  private Boolean added;
  private Boolean isReviewQuestion;
}
