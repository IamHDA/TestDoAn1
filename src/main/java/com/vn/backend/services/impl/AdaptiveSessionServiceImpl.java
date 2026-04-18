package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.session.SubmitAnswerRequest;
import com.vn.backend.dto.response.answer.AnswerResponse;
import com.vn.backend.dto.response.session.NextQuestionResponse;
import com.vn.backend.dto.response.session.PracticeQuestionResponse;
import com.vn.backend.dto.response.session.PracticeSetResponse;
import com.vn.backend.dto.response.session.SubmitAnswerResponse;
import com.vn.backend.entities.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.AdaptiveSessionService;
import com.vn.backend.services.AuthService;
import com.vn.backend.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdaptiveSessionServiceImpl extends BaseService implements AdaptiveSessionService {

    private static final double MASTERY_THRESHOLD = 0.8;
    private static final double LEARNING_RATE = 0.1; // Learning rate cho việc cập nhật mastery
    private static final int MIN_QUESTIONS_PER_TOPIC = 15; // Tối thiểu số câu hỏi để topic được tham gia hệ thống Học Thích ứng

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final TopicRepository topicRepository;
    private final UserTopicMasteryRepository userTopicMasteryRepository;
    private final UserInteractionLogRepository userInteractionLogRepository;
    private final AuthService authService;

    public AdaptiveSessionServiceImpl(
            MessageUtils messageUtils,
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            TopicRepository topicRepository,
            UserTopicMasteryRepository userTopicMasteryRepository,
            UserInteractionLogRepository userInteractionLogRepository,
            AuthService authService) {
        super(messageUtils);
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.topicRepository = topicRepository;
        this.userTopicMasteryRepository = userTopicMasteryRepository;
        this.userInteractionLogRepository = userInteractionLogRepository;
        this.authService = authService;
    }


    @Override
    public NextQuestionResponse getNextQuestion(Long subjectId) {
        log.info("Getting next question for subjectId: {}", subjectId);
        User currentUser = authService.getCurrentUser();

        // Validate subjectId
        if (subjectId == null) {
            throw new AppException(AppConst.MessageConst.REQUIRED_FIELD_EMPTY,
                    messageUtils.getMessage(AppConst.MessageConst.REQUIRED_FIELD_EMPTY), HttpStatus.BAD_REQUEST);
        }

        // Lấy tất cả topics active thuộc subject này mà user có thể học
        List<Topic> availableTopics = topicRepository.findAvailableTopicsForAdaptiveLearning(
                subjectId,
                currentUser.getId(),
                MIN_QUESTIONS_PER_TOPIC,
                MASTERY_THRESHOLD
        );
        
        if (availableTopics.isEmpty()) {
            throw new AppException(AppConst.MessageConst.NOT_FOUND,
                    "No available topics for learning in this subject", HttpStatus.NOT_FOUND);
        }
        
        // Ưu tiên topics có mastery thấp hoặc chưa có mastery, hoặc cần review)
        Topic selectedTopic = selectTopicForLearning(currentUser.getId(), availableTopics);
        
        UserTopicMastery mastery = userTopicMasteryRepository
                .findByUserIdAndTopicIdAndIsDeletedFalse(currentUser.getId(), selectedTopic.getTopicId())
                .orElse(null);

        Integer targetDifficulty = determineDifficulty(mastery);

        List<Question> questions = questionRepository.findByTopicIdAndDifficultyLevelAndIsReviewQuestionTrue(
                selectedTopic.getTopicId(), targetDifficulty);

        if (questions.isEmpty()) {
            // Fallback
            questions = questionRepository.findByTopicIdAndIsReviewQuestionTrue(selectedTopic.getTopicId());
        }

        if (questions.isEmpty()) {
            throw new AppException(AppConst.MessageConst.NOT_FOUND,
                    "No questions available for this topic", HttpStatus.NOT_FOUND);
        }

        Question selectedQuestion = questions.get(new Random().nextInt(questions.size()));

        List<Answer> answers = answerRepository.findByQuestionIdOrderByDisplayOrder(selectedQuestion.getQuestionId());
        List<AnswerResponse> answerResponses = answers.stream()
                .filter(a -> !a.getIsDeleted())
                .map(a -> {
                    AnswerResponse ar = new AnswerResponse();
                    ar.setId(a.getAnswerId());
                    ar.setContent(a.getContent());
                    ar.setDisplayOrder(a.getDisplayOrder());
                    return ar;
                })
                .collect(Collectors.toList());
        
        Subject subject = selectedTopic.getSubject();
        Long responseSubjectId = subject != null ? subject.getSubjectId() : selectedTopic.getSubjectId();
        String subjectName = subject != null ? subject.getSubjectName() : null;

        return NextQuestionResponse.builder()
                .questionId(selectedQuestion.getQuestionId())
                .questionText(selectedQuestion.getContent())
                .imageUrl(selectedQuestion.getImageUrl())
                .difficultyLevel(selectedQuestion.getDifficultyLevel())
                .topicId(selectedTopic.getTopicId())
                .topicName(selectedTopic.getTopicName())
                .subjectId(responseSubjectId)
                .subjectName(subjectName)
                .answers(answerResponses)
                .build();
    }

    @Override
    @Transactional
    public SubmitAnswerResponse submitAnswer(SubmitAnswerRequest request) {
        log.info("Submitting answer for questionId: {}", request.getQuestionId());
        User currentUser = authService.getCurrentUser();

        // Get question
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));

        // Get selected answer
        Answer selectedAnswer = answerRepository.findById(request.getSelectedAnswerId())
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));

        // Get correct answer
        List<Answer> correctAnswers = answerRepository.findCorrectAnswersByQuestionId(question.getQuestionId());

        Answer correctAnswerEntity = correctAnswers.isEmpty() ? null : correctAnswers.get(0);
        Long correctAnswerId = correctAnswerEntity != null ? correctAnswerEntity.getAnswerId() : null;
        Boolean wasCorrect = selectedAnswer.getIsCorrect() != null && selectedAnswer.getIsCorrect();

        // Log interaction
        UserInteractionLog log = UserInteractionLog.builder()
                .userId(currentUser.getId())
                .questionId(question.getQuestionId())
                .selectedAnswerId(request.getSelectedAnswerId())
                .wasCorrect(wasCorrect)
                .responseTimeSeconds(request.getResponseTimeSeconds())
                .interactionTimestamp(LocalDateTime.now())
                .isDeleted(false)
                .build();
        userInteractionLogRepository.save(log);

        // Update mastery score using simplified algorithm
        Double newMasteryScore = updateMasteryScore(currentUser.getId(), question.getTopicId(), wasCorrect);

        // Map correct answer to AnswerResponse
        AnswerResponse correctAnswerResponse = null;
        if (correctAnswerEntity != null) {
            correctAnswerResponse = AnswerResponse.fromEntity(correctAnswerEntity);
        }

        // Get topic information
        Topic topic = question.getTopic();
        Long topicId = topic != null ? topic.getTopicId() : null;
        String topicName = topic != null ? topic.getTopicName() : null;

        return SubmitAnswerResponse.builder()
                .wasCorrect(wasCorrect)
                .correctAnswerId(correctAnswerId)
                .correctAnswer(correctAnswerResponse)
                .newMasteryScore(newMasteryScore)
                .message(wasCorrect ? "Correct!" : "Incorrect. Keep practicing!")
                .questionId(question.getQuestionId())
                .topicId(topicId)
                .topicName(topicName)
                .build();
    }
    

    private Topic selectTopicForLearning(Long userId, List<Topic> availableTopics) {
        LocalDate today = LocalDate.now();
        
        // Get masteries
        List<Long> topicIds = availableTopics.stream().map(Topic::getTopicId).collect(Collectors.toList());
        List<UserTopicMastery> masteries = userTopicMasteryRepository
                .findByUserIdAndTopicIdIn(userId, topicIds);
        Map<Long, UserTopicMastery> masteryMap = masteries.stream()
                .collect(Collectors.toMap(UserTopicMastery::getTopicId, m -> m));

        // Phân loại topics
        List<Topic> reviewTopics = new ArrayList<>(); // Topics Mastered cần review (ưu tiên #1)
        List<Topic> learningTopics = new ArrayList<>(); // Topics Learning (ưu tiên #2)
        
        for (Topic topic : availableTopics) {
            UserTopicMastery mastery = masteryMap.get(topic.getTopicId());
            if (mastery == null) {
                learningTopics.add(topic); // Chưa có mastery = Learning
            } else if (mastery.getMasteryScore() >= MASTERY_THRESHOLD) {
                // Mastered - kiểm tra có đến hạn review không
                if (mastery.getNextReviewDate() != null && 
                    !mastery.getNextReviewDate().isAfter(today)) {
                    reviewTopics.add(topic); // Đến hạn review
                }
            } else {
                learningTopics.add(topic); // Learning (mastery < 0.8)
            }
        }
        
        // Ưu tiên #1: Topics Mastered cần review (ưu tiên review date sớm nhất)
        if (!reviewTopics.isEmpty()) {
            return reviewTopics.stream()
                    .min(Comparator.comparing(topic -> {
                        UserTopicMastery m = masteryMap.get(topic.getTopicId());
                        return m != null && m.getNextReviewDate() != null 
                                ? m.getNextReviewDate() 
                                : LocalDate.MAX;
                    }))
                    .orElse(reviewTopics.get(0));
        }
        
        // Ưu tiên #2: Topics Learning (ưu tiên mastery thấp nhất)
        if (!learningTopics.isEmpty()) {
            return learningTopics.stream()
                    .min(Comparator.comparingDouble(topic -> {
                        UserTopicMastery mastery = masteryMap.get(topic.getTopicId());
                        return mastery != null ? mastery.getMasteryScore() : 0.0;
                    }))
                    .orElse(learningTopics.get(0));
        }
        
        // Fallback
        return availableTopics.get(0);
    }

    private Integer determineDifficulty(UserTopicMastery mastery) {
        if (mastery == null) {
            return 1; // Start with easy
        }
        
        double score = mastery.getMasteryScore();
        if (score < 0.4) {
            return 1; // Easy
        } else if (score < 0.7) {
            return 2; // Medium
        } else {
            return 3; // Hard
        }
    }

    private Double updateMasteryScore(Long userId, Long topicId, Boolean wasCorrect) {
        UserTopicMastery mastery = userTopicMasteryRepository
                .findByUserIdAndTopicIdAndIsDeletedFalse(userId, topicId)
                .orElse(null);

        if (mastery == null) {
            // Create new mastery
            mastery = UserTopicMastery.builder()
                    .userId(userId)
                    .topicId(topicId)
                    .masteryScore(0.0)
                    .totalAttempts(0)
                    .correctAttempts(0)
                    .isDeleted(false)
                    .build();
        }

        // Update counts
        mastery.setTotalAttempts(mastery.getTotalAttempts() + 1);
        if (wasCorrect) {
            mastery.setCorrectAttempts(mastery.getCorrectAttempts() + 1);
        }

        double correctRate = (double) mastery.getCorrectAttempts() / mastery.getTotalAttempts();
        
        double newScore = mastery.getMasteryScore() * (1 - LEARNING_RATE) + correctRate * LEARNING_RATE;
        mastery.setMasteryScore(Math.min(1.0, Math.max(0.0, newScore)));
        
        // Tính toán NextReviewDate (Spaced Repetition Algorithm)
        LocalDate nextReviewDate = calculateNextReviewDate(mastery.getMasteryScore(), wasCorrect, mastery.getNextReviewDate());
        mastery.setNextReviewDate(nextReviewDate);

        userTopicMasteryRepository.save(mastery);
        return mastery.getMasteryScore();
    }
    
    /**
     * Tính toán NextReviewDate dựa trên spaced repetition algorithm
     * - Nếu sai: reset về 1 ngày sau
     * - Nếu đúng nhưng chưa mastered: 1 ngày sau
     * - Nếu đúng và đã mastered: tăng khoảng thời gian theo exponential backoff
     */
    private LocalDate calculateNextReviewDate(Double masteryScore, Boolean wasCorrect, LocalDate currentNextReviewDate) {
        LocalDate today = LocalDate.now();
        
        if (!wasCorrect) {
            // Trả lời sai -> reset về 1 ngày sau
            return today.plusDays(1);
        }
        
        if (masteryScore < MASTERY_THRESHOLD) {
            // Chưa mastered -> 1 ngày sau
            return today.plusDays(1);
        }
        
        // Đã mastered và trả lời đúng -> tăng khoảng thời gian (exponential backoff)
        if (currentNextReviewDate == null) {
            // Lần đầu đạt mastered -> 3 ngày sau
            return today.plusDays(3);
        }
        
        // Kiểm tra xem có đang review đúng hạn hoặc quá hạn không
        // daysFromNextReview = số ngày từ nextReviewDate đến today
        // Nếu >= 0: đang review đúng hạn hoặc quá hạn
        long daysFromNextReview = java.time.temporal.ChronoUnit.DAYS.between(currentNextReviewDate, today);
        
        if (daysFromNextReview >= 0) {
            // Tính số ngày giữa các interval có thể: 3, 7, 14, 30, 60, 90
            // Ước tính interval hiện tại dựa vào số ngày
            long nextInterval = getNextInterval(daysFromNextReview);

            return today.plusDays(nextInterval);
        } else {
            // Đang review sớm (chưa đến hạn) -> giữ nguyên nextReviewDate
            return currentNextReviewDate;
        }
    }

    private static long getNextInterval(long daysFromNextReview) {
        long estimatedInterval = 3;
        if (daysFromNextReview <= 10) {
            estimatedInterval = 3; // Có thể là 3 hoặc 7
        } else if (daysFromNextReview <= 20) {
            estimatedInterval = 7; // Có thể là 7 hoặc 14
        } else if (daysFromNextReview <= 40) {
            estimatedInterval = 14; // Có thể là 14 hoặc 30
        } else {
            estimatedInterval = 30; // Có thể là 30 hoặc 60
        }

        // Tính interval tiếp theo dựa vào estimatedInterval
        long nextInterval;
        if (estimatedInterval <= 3) {
            nextInterval = 7; // 3 -> 7
        } else if (estimatedInterval <= 7) {
            nextInterval = 14; // 7 -> 14
        } else if (estimatedInterval <= 14) {
            nextInterval = 30; // 14 -> 30
        } else {
            nextInterval = 60; // 30 -> 60
        }
        return nextInterval;
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeSetResponse getPracticeSet(Long topicId) {
        log.info("Getting practice set for topicId: {}", topicId);
        User currentUser = authService.getCurrentUser();

        // Kiểm tra topic tồn tại, active và chưa bị xóa
        Topic topic = topicRepository.findById(topicId)
                .filter(t -> !t.getIsDeleted() && t.getIsActive() != null && t.getIsActive())
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        "Topic not found or not accessible", HttpStatus.NOT_FOUND));

        // Kiểm tra user đã mastered topic này
        UserTopicMastery mastery = userTopicMasteryRepository
                .findByUserIdAndTopicIdAndIsDeletedFalse(currentUser.getId(), topicId)
                .orElse(null);

        if (mastery == null || mastery.getMasteryScore() < MASTERY_THRESHOLD) {
            throw new AppException(AppConst.MessageConst.FORBIDDEN,
                    "Topic must be mastered before practice", HttpStatus.FORBIDDEN);
        }
        
        List<Question> questions = questionRepository.findByTopicIdAndIsReviewQuestionTrue(topicId);

        if (questions.isEmpty()) {
            throw new AppException(AppConst.MessageConst.NOT_FOUND,
                    "No questions available for this topic", HttpStatus.NOT_FOUND);
        }

        // Shuffle questions
        Collections.shuffle(questions, new Random());

        // Map questions to PracticeQuestionResponse (BAO GỒM isCorrect)
        List<PracticeQuestionResponse> practiceQuestions = questions.stream()
                .map(q -> {
                    List<Answer> answers = answerRepository.findByQuestionIdOrderByDisplayOrder(q.getQuestionId());
                    List<AnswerResponse> answerResponses = answers.stream()
                            .filter(a -> !a.getIsDeleted())
                            .map(a -> {
                                AnswerResponse ar = new AnswerResponse();
                                ar.setId(a.getAnswerId());
                                ar.setContent(a.getContent());
                                ar.setIsCorrect(a.getIsCorrect()); // BAO GỒM isCorrect cho Practice Mode
                                ar.setDisplayOrder(a.getDisplayOrder());
                                return ar;
                            })
                            .collect(Collectors.toList());

                    // Tìm correctAnswerId
                    Long correctAnswerId = answers.stream()
                            .filter(a -> !a.getIsDeleted() && 
                                        a.getIsCorrect() != null && 
                                        a.getIsCorrect())
                            .map(Answer::getAnswerId)
                            .findFirst()
                            .orElse(null);

                    return PracticeQuestionResponse.builder()
                            .questionId(q.getQuestionId())
                            .questionText(q.getContent())
                            .imageUrl(q.getImageUrl())
                            .questionType(q.getType().name())
                            .difficultyLevel(q.getDifficultyLevel())
                            .answers(answerResponses)
                            .correctAnswerId(correctAnswerId)
                            .build();
                })
                .collect(Collectors.toList());
        return PracticeSetResponse.builder()
                .topicId(topic.getTopicId())
                .subjectName(topic.getSubject().getSubjectName())
                .topicName(topic.getTopicName())
                .totalQuestions(practiceQuestions.size())
                .questions(practiceQuestions)
                .build();
    }
}

