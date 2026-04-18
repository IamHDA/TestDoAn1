package com.vn.backend.services;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.topic.CreateApprovalTopicRequest;
import com.vn.backend.dto.request.topic.CreateTopicRequest;
import com.vn.backend.dto.request.topic.TopicFilterRequest;
import com.vn.backend.dto.request.topic.UpdateTopicRequest;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.topic.TopicDetailResponse;
import com.vn.backend.dto.response.topic.TopicResponse;

public interface TopicService {
    void approvalRequestTopic(CreateApprovalTopicRequest request);


    ResponseListData<TopicResponse> searchTopics(BaseFilterSearchRequest<TopicFilterRequest> request);

     void updateTopic(Long topicId, UpdateTopicRequest request);

     void deleteTopic(Long topicId);
}
