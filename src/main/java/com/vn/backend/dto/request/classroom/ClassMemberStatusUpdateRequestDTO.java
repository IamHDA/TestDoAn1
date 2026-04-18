package com.vn.backend.dto.request.classroom;

import com.vn.backend.enums.ClassMemberStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassMemberStatusUpdateRequestDTO {

  private ClassMemberStatus classMemberStatus;
}
