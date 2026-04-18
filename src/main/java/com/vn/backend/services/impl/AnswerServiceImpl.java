package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.answer.AnswerCreateRequest;
import com.vn.backend.dto.request.answer.AnswerUpdateRequest;
import com.vn.backend.dto.response.answer.AnswerResponse;
import com.vn.backend.entities.Answer;
import com.vn.backend.entities.Question;
import com.vn.backend.entities.User;
import com.vn.backend.enums.QuestionType;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AnswerRepository;
import com.vn.backend.repositories.QuestionRepository;
import com.vn.backend.services.AnswerService;
import com.vn.backend.services.AuthService;
import com.vn.backend.utils.MessageUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AnswerServiceImpl extends BaseService implements AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final AuthService authService;

    public AnswerServiceImpl(MessageUtils messageUtils, AnswerRepository answerRepository, QuestionRepository questionRepository, AuthService authService) {
        super(messageUtils);
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.authService = authService;
    }

    @Override
    public AnswerResponse addAnswer(AnswerCreateRequest request) {
        User currentUser = authService.getCurrentUser();
        Optional<Question> question = questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(request.getQuestionId(),currentUser.getId());
        if(question.isEmpty()) {
            throw new AppException(AppConst.MessageConst.NOT_FOUND,
                    messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST);

        }
        // Validate Single Choice: không được phép có > 1 đáp án đúng
        Question q = question.get();
        if (q.getType() != null && q.getType() == QuestionType.SINGLE_CHOICE && Boolean.TRUE.equals(request.getIsCorrect())) {
            long existingCorrect = answerRepository.countByQuestionIdAndIsCorrectTrue(request.getQuestionId());
            if (existingCorrect > 1) {
                throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                        messageUtils.getMessage(AppConst.MessageConst.INVALID_LOGIC_QUESTION), HttpStatus.BAD_REQUEST);
            }
        }
        Answer answer = Answer.builder()
                .content(request.getContent())
                .questionId(request.getQuestionId())
                .isCorrect(request.getIsCorrect() != null && request.getIsCorrect())
                .displayOrder(request.getDisplayOrder())
                .isDeleted(false)
                .build();
        Answer saved = answerRepository.saveAndFlush(answer);
        return mapToResponse(saved);
    }

    @Override
    public AnswerResponse updateAnswer(Long answerId, AnswerUpdateRequest request) {
        Optional<Answer> opt = answerRepository.findByAnswerIdAndIsDeletedFalse(answerId);
        if (opt.isEmpty()) {
            throw new AppException(AppConst.MessageConst.NOT_FOUND,
                    messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST);

        }
        Answer answer = opt.get();
        // Validate Single Choice when marking this answer as correct
        if (request.getIsCorrect() != null && request.getIsCorrect()) {
            Optional<Question> questionOpt = questionRepository.findById(answer.getQuestionId());
            if (questionOpt.isPresent()) {
                Question q = questionOpt.get();
                if (q.getType() != null && q.getType() == QuestionType.SINGLE_CHOICE) {
                    Optional<Answer> answerCorrect = answerRepository.findByAnswerIdAndIsCorrectTrueAndIsDeletedFalse(answerId);
                    if (answerCorrect.isEmpty()) {
                        throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                                messageUtils.getMessage(AppConst.MessageConst.INVALID_LOGIC_QUESTION), HttpStatus.BAD_REQUEST);
                    }
                }
            }
        }
        // Disallow removing the last correct answer
        if (request.getIsCorrect() != null && !request.getIsCorrect()) {
            if (Boolean.TRUE.equals(answer.getIsCorrect())) {
                long otherCorrect = answerRepository.countByQuestionIdAndIsCorrectTrueAndAnswerIdNotAndIsDeletedFalse(answer.getQuestionId(), answerId);
                if (otherCorrect == 0) {
                    throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                            messageUtils.getMessage(AppConst.MessageConst.INVALID_LOGIC_QUESTION), HttpStatus.BAD_REQUEST);
                }
            }
        }
        if (request.getContent() != null) answer.setContent(request.getContent());
        if (request.getIsCorrect() != null) answer.setIsCorrect(request.getIsCorrect());
        if (request.getDisplayOrder() != null) answer.setDisplayOrder(request.getDisplayOrder());
        Answer updated = answerRepository.saveAndFlush(answer);
        return mapToResponse(updated);
    }

    @Override
    public void softDeleteAnswer(Long answerId) {
        Optional<Answer> opt = answerRepository.findById(answerId);
        opt.ifPresent(ans -> {
            ans.setIsDeleted(true);
            answerRepository.saveAndFlush(ans);
        });
    }

    @Override
    public void reorderAnswers(Long questionId, List<Long> answerOrderIds) {
        List<Answer> answers = answerRepository.findByQuestionIdOrderByDisplayOrder(questionId);
        for (int i = 0; i < answerOrderIds.size(); i++) {
            Long curId = answerOrderIds.get(i);
            for (Answer a : answers) {
                if (a.getAnswerId().equals(curId) && !Boolean.TRUE.equals(a.getIsDeleted())) {
                    a.setDisplayOrder(i);
                    answerRepository.save(a);
                    break;
                }
            }
        }
    }

    private AnswerResponse mapToResponse(Answer answer) {
        AnswerResponse ar = new AnswerResponse();
        ar.setId(answer.getAnswerId());
        ar.setContent(answer.getContent());
        ar.setIsCorrect(answer.getIsCorrect());
        ar.setDisplayOrder(answer.getDisplayOrder());
        return ar;
    }
}
