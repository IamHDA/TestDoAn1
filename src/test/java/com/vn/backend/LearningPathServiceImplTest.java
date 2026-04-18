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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LearningPathServiceImpl Unit Tests")
class LearningPathServiceImplTest {

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
    private MessageUtils messageUtils;

    @InjectMocks
    private LearningPathServiceImpl learningPathService;

    private User currentUser;
    private Subject subject;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .username("student1")
                .build();

        subject = Subject.builder()
                .subjectId(100L)
                .subjectName("Mathematics")
                .build();
    }

    // ===================== getLearningPath =====================

    @Test
    @DisplayName("getLearningPath - thành công tạo learning path với trạng thái mastery chính xác")
    void getLearningPath_Success() {
        // Mock currentUser
        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(subjectRepository.findById(100L)).thenReturn(Optional.of(subject));

        // Mock 3 topics: T1 (root), T2 (requires T1), T3 (requires T2)
        Topic t1 = Topic.builder().topicId(1L).subjectId(100L).topicName("Topic 1").prerequisiteTopicId(null).build();
        Topic t2 = Topic.builder().topicId(2L).subjectId(100L).topicName("Topic 2").prerequisiteTopicId(1L).build();
        Topic t3 = Topic.builder().topicId(3L).subjectId(100L).topicName("Topic 3").prerequisiteTopicId(2L).build();

        when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(100L))
                .thenReturn(List.of(t1, t2, t3));

        // Mock questions rule: only T1 and T2 have >= 15 questions
        when(questionRepository.findTopicIdsWithMinimumQuestions(15))
                .thenReturn(List.of(1L, 2L)); // T3 is omitted

        // Mock mastery: user has 0.9 (Mastered) in T1
        UserTopicMastery m1 = new UserTopicMastery();
        m1.setTopicId(1L);
        m1.setMasteryScore(0.9);

        // Expected check mastery on included topics (1, 2)
        when(userTopicMasteryRepository.findByUserIdAndTopicIdIn(1L, List.of(1L, 2L)))
                .thenReturn(List.of(m1));

        LearningPathResponse response = learningPathService.getLearningPath(100L);

        // Verification
        assertThat(response).isNotNull();
        assertThat(response.getSubjectId()).isEqualTo(100L);
        
        // Only T1 and T2 remain
        assertThat(response.getTopics()).hasSize(2);
        
        // Edge: T1 -> T2
        assertThat(response.getPrerequisites()).hasSize(1);
        assertThat(response.getPrerequisites().get(0).getFromTopicId()).isEqualTo(1L);
        assertThat(response.getPrerequisites().get(0).getToTopicId()).isEqualTo(2L);

        // Status check
        List<LearningPathResponse.UserMasteryStatus> statuses = response.getUserMasteryStatuses();
        assertThat(statuses).hasSize(2);

        // T1 has no prereq, score=0.9 -> MASTERED
        LearningPathResponse.UserMasteryStatus s1 = statuses.stream().filter(s -> s.getTopicId() == 1L).findFirst().get();
        assertThat(s1.getState()).isEqualTo(LearningPathResponse.UserMasteryStatus.MasteryState.MASTERED);

        // T2 prereq is T1 (score=0.9 >= 0.8), but T2 score=0.0 -> LEARNING
        LearningPathResponse.UserMasteryStatus s2 = statuses.stream().filter(s -> s.getTopicId() == 2L).findFirst().get();
        assertThat(s2.getState()).isEqualTo(LearningPathResponse.UserMasteryStatus.MasteryState.LEARNING);
    }

    @Test
    @DisplayName("getLearningPath - ném exception khi subject không tồn tại")
    void getLearningPath_ThrowsException_WhenSubjectNotFound() {
        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(subjectRepository.findById(100L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> learningPathService.getLearningPath(100L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    // ===================== getMasteryProgress =====================

    @Test
    @DisplayName("getMasteryProgress - lấy tiến độ học tập thành công")
    void getMasteryProgress_Success() {
        when(authService.getCurrentUser()).thenReturn(currentUser);

        UserTopicMastery m1 = new UserTopicMastery();
        m1.setTopicId(1L);
        m1.setMasteryScore(0.5);
        m1.setTotalAttempts(10);
        m1.setCorrectAttempts(5);
        
        Topic t1 = Topic.builder().topicId(1L).topicName("Topic 1").subjectId(100L).subject(subject).build();
        m1.setTopic(t1);

        when(userTopicMasteryRepository.findByUserIdAndIsDeletedFalse(1L))
                .thenReturn(List.of(m1));

        MasteryProgressResponse response = learningPathService.getMasteryProgress();

        assertThat(response).isNotNull();
        assertThat(response.getTopicMasteries()).hasSize(1);

        MasteryProgressResponse.TopicMastery tm = response.getTopicMasteries().get(0);
        assertThat(tm.getTopicId()).isEqualTo(1L);
        assertThat(tm.getTopicName()).isEqualTo("Topic 1");
        assertThat(tm.getSubjectId()).isEqualTo(100L);
        assertThat(tm.getSubjectName()).isEqualTo("Mathematics");
        assertThat(tm.getMasteryScore()).isEqualTo(0.5);
        assertThat(tm.getTotalAttempts()).isEqualTo(10);
        assertThat(tm.getCorrectAttempts()).isEqualTo(5);
    }
}
