package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.answer.AnswerCreateRequest;
import com.vn.backend.dto.request.question.CreateApprovalQuestionRequest;
import com.vn.backend.dto.request.question.QuestionBulkCreateItemRequest;
import com.vn.backend.dto.request.question.QuestionBulkAnswerCreateRequest;
import com.vn.backend.dto.request.question.QuestionBulkCreateRequest;
import com.vn.backend.dto.request.question.QuestionCreateRequest;
import com.vn.backend.dto.request.question.QuestionUpdateRequest;
import com.vn.backend.dto.request.question.QuestionSearchRequest;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.question.QuestionDetailResponse;
import com.vn.backend.dto.response.question.QuestionSearchResponse;
import com.vn.backend.entities.Answer;
import com.vn.backend.entities.Question;
import com.vn.backend.entities.Topic;
import com.vn.backend.entities.User;
import com.vn.backend.enums.QuestionType;
import com.vn.backend.enums.RequestType;
import com.vn.backend.enums.Role;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AnswerRepository;
import com.vn.backend.repositories.QuestionRepository;
import com.vn.backend.repositories.TopicRepository;
import com.vn.backend.services.ApprovalRequestService;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.impl.QuestionServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionServiceImpl Unit Tests")
class QuestionServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private ApprovalRequestService approvalRequestService;

    @Mock
    private AuthService authService;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private QuestionServiceImpl questionService;

    private User teacherUser;
    private User adminUser;
    private Topic topic;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder()
                .id(1L)
                .username("teacher1")
                .role(Role.TEACHER)
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("admin1")
                .role(Role.ADMIN)
                .build();

        topic = Topic.builder()
                .topicId(10L)
                .topicName("Topic 1")
                .isDeleted(false)
                .build();
    }

    // ===================== createQuestion =====================

    @Test
    @DisplayName("createQuestion - thành công khi payload hợp lệ")
    void createQuestion_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));

        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setTopicId(10L);
        request.setContent("What is 1+1?");
        request.setType(QuestionType.SINGLE_CHOICE);
        
        AnswerCreateRequest a1 = new AnswerCreateRequest(); a1.setContent("1"); a1.setIsCorrect(false);
        AnswerCreateRequest a2 = new AnswerCreateRequest(); a2.setContent("2"); a2.setIsCorrect(true); // 1 correct answer
        request.setAnswers(List.of(a1, a2));

        Question savedQ = Question.builder().questionId(100L).topicId(10L).build();
        when(questionRepository.saveAndFlush(any(Question.class))).thenReturn(savedQ);
        when(topicRepository.findByTopicIdAndIsDeletedFalse(10L)).thenReturn(topic); // used inside mapToDetailResponse
        when(answerRepository.findByQuestionIdOrderByDisplayOrder(100L)).thenReturn(new ArrayList<>());

        QuestionDetailResponse res = questionService.createQuestion(request);

        assertThat(res).isNotNull();
        verify(questionRepository).saveAndFlush(any(Question.class));
        verify(answerRepository, times(2)).save(any(Answer.class));
    }

    @Test
    @DisplayName("createQuestion - ném lỗi khi SINGLE_CHOICE có nhiều đáp án đúng")
    void createQuestion_ThrowsException_WhenSingleChoiceHasMultipleCorrectAnswers() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));

        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setTopicId(10L);
        request.setType(QuestionType.SINGLE_CHOICE);
        
        AnswerCreateRequest a1 = new AnswerCreateRequest(); a1.setContent("A"); a1.setIsCorrect(true);
        AnswerCreateRequest a2 = new AnswerCreateRequest(); a2.setContent("B"); a2.setIsCorrect(true); // 2 correct
        request.setAnswers(List.of(a1, a2));

        when(messageUtils.getMessage(AppConst.MessageConst.INVALID_LOGIC_QUESTION)).thenReturn("Invalid Answers");

        assertThatThrownBy(() -> questionService.createQuestion(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.INVALID_LOGIC_QUESTION);
                });
    }

    // ===================== searchQuestions =====================

    @Test
    @DisplayName("searchQuestions - quyền ADMIN thì không xét userId trong query")
    void searchQuestions_AsAdmin() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        BaseFilterSearchRequest<QuestionSearchRequest> req = mock(BaseFilterSearchRequest.class);
        SearchRequest pagination = mock(SearchRequest.class);
        PagingMeta pagingMeta = new PagingMeta();
        pagingMeta.setPageNum(1);
        pagingMeta.setPageSize(10);
        
        when(req.getPagination()).thenReturn(pagination);
        when(pagination.getPagingMeta()).thenReturn(pagingMeta);
        when(req.getFilters()).thenReturn(new QuestionSearchRequest());

        Question q = Question.builder().questionId(100L).content("Q1").type(QuestionType.SINGLE_CHOICE).topic(topic).build();
        Page<Question> page = new PageImpl<>(List.of(q));

        when(questionRepository.searchQuestionsForAdmin(
                any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(page);

        ResponseListData<QuestionSearchResponse> res = questionService.searchQuestions(req);

        assertThat(res.getContent()).hasSize(1);
        verify(questionRepository).searchQuestionsForAdmin(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    // ===================== updateQuestion =====================

    @Test
    @DisplayName("updateQuestion - thành công cập nhật khi là owner")
    void updateQuestion_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser); // ID = 1
        
        Question q = Question.builder().questionId(100L).topicId(10L).createdBy(1L).isReviewQuestion(true).build();
        when(questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(100L, 1L)).thenReturn(Optional.of(q));
        when(questionRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(topicRepository.findByTopicIdAndIsDeletedFalse(10L)).thenReturn(topic);

        QuestionUpdateRequest request = new QuestionUpdateRequest();
        request.setContent("New content");

        QuestionDetailResponse res = questionService.updateQuestion(100L, request);

        assertThat(res.getContent()).isEqualTo("New content");
        assertThat(q.getIsReviewQuestion()).isFalse(); // Được reset
        verify(questionRepository).saveAndFlush(q);
    }

    // ===================== createApprovalQuestion =====================

    @Test
    @DisplayName("createApprovalQuestion - thành công gọi ApprovalRequestService")
    void createApprovalQuestion_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        
        Question q = Question.builder().questionId(100L).createdBy(1L).isDeleted(false).build();
        when(questionRepository.findById(100L)).thenReturn(Optional.of(q));

        CreateApprovalQuestionRequest request = new CreateApprovalQuestionRequest();
        request.setQuestionIds(List.of(100L));
        request.setRequestType(RequestType.QUESTION_REVIEW_CREATE);
        request.setDescription("To review");

        questionService.createApprovalQuestion(request);

        verify(approvalRequestService).createRequest(
                eq(RequestType.QUESTION_REVIEW_CREATE), eq("To review"), eq(1L), eq(List.of(100L))
        );
    }
}
