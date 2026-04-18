package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.dto.response.examquestionsnapshot.ExamQuestionSnapshotResponse;
import com.vn.backend.entities.ExamQuestion;
import com.vn.backend.entities.ExamQuestionAnswerSnapshot;
import com.vn.backend.entities.ExamQuestionSnapshot;
import com.vn.backend.entities.Question;
import com.vn.backend.enums.QuestionOrderMode;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AnswerRepository;
import com.vn.backend.repositories.ExamQuestionAnswerSnapshotRepository;
import com.vn.backend.repositories.ExamQuestionRepository;
import com.vn.backend.repositories.ExamQuestionSnapshotRepository;
import com.vn.backend.services.ExamQuestionSnapshotService;
import com.vn.backend.utils.MessageUtils;
import com.vn.backend.utils.SearchUtils;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamQuestionSnapshotServiceImpl extends BaseService implements
    ExamQuestionSnapshotService {

  private final ExamQuestionSnapshotRepository examQuestionSnapshotRepository;
  private final ExamQuestionAnswerSnapshotRepository examQuestionAnswerSnapshotRepository;
  private final ExamQuestionRepository examQuestionRepository;
  private final AnswerRepository answerRepository;

  public ExamQuestionSnapshotServiceImpl(MessageUtils messageUtils,
      ExamQuestionSnapshotRepository examQuestionSnapshotRepository,
      ExamQuestionAnswerSnapshotRepository examQuestionAnswerSnapshotRepository,
      ExamQuestionRepository examQuestionRepository, AnswerRepository answerRepository) {
    super(messageUtils);
    this.examQuestionSnapshotRepository = examQuestionSnapshotRepository;
    this.examQuestionAnswerSnapshotRepository = examQuestionAnswerSnapshotRepository;
    this.examQuestionRepository = examQuestionRepository;
    this.answerRepository = answerRepository;
  }

  @Override
  @Transactional
  public void createExamQuestionSnapshots(Long examId, Long sessionExamId) {
    if (examId == null) {
      return;
    }
    List<ExamQuestion> examQuestionList = examQuestionRepository.getAllExamQuestion(examId);
    if (examQuestionList.isEmpty()) {
      throw new AppException(MessageConst.EXAM_EMPTY,
              messageUtils.getMessage(MessageConst.EXAM_EMPTY), HttpStatus.BAD_REQUEST);
    }
    List<ExamQuestionSnapshot> examQuestionSnapshotList = examQuestionList.stream()
        .map(eq -> toSnapshot(eq, sessionExamId))
        .toList();
    examQuestionSnapshotRepository.saveAll(examQuestionSnapshotList);
  }

  /**
   * Dùng khi học sinh tải đề thi ở chế độ thi flexible
   * @param sessionExamId id của session exam
   * @return trả về toàn bộ câu hỏi + câu trả lời của exam. Câu trả lời không có thuộc tính isCorrect
   */
  @Override
  public List<ExamQuestionSnapshotResponse> getAllQuestions(Long sessionExamId, QuestionOrderMode orderMode) {
    if (sessionExamId == null) {
      return null;
    }
    List<ExamQuestionSnapshot> examQuestionSnapshotList = examQuestionSnapshotRepository.findAllBySessionExamId(
        sessionExamId, SearchUtils.getSortQuestion(orderMode));
    if (examQuestionSnapshotList.isEmpty()) {
      throw new AppException(MessageConst.NOT_FOUND,
          messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST);
    }
    if (QuestionOrderMode.RANDOM.equals(orderMode)) {
      Collections.shuffle(examQuestionSnapshotList);
    }
    return examQuestionSnapshotList.stream()
        .map(ExamQuestionSnapshotResponse::fromEntity)
        .toList();
  }

  private ExamQuestionSnapshot toSnapshot(ExamQuestion examQuestion, Long sessionExamId) {
    Question question = examQuestion.getQuestion();
    ExamQuestionSnapshot snapshot = ExamQuestionSnapshot.builder()
        .examId(examQuestion.getExamId())
        .sessionExamId(sessionExamId)
        .sourceQuestionId(question.getQuestionId())
        .questionContent(question.getContent())
        .questionImageUrl(question.getImageUrl())
        .questionType(question.getType())
        .difficultyLevel(question.getDifficultyLevel())
        .topicId(question.getTopicId())
        .score(examQuestion.getScore())
        .orderIndex(examQuestion.getOrderIndex())
        .build();

    if (question.getAnswers() != null && !question.getAnswers().isEmpty()) {
      List<ExamQuestionAnswerSnapshot> answerSnapshots = question.getAnswers().stream()
          .map(answer -> ExamQuestionAnswerSnapshot.builder()
              .sourceAnswerId(answer.getAnswerId())
              .answerContent(answer.getContent())
              .isCorrect(answer.getIsCorrect())
              .displayOrder(answer.getDisplayOrder())
              .examQuestionSnapshot(snapshot)
              .build()
          )
          .collect(Collectors.toList());

      snapshot.setExamQuestionAnswers(answerSnapshots);
    }

    return snapshot;
  }


}
