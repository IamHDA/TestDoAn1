package com.vn.backend.services;

import com.vn.backend.dto.response.examquestionsnapshot.ExamQuestionSnapshotResponse;
import com.vn.backend.enums.QuestionOrderMode;
import java.util.List;

public interface ExamQuestionSnapshotService {

  void createExamQuestionSnapshots(Long examId, Long sessionExamId);

  List<ExamQuestionSnapshotResponse> getAllQuestions(Long sessionExamId, QuestionOrderMode orderMode);
}
