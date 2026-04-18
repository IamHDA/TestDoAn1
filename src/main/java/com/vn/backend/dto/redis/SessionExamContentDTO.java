package com.vn.backend.dto.redis;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionExamContentDTO {

  private List<ExamQuestionDTO> questions;
}
