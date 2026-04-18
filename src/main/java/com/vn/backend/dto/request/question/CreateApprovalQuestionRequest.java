package com.vn.backend.dto.request.question;

import com.vn.backend.enums.RequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateApprovalQuestionRequest {
    
    @NotNull(message = "Request type is required")
    private RequestType requestType;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotEmpty(message = "Question IDs are required")
    @NotNull(message = "Question IDs are required")
    private List<Long> questionIds;
}

