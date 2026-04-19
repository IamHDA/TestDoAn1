package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.response.learningpath.LearningPathResponse;
import com.vn.backend.dto.response.progress.MasteryProgressResponse;
import com.vn.backend.entities.Subject;
import com.vn.backend.entities.Topic;
import com.vn.backend.entities.User;
import com.vn.backend.entities.UserTopicMastery;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.QuestionRepository;
import com.vn.backend.repositories.SubjectRepository;
import com.vn.backend.repositories.TopicRepository;
import com.vn.backend.repositories.UserTopicMasteryRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.impl.LearningPathServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LearningPathServiceImpl Unit Tests")
public class LearningPathServiceImplTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private UserTopicMasteryRepository userTopicMasteryRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AuthService authService;
    @Mock
    private MessageSource messageSource;

    private MessageUtils messageUtils;

    private LearningPathServiceImpl learningPathService;

    private User currentUser;
    private Subject subject;

    @BeforeEach
    void setUp() {
        messageUtils = new MessageUtils(messageSource);
        learningPathService = new LearningPathServiceImpl(
                messageUtils, topicRepository, subjectRepository, userTopicMasteryRepository, questionRepository, authService
        );
        
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Mock Message");
        currentUser = User.builder().id(1L).username("student1").build();
        subject = Subject.builder().subjectId(100L).subjectName("Mathematics").build();
    }

    @Nested
    @DisplayName("1. Tests for getLearningPath(Long subjectId)")
    class GetLearningPathTests {

        @Test
        @DisplayName("TC_LP1_001: subjectId not found -> throw NOT_FOUND AppException")
        void getLearningPath_SubjectNotFound() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(subjectRepository.findById(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> learningPathService.getLearningPath(100L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("TC_LP1_002: Lọc Topics - Chỉ giữ lại topic có ID trong set tối thiểu (>=15 câu)")
        void getLearningPath_FilterMinimumQuestions() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(subjectRepository.findById(100L)).thenReturn(Optional.of(subject));

            Topic t1 = Topic.builder().topicId(1L).build();
            Topic t2 = Topic.builder().topicId(2L).build(); // will be filtered out due to insufficient questions
            Topic t3 = Topic.builder().topicId(3L).build();

            when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(100L))
                    .thenReturn(List.of(t1, t2, t3));

            // Only T1 and T3 have enough questions
            when(questionRepository.findTopicIdsWithMinimumQuestions(15))
                    .thenReturn(List.of(1L, 3L));

            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(eq(1L), eq(List.of(1L, 3L))))
                    .thenReturn(Collections.emptyList());

            LearningPathResponse response = learningPathService.getLearningPath(100L);

            assertThat(response.getTopics()).hasSize(2)
                    .extracting(LearningPathResponse.TopicNode::getTopicId)
                    .containsExactlyInAnyOrder(1L, 3L);
        }

        @Test
        @DisplayName("TC_LP1_003: No prerequisites + score >= 0.8 -> MASTERED")
        void getLearningPath_NoPrereq_ScoreHigh() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(subjectRepository.findById(100L)).thenReturn(Optional.of(subject));

            Topic t1 = Topic.builder().topicId(1L).prerequisiteTopicId(null).build();
            when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(100L)).thenReturn(List.of(t1));
            when(questionRepository.findTopicIdsWithMinimumQuestions(anyInt())).thenReturn(List.of(1L));

            UserTopicMastery mastery = new UserTopicMastery();
            mastery.setTopicId(1L);
            mastery.setMasteryScore(0.85);

            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(eq(1L), eq(List.of(1L))))
                    .thenReturn(List.of(mastery));

            LearningPathResponse response = learningPathService.getLearningPath(100L);

            LearningPathResponse.UserMasteryStatus status = response.getUserMasteryStatuses().get(0);
            assertThat(status.getTopicId()).isEqualTo(1L);
            assertThat(status.getState()).isEqualTo(LearningPathResponse.UserMasteryStatus.MasteryState.MASTERED);
        }

        @Test
        @DisplayName("TC_LP1_004: No prerequisites + score < 0.8 -> LEARNING")
        void getLearningPath_NoPrereq_ScoreLow() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(subjectRepository.findById(100L)).thenReturn(Optional.of(subject));

            Topic t1 = Topic.builder().topicId(1L).prerequisiteTopicId(null).build();
            when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(100L)).thenReturn(List.of(t1));
            when(questionRepository.findTopicIdsWithMinimumQuestions(anyInt())).thenReturn(List.of(1L));

            // Setup score 0.79
            UserTopicMastery mastery = new UserTopicMastery();
            mastery.setTopicId(1L);
            mastery.setMasteryScore(0.79);

            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(eq(1L), eq(List.of(1L))))
                    .thenReturn(List.of(mastery));

            LearningPathResponse response = learningPathService.getLearningPath(100L);

            LearningPathResponse.UserMasteryStatus status = response.getUserMasteryStatuses().get(0);
            assertThat(status.getTopicId()).isEqualTo(1L);
            assertThat(status.getState()).isEqualTo(LearningPathResponse.UserMasteryStatus.MasteryState.LEARNING);
        }

        @Test
        @DisplayName("TC_LP1_005: Prerequisite exists + Prereq Mastered + self Mastered -> MASTERED")
        void getLearningPath_PrereqMastered_SelfMastered() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(subjectRepository.findById(100L)).thenReturn(Optional.of(subject));

            Topic t1 = Topic.builder().topicId(1L).prerequisiteTopicId(null).build();
            Topic t2 = Topic.builder().topicId(2L).prerequisiteTopicId(1L).build(); // Requires t1

            when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(100L)).thenReturn(List.of(t1, t2));
            when(questionRepository.findTopicIdsWithMinimumQuestions(anyInt())).thenReturn(List.of(1L, 2L));

            UserTopicMastery m1 = new UserTopicMastery(); m1.setTopicId(1L); m1.setMasteryScore(1.0);
            UserTopicMastery m2 = new UserTopicMastery(); m2.setTopicId(2L); m2.setMasteryScore(0.8);

            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(1L, List.of(1L, 2L)))
                    .thenReturn(List.of(m1, m2));

            LearningPathResponse response = learningPathService.getLearningPath(100L);

            LearningPathResponse.UserMasteryStatus s2 = response.getUserMasteryStatuses().stream()
                    .filter(s -> s.getTopicId() == 2L).findFirst().get();
            assertThat(s2.getState()).isEqualTo(LearningPathResponse.UserMasteryStatus.MasteryState.MASTERED);
        }

        @Test
        @DisplayName("TC_LP1_006: Prerequisite exists + Prereq Mastered + self NOT Mastered -> LEARNING")
        void getLearningPath_PrereqMastered_SelfLearning() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(subjectRepository.findById(100L)).thenReturn(Optional.of(subject));

            Topic t1 = Topic.builder().topicId(1L).prerequisiteTopicId(null).build();
            Topic t2 = Topic.builder().topicId(2L).prerequisiteTopicId(1L).build(); // Requires t1

            when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(100L)).thenReturn(List.of(t1, t2));
            when(questionRepository.findTopicIdsWithMinimumQuestions(anyInt())).thenReturn(List.of(1L, 2L));

            UserTopicMastery m1 = new UserTopicMastery(); m1.setTopicId(1L); m1.setMasteryScore(0.85); // prerequisite met
            UserTopicMastery m2 = new UserTopicMastery(); m2.setTopicId(2L); m2.setMasteryScore(0.5); // self not met

            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(1L, List.of(1L, 2L)))
                    .thenReturn(List.of(m1, m2));

            LearningPathResponse response = learningPathService.getLearningPath(100L);

            LearningPathResponse.UserMasteryStatus s2 = response.getUserMasteryStatuses().stream()
                    .filter(s -> s.getTopicId() == 2L).findFirst().get();
            assertThat(s2.getState()).isEqualTo(LearningPathResponse.UserMasteryStatus.MasteryState.LEARNING);
        }

        @Test
        @DisplayName("TC_LP1_007: Prerequisite exists + Prereq NOT Mastered -> LOCKED (regardless of own score)")
        void getLearningPath_PrereqNotMastered_Locked() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(subjectRepository.findById(100L)).thenReturn(Optional.of(subject));

            Topic t1 = Topic.builder().topicId(1L).prerequisiteTopicId(null).build();
            Topic t2 = Topic.builder().topicId(2L).prerequisiteTopicId(1L).build(); // Requires t1

            when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(100L)).thenReturn(List.of(t1, t2));
            when(questionRepository.findTopicIdsWithMinimumQuestions(anyInt())).thenReturn(List.of(1L, 2L));

            UserTopicMastery m1 = new UserTopicMastery(); m1.setTopicId(1L); m1.setMasteryScore(0.7); // prerequisite not met
            UserTopicMastery m2 = new UserTopicMastery(); m2.setTopicId(2L); m2.setMasteryScore(0.9); // intentionally high to prove it's still locked

            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(1L, List.of(1L, 2L)))
                    .thenReturn(List.of(m1, m2));

            LearningPathResponse response = learningPathService.getLearningPath(100L);

            LearningPathResponse.UserMasteryStatus s2 = response.getUserMasteryStatuses().stream()
                    .filter(s -> s.getTopicId() == 2L).findFirst().get();
            assertThat(s2.getState()).isEqualTo(LearningPathResponse.UserMasteryStatus.MasteryState.LOCKED);
        }

        @Test
        @DisplayName("TC_LP1_008: Prerequisite exists + Prereq Mastery object missing -> LOCKED")
        void getLearningPath_PrereqMasteryNull_Locked() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(subjectRepository.findById(100L)).thenReturn(Optional.of(subject));

            Topic t1 = Topic.builder().topicId(1L).prerequisiteTopicId(null).build();
            Topic t2 = Topic.builder().topicId(2L).prerequisiteTopicId(1L).build(); 

            when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(100L)).thenReturn(List.of(t1, t2));
            when(questionRepository.findTopicIdsWithMinimumQuestions(anyInt())).thenReturn(List.of(1L, 2L));

            // Only m2 exists, m1 missing
            UserTopicMastery m2 = new UserTopicMastery(); m2.setTopicId(2L); m2.setMasteryScore(0.9); 

            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(1L, List.of(1L, 2L)))
                    .thenReturn(List.of(m2)); // m1 is missing

            LearningPathResponse response = learningPathService.getLearningPath(100L);

            LearningPathResponse.UserMasteryStatus s2 = response.getUserMasteryStatuses().stream()
                    .filter(s -> s.getTopicId() == 2L).findFirst().get();
            assertThat(s2.getState()).isEqualTo(LearningPathResponse.UserMasteryStatus.MasteryState.LOCKED);
        }
        
        @Test
        @DisplayName("TC_LP1_009: No mastery object for self -> LEARNING with score 0.0")
        void getLearningPath_NoMastery_DefaultsToLearning() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(subjectRepository.findById(100L)).thenReturn(Optional.of(subject));

            Topic t1 = Topic.builder().topicId(1L).prerequisiteTopicId(null).build();

            when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(100L)).thenReturn(List.of(t1));
            when(questionRepository.findTopicIdsWithMinimumQuestions(anyInt())).thenReturn(List.of(1L));

            // Empty mastery repository
            when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(1L, List.of(1L)))
                    .thenReturn(Collections.emptyList());

            LearningPathResponse response = learningPathService.getLearningPath(100L);

            LearningPathResponse.UserMasteryStatus status = response.getUserMasteryStatuses().get(0);
            assertThat(status.getTopicId()).isEqualTo(1L);
            assertThat(status.getMasteryScore()).isEqualTo(0.0);
            assertThat(status.getState()).isEqualTo(LearningPathResponse.UserMasteryStatus.MasteryState.LEARNING);
        }
    }

    @Nested
    @DisplayName("2. Tests for getMasteryProgress()")
    class GetMasteryProgressTests {

        @Test
        @DisplayName("TC_LP2_001: Returns mapped list of masteries completely mapped")
        void getMasteryProgress_Success() {
            when(authService.getCurrentUser()).thenReturn(currentUser);

            UserTopicMastery m1 = new UserTopicMastery();
            m1.setTopicId(1L);
            m1.setMasteryScore(0.5);
            m1.setTotalAttempts(10);
            m1.setCorrectAttempts(5);
            Topic t1 = Topic.builder().topicId(1L).topicName("Topic 1").subjectId(100L).subject(subject).build();
            m1.setTopic(t1);

            UserTopicMastery m2 = new UserTopicMastery();
            m2.setTopicId(2L);
            m2.setMasteryScore(1.0);
            Topic t2 = Topic.builder().topicId(2L).topicName("Topic 2").subjectId(100L).subject(subject).build();
            m2.setTopic(t2);

            when(userTopicMasteryRepository.findByUserIdAndIsDeletedFalse(1L))
                    .thenReturn(List.of(m1, m2));

            MasteryProgressResponse response = learningPathService.getMasteryProgress();

            assertThat(response).isNotNull();
            assertThat(response.getTopicMasteries()).hasSize(2);
            assertThat(response.getTopicMasteries().get(0).getTopicName()).isEqualTo("Topic 1");
            assertThat(response.getTopicMasteries().get(1).getTopicName()).isEqualTo("Topic 2");
        }

        @Test
        @DisplayName("TC_LP2_002: Null safety when Topic is missing from Mastery")
        void getMasteryProgress_MissingTopic_NullSafety() {
            when(authService.getCurrentUser()).thenReturn(currentUser);

            UserTopicMastery m1 = new UserTopicMastery();
            m1.setTopicId(1L);
            m1.setMasteryScore(0.5);
            m1.setTopic(null); // Force null

            when(userTopicMasteryRepository.findByUserIdAndIsDeletedFalse(1L))
                    .thenReturn(List.of(m1));

            MasteryProgressResponse response = learningPathService.getMasteryProgress();

            assertThat(response.getTopicMasteries().get(0).getTopicId()).isEqualTo(1L);
            assertThat(response.getTopicMasteries().get(0).getTopicName()).isNull();
            assertThat(response.getTopicMasteries().get(0).getSubjectId()).isNull();
            assertThat(response.getTopicMasteries().get(0).getSubjectName()).isNull();
        }

        @Test
        @DisplayName("TC_LP2_003: Null safety when Subject is missing from Topic")
        void getMasteryProgress_MissingSubject_NullSafety() {
            when(authService.getCurrentUser()).thenReturn(currentUser);

            UserTopicMastery m1 = new UserTopicMastery();
            m1.setTopicId(1L);
            m1.setMasteryScore(0.5);
            Topic t1 = Topic.builder().topicId(1L).topicName("Topic 1").subjectId(100L).subject(null).build();
            m1.setTopic(t1);

            when(userTopicMasteryRepository.findByUserIdAndIsDeletedFalse(1L))
                    .thenReturn(List.of(m1));

            MasteryProgressResponse response = learningPathService.getMasteryProgress();

            assertThat(response.getTopicMasteries().get(0).getTopicId()).isEqualTo(1L);
            assertThat(response.getTopicMasteries().get(0).getSubjectId()).isEqualTo(100L);
            assertThat(response.getTopicMasteries().get(0).getSubjectName()).isNull();
        }

        @Test
        @DisplayName("TC_LP2_004: Empty mastery repository -> Returns empty list, no errors")
        void getMasteryProgress_Empty() {
            when(authService.getCurrentUser()).thenReturn(currentUser);
            when(userTopicMasteryRepository.findByUserIdAndIsDeletedFalse(1L))
                    .thenReturn(Collections.emptyList());

            MasteryProgressResponse response = learningPathService.getMasteryProgress();

            assertThat(response).isNotNull();
            assertThat(response.getTopicMasteries()).isEmpty();
        }
    }
}
