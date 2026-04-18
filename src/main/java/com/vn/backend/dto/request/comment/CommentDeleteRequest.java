package com.vn.backend.dto.request.comment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDeleteRequest {

    @NotNull(message = "Comment ID is required")
    private Long commentId;
}
