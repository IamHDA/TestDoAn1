package com.vn.backend.dto.request.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateRequest {

    @NotBlank(message = "Comment content is required")
    @Size(min = 1, max = 2000, message = "Comment content must be between 1 and 2000 characters")
    private String content;
}
