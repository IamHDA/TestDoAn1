package com.vn.backend.dto.request.classroom;

import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassMemberSearchRequestDTO {

  private Long classroomId;

  private String keyword;

  private ClassMemberRole classMemberRole;

  private ClassMemberStatus classMemberStatus;
}
