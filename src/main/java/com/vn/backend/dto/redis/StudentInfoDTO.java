package com.vn.backend.dto.redis;

import com.vn.backend.entities.User;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentInfoDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long studentId;
  private String studentCode;
  private String fullName;
  private String email;

  public static StudentInfoDTO fromUser(User user) {
    return StudentInfoDTO.builder()
        .studentId(user.getId())
        .studentCode(user.getCode())
        .fullName(user.getFullName())
        .email(user.getEmail())
        .build();
  }
}
