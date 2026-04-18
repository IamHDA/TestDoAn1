package com.vn.backend.dto.request.sessionexam;

import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class StudentAnswerRequest {

  @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.QUESTION_ID)
  private Long questionSnapshotId;

  @Size(min = 1, message = "Phải chọn ít nhất 1 đáp án")
  private List<Long> selectedAnswerIds;
}
