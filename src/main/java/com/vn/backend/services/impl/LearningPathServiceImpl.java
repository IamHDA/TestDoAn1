package com.vn.backend.services.impl;

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
import com.vn.backend.services.LearningPathService;
import com.vn.backend.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LearningPathServiceImpl extends BaseService implements LearningPathService {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final UserTopicMasteryRepository userTopicMasteryRepository;
    private final QuestionRepository questionRepository;
    private final AuthService authService;

    private static final double MASTERY_THRESHOLD = 0.8; // Threshold để coi là "mastered"
    private static final int MIN_QUESTIONS_PER_TOPIC = 15; // Tối thiểu số câu hỏi để topic được tham gia hệ thống Học Thích ứng

    public LearningPathServiceImpl(
            MessageUtils messageUtils,
            TopicRepository topicRepository,
            SubjectRepository subjectRepository,
            UserTopicMasteryRepository userTopicMasteryRepository,
            QuestionRepository questionRepository,
            AuthService authService) {
        super(messageUtils);
        this.topicRepository = topicRepository;
        this.subjectRepository = subjectRepository;
        this.userTopicMasteryRepository = userTopicMasteryRepository;
        this.questionRepository = questionRepository;
        this.authService = authService;
    }

    @Override
    public LearningPathResponse getLearningPath(Long subjectId) {
        log.info("Getting learning path for subjectId: {}", subjectId);
        User currentUser = authService.getCurrentUser();

        // Validate subject exists
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));

        // Get all topics of subject 
        List<Topic> allTopics = topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(subjectId);

        // Lấy danh sách topicIds có >= 15 câu hỏi từ database (query hiệu quả hơn)
        List<Long> topicIdsWithEnoughQuestions = questionRepository.findTopicIdsWithMinimumQuestions(MIN_QUESTIONS_PER_TOPIC);
        Set<Long> topicIdsSet = new HashSet<>(topicIdsWithEnoughQuestions);

        // Filter topics: CHỈ giữ lại topics có >= 15 câu hỏi (để tham gia hệ thống Học Thích ứng)
        List<Topic> topics = allTopics.stream()
                .filter(topic -> topicIdsSet.contains(topic.getTopicId()))
                .collect(Collectors.toList());

        // Get user mastery statuses
        List<Long> topicIds = topics.stream().map(Topic::getTopicId).collect(Collectors.toList());
        List<UserTopicMastery> userMasteries = userTopicMasteryRepository
                .findByUserIdAndTopicIdIn(currentUser.getId(), topicIds);

        Map<Long, UserTopicMastery> masteryMap = userMasteries.stream()
                .collect(Collectors.toMap(UserTopicMastery::getTopicId, m -> m));

        // Build response
        List<LearningPathResponse.TopicNode> topicNodes = topics.stream()
                .map(t -> LearningPathResponse.TopicNode.builder()
                        .topicId(t.getTopicId())
                        .topicName(t.getTopicName())
                        .subjectId(t.getSubjectId())
                        .build())
                .collect(Collectors.toList());

        // Build edges from Topic.prerequisiteTopicId (mỗi topic chỉ có tối đa 1 prerequisite)
        List<LearningPathResponse.PrerequisiteEdge> edges = topics.stream()
                .filter(t -> t.getPrerequisiteTopicId() != null)
                .map(t -> LearningPathResponse.PrerequisiteEdge.builder()
                        .fromTopicId(t.getPrerequisiteTopicId())
                        .toTopicId(t.getTopicId())
                        .build())
                .collect(Collectors.toList());

        // Calculate mastery status with prerequisites check
        List<LearningPathResponse.UserMasteryStatus> userMasteryStatuses = topics.stream()
                .map(topic -> {
                    UserTopicMastery mastery = masteryMap.get(topic.getTopicId());
                    Double masteryScore = (mastery != null) ? mastery.getMasteryScore() : 0.0;

                    // Check if prerequisite is satisfied (mỗi topic chỉ có tối đa 1 prerequisite)
                    boolean allPrerequisitesMastered = true;
                    if (topic.getPrerequisiteTopicId() != null) {
                        UserTopicMastery prereqMastery = masteryMap.get(topic.getPrerequisiteTopicId());
                        if (prereqMastery == null || prereqMastery.getMasteryScore() < MASTERY_THRESHOLD) {
                            allPrerequisitesMastered = false;
                        }
                    }

                    // Determine state
                    LearningPathResponse.UserMasteryStatus.MasteryState state;
                    if (!allPrerequisitesMastered) {
                        state = LearningPathResponse.UserMasteryStatus.MasteryState.LOCKED;
                    } else if (masteryScore >= MASTERY_THRESHOLD) {
                        state = LearningPathResponse.UserMasteryStatus.MasteryState.MASTERED;
                    } else {
                        state = LearningPathResponse.UserMasteryStatus.MasteryState.LEARNING;
                    }

                    return LearningPathResponse.UserMasteryStatus.builder()
                            .topicId(topic.getTopicId())
                            .masteryScore(masteryScore)
                            .state(state)
                            .build();
                })
                .collect(Collectors.toList());

        return LearningPathResponse.builder()
                .subjectId(subject.getSubjectId())
                .subjectName(subject.getSubjectName())
                .topics(topicNodes)
                .prerequisites(edges)
                .userMasteryStatuses(userMasteryStatuses)
                .build();
    }

    @Override
    public MasteryProgressResponse getMasteryProgress() {
        log.info("Getting mastery progress for current user");
        User currentUser = authService.getCurrentUser();

        List<UserTopicMastery> masteries = userTopicMasteryRepository
                .findByUserIdAndIsDeletedFalse(currentUser.getId());

        List<MasteryProgressResponse.TopicMastery> topicMasteries = masteries.stream()
                .map(m -> {
                    Topic topic = m.getTopic();
                    Subject subject = topic != null ? topic.getSubject() : null;
                    return MasteryProgressResponse.TopicMastery.builder()
                            .topicId(m.getTopicId())
                            .topicName(topic != null ? topic.getTopicName() : null)
                            .subjectId(topic != null ? topic.getSubjectId() : null)
                            .subjectName(subject != null ? subject.getSubjectName() : null)
                            .masteryScore(m.getMasteryScore())
                            .totalAttempts(m.getTotalAttempts())
                            .correctAttempts(m.getCorrectAttempts())
                            .build();
                })
                .collect(Collectors.toList());

        return MasteryProgressResponse.builder()
                .topicMasteries(topicMasteries)
                .build();
    }
}


