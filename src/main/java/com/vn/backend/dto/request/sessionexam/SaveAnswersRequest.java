package com.vn.backend.dto.request.sessionexam;


import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class SaveAnswersRequest {

  private List<StudentAnswerRequest> answers;
}
