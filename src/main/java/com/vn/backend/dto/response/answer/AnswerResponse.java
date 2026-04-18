package com.vn.backend.dto.response.answer;
import com.vn.backend.entities.Answer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerResponse {
    private Long id;
    private String content;
    private Boolean isCorrect;
    private Integer displayOrder;
    // ...

    public static AnswerResponse fromEntity(Answer entity){
        AnswerResponse answerResponse = new AnswerResponse();
        answerResponse.setId(entity.getAnswerId());
        answerResponse.setContent(entity.getContent());
        answerResponse.setIsCorrect(entity.getIsCorrect());
        answerResponse.setDisplayOrder(entity.getDisplayOrder());
        return answerResponse;
    }
}
