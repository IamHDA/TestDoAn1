package com.vn.backend.dto.request.sessionexam;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitExamRequest {

  private List<StudentAnswerRequest> answers;
}
