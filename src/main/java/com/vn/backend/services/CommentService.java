package com.vn.backend.services;

import com.vn.backend.dto.request.comment.CommentCreateRequest;
import com.vn.backend.dto.request.comment.CommentListRequest;
import com.vn.backend.dto.request.comment.CommentUpdateRequest;
import com.vn.backend.dto.request.comment.CommentDeleteRequest;
import com.vn.backend.dto.response.comment.CommentResponse;
import com.vn.backend.dto.response.common.ResponseListData;

public interface CommentService {

    /**
     * Create a new comment
     */
    CommentResponse createComment(Long announcementId, CommentCreateRequest request);

    /**
     * Get list of comments for an announcement
     */
    ResponseListData<CommentResponse> getCommentList(CommentListRequest request);

    /**
     * Update a comment
     */
    CommentResponse updateComment(CommentUpdateRequest request);

    /**
     * Delete a comment (soft delete)
     */
    void deleteComment(CommentDeleteRequest request);
}
