package com.vn.backend.services;

import com.vn.backend.dto.request.announcement.AnnouncementCreateRequest;
import com.vn.backend.dto.request.announcement.AnnouncementListRequest;
import com.vn.backend.dto.request.announcement.AnnouncementUpdateRequest;
import com.vn.backend.dto.request.assignment.AssignmentCreateRequest;
import com.vn.backend.dto.request.assignment.AssignmentUpdateRequest;
import com.vn.backend.dto.response.announcement.AnnouncementResponse;
import com.vn.backend.dto.response.assignment.AssignmentResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.entities.Announcement;
import com.vn.backend.entities.Assignment;
import com.vn.backend.entities.Classroom;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.enums.AnnouncementType;

public interface AnnouncementService {
    
    void createAnnouncement(Long classroomId, AnnouncementCreateRequest request);
    
    ResponseListData<AnnouncementResponse> getAnnouncementList(Long classroomId, AnnouncementListRequest request);
    
    AnnouncementResponse getAnnouncementDetail(Long announcementId);
    
    void updateAnnouncement(Long announcementId, AnnouncementUpdateRequest request);
    
    void deleteAnnouncement(Long announcementId);
    void notifyAnnouncement(Announcement announcement);
    String getTitleWithAnnouncementType(AnnouncementType announcementType, Classroom classroom, String title );
}