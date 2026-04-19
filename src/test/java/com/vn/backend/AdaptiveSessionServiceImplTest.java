package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.session.SubmitAnswerRequest;
import com.vn.backend.dto.response.session.NextQuestionResponse;
import com.vn.backend.dto.response.session.PracticeSetResponse;
import com.vn.backend.dto.response.session.SubmitAnswerResponse;
import com.vn.backend.entities.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.impl.AdaptiveSessionServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import com.vn.backend.enums.QuestionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdaptiveSessionServiceImpl Unit Tests")
public class AdaptiveSessionServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private UserTopicMasteryRepository userTopicMasteryRepository;
    @Mock
    private UserInteractionLogRepository userInteractionLogRepository;
    @Mock
    private AuthService authService;
    @Mock
    private MessageSource messageSource;

    private MessageUtils messageUtils;

    private AdaptiveSessionServiceImpl adaptiveSessionService;

    private User currentUser;
    private Topic sampleTopic;
    private Subject sampleSubject;
    private Question sampleQuestion;
    private Answer sampleAnswer1;
    private Answer sampleAnswer2;

    private SubmitAnswerRequest createReq() {
        SubmitAnswerRequest req = new SubmitAnswerRequest();
        req.setQuestionId(1000L);
        req.setSelectedAnswerId(10001L);
        return req;
    }

    @BeforeEach
    void setUp() {
        messageUtils = new MessageUtils(messageSource);
        adaptiveSessionService = new AdaptiveSessionServiceImpl(
                messageUtils, questionRepository, answerRepository, topicRepository, userTopicMasteryRepository, userInteractionLogRepository, authService
        );
        
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Mock Message");
        currentUser = User.builder().id(1L).username("testUser").build();
        
        sampleSubject = Subject.builder().subjectId(10L).subjectName("Math").build();
        
        sampleTopic = Topic.builder()
                .topicId(100L)
                .topicName("Algebra")
                .subjectId(10L)
                .subject(sampleSubject)
                .isActive(true)
                .isDeleted(false)
                .build();
                
        sampleQuestion = Question.builder()
                .questionId(1000L)
                .content("What is X?")
                .topicId(100L)
                .topic(sampleTopic)
                .difficultyLevel(2)
                .type(QuestionType.MULTI_CHOICE)
                .build();
                
        sampleAnswer1 = Answer.builder()
                .answerId(10001L)
                .content("Option A")
                .isCorrect(true)
                .isDeleted(false)
                .displayOrder(1)
                .build();
                
        sampleAnswer2 = Answer.builder()
                .answerId(10002L)
                .content("Option B")
                .isCorrect(false)
                .isDeleted(false)
                .displayOrder(2)
                .build();
    }

    @Nested
    @DisplayName("1. Tests for getNextQuestion(Long subjectId)")
    class GetNextQuestionTests {

        @Test
        @DisplayName("TC_ADP1_001: subjectId null -> throw AppException BAD_REQUEST")
        void getNextQuestion_NullSubjectId() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            
            assertThatThrownBy(() -> adaptiveSessionService.getNextQuestion(null))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("TC_ADP1_002: no available topics -> throw AppException NOT_FOUND")
        void getNextQuestion_NoAvailableTopics() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(topicRepository.findAvailableTopicsForAdaptiveLearning(eq(10L), eq(1L), anyInt(), anyDouble()))
                    .thenReturn(Collections.emptyList());
                    
            assertThatThrownBy(() -> adaptiveSessionService.getNextQuestion(10L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("TC_ADP1_003: no mastery -> difficulty 1 -> success")
        void getNextQuestion_NoMastery_Difficulty1() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(topicRepository.findAvailableTopicsForAdaptiveLearning(eq(10L), eq(1L), anyInt(), anyDouble()))
                    .thenReturn(List.of(sampleTopic));
            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(eq(1L), anyList()))
                    .thenReturn(Collections.emptyList());
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(1L, 100L))
                    .thenReturn(Optional.empty()); // No mastery -> diff 1
                    
            when(questionRepository.findByTopicIdAndDifficultyLevelAndIsReviewQuestionTrue(100L, 1))
                    .thenReturn(List.of(sampleQuestion));
            when(answerRepository.findByQuestionIdOrderByDisplayOrder(1000L))
                    .thenReturn(List.of(sampleAnswer1, sampleAnswer2));

            NextQuestionResponse response = adaptiveSessionService.getNextQuestion(10L);
            
            assertThat(response).isNotNull();
            assertThat(response.getQuestionId()).isEqualTo(1000L);
            assertThat(response.getDifficultyLevel()).isEqualTo(2); // From entity
            assertThat(response.getAnswers()).hasSize(2);
        }

        @Test
        @DisplayName("TC_ADP1_004: mastery < 0.4 -> difficulty 1")
        void getNextQuestion_LowMastery_Difficulty1() {
            UserTopicMastery mastery = UserTopicMastery.builder().topicId(100L).masteryScore(0.3).build();
            
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(topicRepository.findAvailableTopicsForAdaptiveLearning(anyLong(), anyLong(), anyInt(), anyDouble()))
                    .thenReturn(List.of(sampleTopic));
            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(anyLong(), anyList()))
                    .thenReturn(List.of(mastery));
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.of(mastery));
                    
            when(questionRepository.findByTopicIdAndDifficultyLevelAndIsReviewQuestionTrue(100L, 1))
                    .thenReturn(List.of(sampleQuestion));
            when(answerRepository.findByQuestionIdOrderByDisplayOrder(1000L))
                    .thenReturn(Collections.emptyList());

            adaptiveSessionService.getNextQuestion(10L);
            verify(questionRepository).findByTopicIdAndDifficultyLevelAndIsReviewQuestionTrue(100L, 1);
        }

        @Test
        @DisplayName("TC_ADP1_005: mastery = 0.5 -> difficulty 2")
        void getNextQuestion_MediumMastery_Difficulty2() {
            UserTopicMastery mastery = UserTopicMastery.builder().topicId(100L).masteryScore(0.5).build();
            
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(topicRepository.findAvailableTopicsForAdaptiveLearning(anyLong(), anyLong(), anyInt(), anyDouble()))
                    .thenReturn(List.of(sampleTopic));
            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(anyLong(), anyList()))
                    .thenReturn(List.of(mastery));
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.of(mastery));
                    
            when(questionRepository.findByTopicIdAndDifficultyLevelAndIsReviewQuestionTrue(100L, 2))
                    .thenReturn(List.of(sampleQuestion));
            when(answerRepository.findByQuestionIdOrderByDisplayOrder(1000L))
                    .thenReturn(Collections.emptyList());

            adaptiveSessionService.getNextQuestion(10L);
            verify(questionRepository).findByTopicIdAndDifficultyLevelAndIsReviewQuestionTrue(100L, 2);
        }

        @Test
        @DisplayName("TC_ADP1_006: mastery = 0.9 -> difficulty 3")
        void getNextQuestion_HighMastery_Difficulty3() {
            UserTopicMastery mastery = UserTopicMastery.builder().topicId(100L).masteryScore(0.9).nextReviewDate(LocalDate.now()).build();
            
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(topicRepository.findAvailableTopicsForAdaptiveLearning(anyLong(), anyLong(), anyInt(), anyDouble()))
                    .thenReturn(List.of(sampleTopic));
            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(anyLong(), anyList()))
                    .thenReturn(List.of(mastery));
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.of(mastery));
                    
            when(questionRepository.findByTopicIdAndDifficultyLevelAndIsReviewQuestionTrue(100L, 3))
                    .thenReturn(List.of(sampleQuestion));
            when(answerRepository.findByQuestionIdOrderByDisplayOrder(1000L))
                    .thenReturn(Collections.emptyList());

            adaptiveSessionService.getNextQuestion(10L);
            verify(questionRepository).findByTopicIdAndDifficultyLevelAndIsReviewQuestionTrue(100L, 3);
        }

        @Test
        @DisplayName("TC_ADP1_007: Fallback difficulty -> get questions without difficulty level")
        void getNextQuestion_FallbackDifficulty() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(topicRepository.findAvailableTopicsForAdaptiveLearning(anyLong(), anyLong(), anyInt(), anyDouble()))
                    .thenReturn(List.of(sampleTopic));
            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(anyLong(), anyList()))
                    .thenReturn(Collections.emptyList());
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.empty());
                    
            // Empty questions for specific difficulty
            when(questionRepository.findByTopicIdAndDifficultyLevelAndIsReviewQuestionTrue(100L, 1))
                    .thenReturn(Collections.emptyList());
            // Fallback
            when(questionRepository.findByTopicIdAndIsReviewQuestionTrue(100L))
                    .thenReturn(List.of(sampleQuestion));
                    
            when(answerRepository.findByQuestionIdOrderByDisplayOrder(1000L))
                    .thenReturn(Collections.emptyList());

            adaptiveSessionService.getNextQuestion(10L);
            verify(questionRepository).findByTopicIdAndIsReviewQuestionTrue(100L);
        }

        @Test
        @DisplayName("TC_ADP1_008: Fallback also empty -> throw NOT_FOUND")
        void getNextQuestion_FallbackAlsoEmpty_Throws() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(topicRepository.findAvailableTopicsForAdaptiveLearning(anyLong(), anyLong(), anyInt(), anyDouble()))
                    .thenReturn(List.of(sampleTopic));
            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(anyLong(), anyList()))
                    .thenReturn(Collections.emptyList());
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.empty());
                    
            when(questionRepository.findByTopicIdAndDifficultyLevelAndIsReviewQuestionTrue(anyLong(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(questionRepository.findByTopicIdAndIsReviewQuestionTrue(anyLong()))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> adaptiveSessionService.getNextQuestion(10L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
        }
        
        @Test
        @DisplayName("TC_ADP1_009: Topic without Subject relation should fallback to topic.getSubjectId()")
        void getNextQuestion_TopicWithoutSubjectEntity() {
            Topic isolatedTopic = Topic.builder().topicId(200L).subjectId(99L).subject(null).build();
            
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(topicRepository.findAvailableTopicsForAdaptiveLearning(anyLong(), anyLong(), anyInt(), anyDouble()))
                    .thenReturn(List.of(isolatedTopic));
            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(anyLong(), anyList()))
                    .thenReturn(Collections.emptyList());
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.empty());
            
            Question q = Question.builder().questionId(2000L).topic(isolatedTopic).build();
            when(questionRepository.findByTopicIdAndDifficultyLevelAndIsReviewQuestionTrue(anyLong(), anyInt()))
                    .thenReturn(List.of(q));
            when(answerRepository.findByQuestionIdOrderByDisplayOrder(anyLong()))
                    .thenReturn(Collections.emptyList());

            NextQuestionResponse response = adaptiveSessionService.getNextQuestion(10L);
            assertThat(response.getSubjectId()).isEqualTo(99L);
        }
        
        @Test
        @DisplayName("TC_ADP1_010: Skips deleted answers during mapping")
        void getNextQuestion_SkipsDeletedAnswers() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(topicRepository.findAvailableTopicsForAdaptiveLearning(anyLong(), anyLong(), anyInt(), anyDouble()))
                    .thenReturn(List.of(sampleTopic));
            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(anyLong(), anyList()))
                    .thenReturn(Collections.emptyList());
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.empty());
            when(questionRepository.findByTopicIdAndDifficultyLevelAndIsReviewQuestionTrue(anyLong(), anyInt()))
                    .thenReturn(List.of(sampleQuestion));
                    
            Answer deletedAnswer = Answer.builder().isDeleted(true).build();
            when(answerRepository.findByQuestionIdOrderByDisplayOrder(anyLong()))
                    .thenReturn(List.of(sampleAnswer1, deletedAnswer));

            NextQuestionResponse response = adaptiveSessionService.getNextQuestion(10L);
            assertThat(response.getAnswers()).hasSize(1);
            assertThat(response.getAnswers().get(0).getId()).isEqualTo(10001L);
        }
    }

    @Nested
    @DisplayName("2. Tests for submitAnswer(SubmitAnswerRequest)")
    class SubmitAnswerTests {

        @Test
        @DisplayName("TC_ADP2_001: Question not found -> throw NOT_FOUND")
        void submitAnswer_QuestionNotFound() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(questionRepository.findById(1000L)).thenReturn(Optional.empty());
            
            SubmitAnswerRequest req = new SubmitAnswerRequest();
            req.setQuestionId(1000L);
            
            assertThatThrownBy(() -> adaptiveSessionService.submitAnswer(req))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("TC_ADP2_002: Selected Answer not found -> throw NOT_FOUND")
        void submitAnswer_AnswerNotFound() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(questionRepository.findById(1000L)).thenReturn(Optional.of(sampleQuestion));
            when(answerRepository.findById(10002L)).thenReturn(Optional.empty());
            
            SubmitAnswerRequest req = new SubmitAnswerRequest();
            req.setQuestionId(1000L);
            req.setSelectedAnswerId(10002L);
            
            assertThatThrownBy(() -> adaptiveSessionService.submitAnswer(req))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("TC_ADP2_003: No Mastery - User answers correctly -> create new mastery")
        void submitAnswer_NoMastery_CorrectAnswer() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(questionRepository.findById(1000L)).thenReturn(Optional.of(sampleQuestion));
            when(answerRepository.findById(10001L)).thenReturn(Optional.of(sampleAnswer1));
            when(answerRepository.findCorrectAnswersByQuestionId(1000L)).thenReturn(List.of(sampleAnswer1));
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(1L, 100L))
                    .thenReturn(Optional.empty()); // No mastery
                    
            SubmitAnswerRequest req = new SubmitAnswerRequest();
            req.setQuestionId(1000L);
            req.setSelectedAnswerId(10001L);
            req.setResponseTimeSeconds(10L);
            
            SubmitAnswerResponse res = adaptiveSessionService.submitAnswer(req);
            
            assertThat(res.getWasCorrect()).isTrue();
            assertThat(res.getCorrectAnswerId()).isEqualTo(10001L);
            assertThat(res.getMessage()).isEqualTo("Correct!");
            
            ArgumentCaptor<UserTopicMastery> captor = ArgumentCaptor.forClass(UserTopicMastery.class);
            verify(userTopicMasteryRepository).save(captor.capture());
            
            UserTopicMastery saved = captor.getValue();
            assertThat(saved.getTotalAttempts()).isEqualTo(1);
            assertThat(saved.getCorrectAttempts()).isEqualTo(1);
            assertThat(saved.getMasteryScore()).isEqualTo(0.1); // 0*(1-0.1) + 1*0.1 = 0.1
            assertThat(saved.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(1)); // score < 0.8 -> today + 1
            
            verify(userInteractionLogRepository).save(any(UserInteractionLog.class));
        }

        @Test
        @DisplayName("TC_ADP2_004: Existing Mastery - User answers incorrectly")
        void submitAnswer_ExistingMastery_IncorrectAnswer() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(questionRepository.findById(1000L)).thenReturn(Optional.of(sampleQuestion));
            when(answerRepository.findById(10002L)).thenReturn(Optional.of(sampleAnswer2));
            when(answerRepository.findCorrectAnswersByQuestionId(1000L)).thenReturn(List.of(sampleAnswer1));
            
            UserTopicMastery mastery = UserTopicMastery.builder()
                    .userTopicMasteryId(10L).topicId(100L).totalAttempts(9).correctAttempts(5).masteryScore(0.5).build();
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(1L, 100L))
                    .thenReturn(Optional.of(mastery));
                    
            SubmitAnswerRequest req = new SubmitAnswerRequest();
            req.setQuestionId(1000L);
            req.setSelectedAnswerId(10002L); // Incorrect
            
            SubmitAnswerResponse res = adaptiveSessionService.submitAnswer(req);
            assertThat(res.getWasCorrect()).isFalse();
            
            ArgumentCaptor<UserTopicMastery> captor = ArgumentCaptor.forClass(UserTopicMastery.class);
            verify(userTopicMasteryRepository).save(captor.capture());
            UserTopicMastery saved = captor.getValue();
            assertThat(saved.getTotalAttempts()).isEqualTo(10);
            assertThat(saved.getCorrectAttempts()).isEqualTo(5);
            // newScore = 0.5 * 0.9 + (5/10) * 0.1 = 0.45 + 0.05 = 0.5
            // But wasCorrect=false -> nextReviewDate = today + 1
            assertThat(saved.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(1));
        }

        @Test
        @DisplayName("TC_ADP2_005: Correct answer & Mastered for the first time")
        void submitAnswer_CorrectAnswer_MasteredFirstTime() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(questionRepository.findById(1000L)).thenReturn(Optional.of(sampleQuestion));
            when(answerRepository.findById(10001L)).thenReturn(Optional.of(sampleAnswer1));
            when(answerRepository.findCorrectAnswersByQuestionId(1000L)).thenReturn(List.of(sampleAnswer1));
            
            // Set mastery score such that new score >= 0.8.
            // total=9, correct=9, score=0.8. new score = 0.8*0.9 + 1*0.1 = 0.72 + 0.1 = 0.82
            UserTopicMastery mastery = UserTopicMastery.builder()
                    .userTopicMasteryId(10L).topicId(100L).totalAttempts(9).correctAttempts(9).masteryScore(0.8)
                    .nextReviewDate(null) // first time mastering
                    .build();
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(1L, 100L))
                    .thenReturn(Optional.of(mastery));
                    
            SubmitAnswerRequest req = new SubmitAnswerRequest();
            req.setQuestionId(1000L);
            req.setSelectedAnswerId(10001L);
            
            adaptiveSessionService.submitAnswer(req);
            
            ArgumentCaptor<UserTopicMastery> captor = ArgumentCaptor.forClass(UserTopicMastery.class);
            verify(userTopicMasteryRepository).save(captor.capture());
            assertThat(captor.getValue().getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(3));
        }

        @Test
        @DisplayName("TC_ADP2_006: Spaced Repetition - daysFromNextReview <= 10 -> interval 7")
        void submitAnswer_SpacedRepetition_Interval7() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(questionRepository.findById(1000L)).thenReturn(Optional.of(sampleQuestion));
            when(answerRepository.findById(10001L)).thenReturn(Optional.of(sampleAnswer1));
            when(answerRepository.findCorrectAnswersByQuestionId(1000L)).thenReturn(List.of(sampleAnswer1));
            
            // Reviewing on time or slightly late (daysFromNextReview >= 0 && <= 10)
            LocalDate previousReviewDate = LocalDate.now().minusDays(2);
            UserTopicMastery mastery = UserTopicMastery.builder()
                    .userTopicMasteryId(10L).topicId(100L).totalAttempts(9).correctAttempts(9).masteryScore(0.85)
                    .nextReviewDate(previousReviewDate) 
                    .build();
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(1L, 100L))
                    .thenReturn(Optional.of(mastery));
                    
            adaptiveSessionService.submitAnswer(createReq());
            
            ArgumentCaptor<UserTopicMastery> captor = ArgumentCaptor.forClass(UserTopicMastery.class);
            verify(userTopicMasteryRepository).save(captor.capture());
            // days = 2 (<=10) -> nextInterval = 7
            assertThat(captor.getValue().getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(7));
        }

        @Test
        @DisplayName("TC_ADP2_007: Spaced Repetition - daysFromNextReview <= 20 -> interval 14")
        void submitAnswer_SpacedRepetition_Interval14() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(questionRepository.findById(1000L)).thenReturn(Optional.of(sampleQuestion));
            when(answerRepository.findById(10001L)).thenReturn(Optional.of(sampleAnswer1));
            when(answerRepository.findCorrectAnswersByQuestionId(1000L)).thenReturn(List.of(sampleAnswer1));
            
            LocalDate previousReviewDate = LocalDate.now().minusDays(15);
            UserTopicMastery mastery = UserTopicMastery.builder()
                    .masteryScore(0.85).nextReviewDate(previousReviewDate).totalAttempts(1).correctAttempts(1).build();
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.of(mastery));
                    
            adaptiveSessionService.submitAnswer(createReq());
            
            ArgumentCaptor<UserTopicMastery> captor = ArgumentCaptor.forClass(UserTopicMastery.class);
            verify(userTopicMasteryRepository).save(captor.capture());
            // days = 15 -> nextInterval = 14
            assertThat(captor.getValue().getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(14));
        }
        
        @Test
        @DisplayName("TC_ADP2_008: Spaced Repetition - daysFromNextReview <= 40 -> interval 30")
        void submitAnswer_SpacedRepetition_Interval30() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(questionRepository.findById(1000L)).thenReturn(Optional.of(sampleQuestion));
            when(answerRepository.findById(10001L)).thenReturn(Optional.of(sampleAnswer1));
            when(answerRepository.findCorrectAnswersByQuestionId(1000L)).thenReturn(List.of(sampleAnswer1));
            
            LocalDate previousReviewDate = LocalDate.now().minusDays(30);
            UserTopicMastery mastery = UserTopicMastery.builder()
                    .masteryScore(0.85).nextReviewDate(previousReviewDate).totalAttempts(1).correctAttempts(1).build();
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.of(mastery));
                    
            adaptiveSessionService.submitAnswer(createReq());
            
            ArgumentCaptor<UserTopicMastery> captor = ArgumentCaptor.forClass(UserTopicMastery.class);
            verify(userTopicMasteryRepository).save(captor.capture());
            // days = 30 -> nextInterval = 30
            assertThat(captor.getValue().getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(30));
        }

        @Test
        @DisplayName("TC_ADP2_009: Spaced Repetition - daysFromNextReview > 40 -> interval 60")
        void submitAnswer_SpacedRepetition_Interval60() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(questionRepository.findById(1000L)).thenReturn(Optional.of(sampleQuestion));
            when(answerRepository.findById(10001L)).thenReturn(Optional.of(sampleAnswer1));
            when(answerRepository.findCorrectAnswersByQuestionId(1000L)).thenReturn(List.of(sampleAnswer1));
            
            LocalDate previousReviewDate = LocalDate.now().minusDays(50);
            UserTopicMastery mastery = UserTopicMastery.builder()
                    .masteryScore(0.85).nextReviewDate(previousReviewDate).totalAttempts(1).correctAttempts(1).build();
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.of(mastery));
                    
            adaptiveSessionService.submitAnswer(createReq());
            
            ArgumentCaptor<UserTopicMastery> captor = ArgumentCaptor.forClass(UserTopicMastery.class);
            verify(userTopicMasteryRepository).save(captor.capture());
            // days = 50 -> nextInterval = 60
            assertThat(captor.getValue().getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(60));
        }

        @Test
        @DisplayName("TC_ADP2_010: Spaced Repetition - Review before due date (days < 0)")
        void submitAnswer_SpacedRepetition_ReviewEarly() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(questionRepository.findById(1000L)).thenReturn(Optional.of(sampleQuestion));
            when(answerRepository.findById(10001L)).thenReturn(Optional.of(sampleAnswer1));
            when(answerRepository.findCorrectAnswersByQuestionId(1000L)).thenReturn(List.of(sampleAnswer1));
            
            LocalDate futureDate = LocalDate.now().plusDays(5);
            UserTopicMastery mastery = UserTopicMastery.builder()
                    .masteryScore(0.85).nextReviewDate(futureDate).totalAttempts(1).correctAttempts(1).build();
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.of(mastery));
                    
            adaptiveSessionService.submitAnswer(createReq());
            
            ArgumentCaptor<UserTopicMastery> captor = ArgumentCaptor.forClass(UserTopicMastery.class);
            verify(userTopicMasteryRepository).save(captor.capture());
            // days < 0 -> nextReviewDate unchanged
            assertThat(captor.getValue().getNextReviewDate()).isEqualTo(futureDate);
        }

        @Test
        @DisplayName("TC_ADP2_011: When correctAnswers is empty, nulls are handled")
        void submitAnswer_EmptyCorrectAnswers() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(questionRepository.findById(1000L)).thenReturn(Optional.of(sampleQuestion));
            when(answerRepository.findById(10001L)).thenReturn(Optional.of(sampleAnswer1)); // selected
            when(answerRepository.findCorrectAnswersByQuestionId(1000L)).thenReturn(Collections.emptyList());
            
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.empty());
                    
            SubmitAnswerResponse response = adaptiveSessionService.submitAnswer(createReq());
            
            assertThat(response.getCorrectAnswerId()).isNull();
            assertThat(response.getCorrectAnswer()).isNull();
        }
        
        @Test
        @DisplayName("TC_ADP2_012: Ensure isolated topic/subject mappings are correct without NPE")
        void submitAnswer_NullTopic() {
            // Setup question without topic
            Question isolatedQuestion = Question.builder().questionId(1000L).topic(null).topicId(100L).build();
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(questionRepository.findById(1000L)).thenReturn(Optional.of(isolatedQuestion));
            when(answerRepository.findById(10001L)).thenReturn(Optional.of(sampleAnswer1));
            when(answerRepository.findCorrectAnswersByQuestionId(1000L)).thenReturn(Collections.emptyList());
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(anyLong(), anyLong()))
                    .thenReturn(Optional.empty());
                    
            SubmitAnswerResponse response = adaptiveSessionService.submitAnswer(createReq());
            assertThat(response.getTopicName()).isNull();
            assertThat(response.getTopicId()).isNull();
        }
    }

    @Nested
    @DisplayName("3. Tests for getPracticeSet(Long topicId)")
    class GetPracticeSetTests {

        @Test
        @DisplayName("TC_ADP3_001: Topic not found or deleted -> throw NOT_FOUND")
        void getPracticeSet_TopicNotFound() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(topicRepository.findById(100L)).thenReturn(Optional.empty());
            
            assertThatThrownBy(() -> adaptiveSessionService.getPracticeSet(100L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("TC_ADP3_002: Inactive or Deleted Topic -> throw NOT_FOUND")
        void getPracticeSet_TopicInactiveOrDeleted() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            Topic inactive = Topic.builder().isActive(false).isDeleted(false).build();
            when(topicRepository.findById(100L)).thenReturn(Optional.of(inactive));
            
            assertThatThrownBy(() -> adaptiveSessionService.getPracticeSet(100L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("TC_ADP3_003: No Mastery -> throw FORBIDDEN")
        void getPracticeSet_NoMastery() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(topicRepository.findById(100L)).thenReturn(Optional.of(sampleTopic));
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(1L, 100L))
                    .thenReturn(Optional.empty());
                    
            assertThatThrownBy(() -> adaptiveSessionService.getPracticeSet(100L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("TC_ADP3_004: Mastery < 0.8 -> throw FORBIDDEN")
        void getPracticeSet_LowMastery() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(topicRepository.findById(100L)).thenReturn(Optional.of(sampleTopic));
            UserTopicMastery mastery = UserTopicMastery.builder().masteryScore(0.79).build();
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(1L, 100L))
                    .thenReturn(Optional.of(mastery));
                    
            assertThatThrownBy(() -> adaptiveSessionService.getPracticeSet(100L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("TC_ADP3_005: No questions -> throw NOT_FOUND")
        void getPracticeSet_NoQuestions() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(topicRepository.findById(100L)).thenReturn(Optional.of(sampleTopic));
            UserTopicMastery mastery = UserTopicMastery.builder().masteryScore(0.85).build();
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(1L, 100L))
                    .thenReturn(Optional.of(mastery));
            when(questionRepository.findByTopicIdAndIsReviewQuestionTrue(100L))
                    .thenReturn(Collections.emptyList());
                    
            assertThatThrownBy(() -> adaptiveSessionService.getPracticeSet(100L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("TC_ADP3_006: Happy Path -> Returns valid practice set with isCorrect mapped")
        void getPracticeSet_HappyPath() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(topicRepository.findById(100L)).thenReturn(Optional.of(sampleTopic));
            UserTopicMastery mastery = UserTopicMastery.builder().masteryScore(0.9).build();
            when(userTopicMasteryRepository.findByUserIdAndTopicIdAndIsDeletedFalse(1L, 100L))
                    .thenReturn(Optional.of(mastery));
            when(questionRepository.findByTopicIdAndIsReviewQuestionTrue(100L))
                    .thenReturn(Arrays.asList(sampleQuestion));
            when(answerRepository.findByQuestionIdOrderByDisplayOrder(1000L))
                    .thenReturn(Arrays.asList(sampleAnswer1, sampleAnswer2));
                    
            PracticeSetResponse response = adaptiveSessionService.getPracticeSet(100L);
            
            assertThat(response).isNotNull();
            assertThat(response.getTopicId()).isEqualTo(100L);
            assertThat(response.getTotalQuestions()).isEqualTo(1);
            assertThat(response.getQuestions().get(0).getAnswers().get(0).getIsCorrect())
                    .isEqualTo(sampleAnswer1.getIsCorrect());
            assertThat(response.getQuestions().get(0).getCorrectAnswerId()).isEqualTo(10001L);
        }
    }
}
