package com.vn.backend.services;

import com.vn.backend.dto.response.learningpath.LearningPathResponse;
import com.vn.backend.dto.response.progress.MasteryProgressResponse;

public interface LearningPathService {
    LearningPathResponse getLearningPath(Long subjectId);
    MasteryProgressResponse getMasteryProgress();
}

