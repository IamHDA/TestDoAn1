package com.vn.backend.entities.classid;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExamQuestionId implements Serializable {

    private static final long serialVersionUID = 389060772153017116L;
    private Long examId;
    private Long questionId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        ExamQuestionId examQuestionId = (ExamQuestionId) o;
        return Objects.equals(this.examId, examQuestionId.examId)
                && Objects.equals(this.questionId, examQuestionId.questionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.examId, this.questionId);
    }
}
