package com.vn.backend.constants;

import com.vn.backend.enums.EntityType;
import com.vn.backend.enums.NotificationObjectType;
import com.vn.backend.enums.RequestType;
import com.vn.backend.enums.TitleAnnouncementType;
import java.security.SecureRandom;
import java.util.Map;

public class AppConst {

  public static final String API = "/api";
  public static final String ENDPOINT_DOWNLOAD_FILE = "api/files/download/";
  public static final String BEARER = "Bearer ";
  public static final long COUNTDOWN_S = 5 * 60;
  public static final long COUNTDOWN_M = 5;
  public static final long TTL_BUFFER = 60 * 60;
  public static final long HEARTBEAT_CUTOFF_SECONDS = 40;
  public static final String MESSAGE_SPLIT = ";,;";
  public static final String CHAR_POOL = "abcdefghijklmnopqrstuvwxyz0123456789";
  public static final int CODE_LENGTH = 6;
  public static final SecureRandom RANDOM = new SecureRandom();
  public static final int MAX_TRIES = 10;
  public static final String DOT_CONSTANT = ".";
  public static final int DOT_CONSTANT_POSITION = 1;
  public static final long MEGABYTE = (long) 1_000_000.0f;
  public static final float FILE_SIZE = 25.0f;
  public static final String EDIT = "EDIT";
  public static final String DELETE = "DELETE";
  public static final String CREATE = "CREATE";
  public static final String COPY = "COPY";
  public static final Map<NotificationObjectType, String> TITLE_NOTIFICATION_MAP = Map.of(
      NotificationObjectType.ASSIGNMENT, "%s đã đăng một bài tập trong lớp %s",
      NotificationObjectType.ANNOUNCEMENT, "%s đã đăng một thông báo trong lớp %s",
      NotificationObjectType.EXAM_CREATED, "Kỳ thi mới được %s tạo trong lớp %s",
      NotificationObjectType.INVITE_CLASS, "%s đã gửi lời mời tham gia lớp %s",
      NotificationObjectType.JOIN_CLASS, "%s đã tham gia lớp %s",
      NotificationObjectType.MATERIAL, "%s đã đăng một tài liệu trong lớp %s",
      NotificationObjectType.EXAM_JOINED, "%s đã thêm bạn vào kỳ thi %s"
  );

  public static final Map<TitleAnnouncementType, String> TITLE_ANNOUCEMENT_MAP = Map.of(
      TitleAnnouncementType.ASSIGNMENT, "%s đã đăng một bài tập mới trong lớp %s",
      TitleAnnouncementType.EXAM, "%s đã đăng một bài thi mới trong lớp %s",
      TitleAnnouncementType.MATERIAL, "%s đã đăng một tài liệu mới trong lớp %s",
      TitleAnnouncementType.GENERIC, "%s đã đăng một thông báo mới trong lớp %s"
  );

  public static final Map<RequestType, EntityType> REQUEST_TYPE_ENTITY_TYPE_MAP = Map.of(
          RequestType.CLASS_CREATE, EntityType.CLASS,
          RequestType.TOPIC_CREATE, EntityType.TOPIC,
          RequestType.QUESTION_REVIEW_CREATE, EntityType.QUESTION
  );


  private AppConst() {
  }

  public static class RegexConst {

    // a-z, A-Z, 0-9, Alphanumeric
    public static final String ALPHANUMERIC = "^[\\u0020\\u0030-\\u0039\\u0041-\\u005A\\u0061-\\u007A]*$";
    // Integer (with optional minus)
    public static final String INTEGER = "^[\\u0030-\\u0039]*$";
    // Số thực (có thể âm) tối đa 2 chữ số thập phân, ví dụ: -3.14, 0, 2.5, 10.00
    public static final String DECIMAL_2 = "^-?[0-9]+(\\.[0-9]{1,2})?$";
    // Số không âm tối đa 2 chữ số thập phân, ví dụ hợp lệ: 0, 0.5, 10, 99.99
    public static final String NON_NEGATIVE_DECIMAL_2 = "^[0-9]+(\\.[0-9]{1,2})?$";

    private RegexConst() {
    }
  }

  public static class MessageConst {

    public static final String REQUIRED_FIELD_EMPTY = "REQUIRED_FIELD_EMPTY";
    public static final String VALUE_TOO_LONG = "VALUE_TOO_LONG";
    public static final String VALUE_OUT_OF_RANGE = "VALUE_OUT_OF_RANGE";
    public static final String INVALID_CHARACTER = "INVALID_CHARACTER";
    public static final String INVALID_NUMBER_FORMAT = "INVALID_NUMBER_FORMAT";
    public static final String SERVER_UNAVAILABLE = "SERVER_UNAVAILABLE";
    public static final String CONNECTION_FAILED = "CONNECTION_FAILED";


    public static final String CLASS_NOT_FOUND = "CLASS_NOT_FOUND";
    public static final String ERR_CLASS_CODE_GENERATION_FAILED = "ERR_CLASS_CODE_GENERATION_FAILED";
    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
    public static final String USERNAME_ALREADY_EXISTS = "USERNAME_ALREADY_EXISTS";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String CODE_ALREADY_EXISTS = "CODE_ALREADY_EXISTS";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";

    // Invitation messages
    public static final String INVITATION_PERMISSION_DENIED = "INVITATION_PERMISSION_DENIED";
    public static final String INVITATION_USER_ALREADY_MEMBER = "INVITATION_USER_ALREADY_MEMBER";
    public static final String INVITATION_PENDING_EXISTS = "INVITATION_PENDING_EXISTS";
    public static final String INVITATION_ALREADY_RESPONDED = "INVITATION_ALREADY_RESPONDED";
    public static final String INVITATION_INVALID_ROLE = "INVITATION_INVALID_ROLE";
    public static final String INVITATION_NOT_FOUND = "INVITATION_NOT_FOUND";
    public static final String CLASSROOM_NOT_FOUND = "CLASSROOM_NOT_FOUND";
    public static final String CLASS_CODE_DISABLED = "CLASS_CODE_DISABLED";
    public static final String REFRESH_TOKEN_EXPIRED = "REFRESH_TOKEN_EXPIRED";
    public static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";

    public static final String FILE_IS_EMPTY = "FILE_IS_EMPTY";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String FILE_TYPE_NOT_ALLOWED = "FILE_TYPE_NOT_ALLOWED";
    public static final String FILE_UPLOAD_FAILED = "FILE_UPLOAD_FAILED";
    public static final String FILE_DOWNLOAD_FAILED = "FILE_DOWNLOAD_FAILED";


    // Comment
    public static final String COMMENT_NOT_ALLOW = "COMMENT_NOT_ALLOW";

    public static final String LATE_SUBMISSION_NOT_ALLOWED = "LATE_SUBMISSION_NOT_ALLOWED";
    public static final String IMPORT_MISSING_STUDENT_INFO = "IMPORT_MISSING_STUDENT_INFO";
    public static final String IMPORT_INVALID_GRADE_FORMAT = "IMPORT_INVALID_GRADE_FORMAT";
    public static final String IMPORT_SUBMISSION_NOT_FOUND = "IMPORT_SUBMISSION_NOT_FOUND";
    public static final String CANNOT_POST = "CANNOT_POST";

    public static final String SUBJECT_CODE_ALREADY_EXISTS = "SUBJECT_CODE_ALREADY_EXISTS";
    public static final String INVALID_LOGIC_QUESTION = "INVALID_LOGIC_QUESTION";
    public static final String CURRENT_DATE_AFTER_END_DATE = "CURRENT_DATE_AFTER_END_DATE";
    public static final String EXAM_ALREADY_STARTED = "EXAM_ALREADY_STARTED";
    public static final String NOT_IN_CLASSROOM = "NOT_IN_CLASSROOM";
    public static final String EXAM_EMPTY = "EXAM_EMPTY";
    public static final String EXAM_ENDED = "EXAM_ENDED";
    public static final String EXAM_NOT_READY = "EXAM_NOT_READY";
    public static final String EXAM_ALREADY_SUBMITTED = "EXAM_ALREADY_SUBMITTED";
    public static final String EXAM_NOT_ONGOING = "EXAM_NOT_ONGOING";
    public static final String EXAM_NOT_STARTED = "EXAM_NOT_STARTED";
    public static final String ANSWER_KEY_NOT_FOUND = "ANSWER_KEY_NOT_FOUND";
    public static final String INVALID_EXAM_TOKEN = "INVALID_EXAM_TOKEN";
    public static final String EXAM_CONTENT_NOT_FOUND = "EXAM_CONTENT_NOT_FOUND";
    public static final String INVALID_QUESTION = "INVALID_QUESTION";
    public static final String INVALID_CHOICE = "INVALID_CHOICE";
    public static final String EXAM_NOT_ENDED_YET = "EXAM_NOT_ENDED_YET";
    public static final String CANNOT_REMOVE_STUDENT_EXAM_PARTICIPATED = "CANNOT_REMOVE_STUDENT_EXAM_PARTICIPATED";
    public static final String CLASS_CODE_LENGTH_INVALID = "CLASS_CODE_LENGTH_INVALID";
    private MessageConst() {
    }
  }

  public static class FieldConst {

    public static final String SORT_KEY = "SORT_KEY";
    public static final String SORT_ORDER = "SORT_ORDER";
    public static final String PAGE_NUM = "PAGE_NUM";
    public static final String PAGE_SIZE = "PAGE_SIZE";

    public static final String CLASS_NAME = "CLASS_NAME";
    public static final String CLASSROOM_STATUS = "CLASSROOM_STATUS";
    public static final String CLASSROOM_ID = "CLASSROOM_ID";
    public static final String CLASS_CODE_STATUS = "CLASS_CODE_STATUS";

    public static final String GRADE_CALCULATION_METHOD = "GRADE_CALCULATION_METHOD";
    public static final String LATE_SUBMISSION_POLICY = "LATE_SUBMISSION_POLICY";

    public static final String CLASS_MEMBER_ROLE = "CLASS_MEMBER_ROLE";
    public static final String CLASS_MEMBER_STATUS = "CLASS_MEMBER_STATUS";
    public static final String MEMBER_ID = "MEMBER_ID";
    public static final String ANNOUNCEMENT_ID = "ANNOUNCEMENT_ID";
    public static final String COMMENT_ID = "COMMENT_ID";
    public static final String NOTIFICATION_ID = "NOTIFICATION_ID";

    public static final String ASSIGNMENT_ID = "ASSIGNMENT_ID";
    public static final String SUBMISSION_ID = "SUBMISSION_ID";
    public static final String ATTACHMENT_ID = "ATTACHMENT_ID";
    public static final String GRADE = "GRADE";
    public static final String USER_ID = "USER_ID";
    public static final String SUBJECT_CODE = "SUBJECT_CODE";
    public static final String SUBJECT_NAME = "SUBJECT_CODE";
    public static final String SUBJECT_ID = "SUBJECT_ID";
    public static final String EXAM_ID = "EXAM_ID";
    public static final String QUESTION_ID = "QUESTION_ID";
    public static final String ORDER_INDEX = "ORDER_INDEX";
    public static final String SCORE = "SCORE";
    public static final String START_DATE = "START_DATE";
    public static final String END_DATE = "END_DATE";
    public static final String SESSION_EXAM_ID = "SESSION_EXAM_ID";
    public static final String DIFFICULTY_LEVEL = "DIFFICULTY_LEVEL";
    public static final String TYPE = "TYPE";
    public static final String TOPIC_ID = "TOPIC_ID";
    public static final String EXAM_MODE = "EXAM_MODE";
    public static final String STUDENT_SESSION_EXAM_ID = "STUDENT_SESSION_EXAM_ID";
    public static final String CLASS_SCHEDULES = "CLASS_SCHEDULES";
    public static final String COURSE_OUTLINE_URL = "COURSE_OUTLINE_URL";
    public static final String START_TIME = "START_TIME";
    public static final String END_TIME = "END_TIME";
    public static final String ROOM = "ROOM";
    public static final String DAY_OF_WEEK = "DAY_OF_WEEK";
    public static final String REQUEST_TYPE = "REQUEST_TYPE";
    public static final String REQUEST_STATUS = "REQUEST_STATUS";
    public static final String CREATED_AT = "CREATED_AT";
    public static final String CLASS_CODE = "CLASS_CODE";


    // EXAM SESSION STATE FIELDS
    public static final String INSTRUCTOR_ID = "instructorId";
    public static final String SESSION_EXAM_ID_KEY = "sessionExamId";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String QUESTION_ORDER_MODE = "questionOrderMode";
    public static final String IS_INSTANTLY_RESULT = "isInstantlyResult";
    public static final String COUNTDOWN_START_AT = "countdownStartAt";
    public static final String DURATION = "duration";
    public static final String TOTAL_STUDENTS = "totalStudents";
    public static final String JOINED_COUNT = "joinedCount";
    public static final String DOWNLOADED_COUNT = "downloadedCount";
    public static final String SUBMITTED_COUNT = "submittedCount";
    public static final String VIOLATION_COUNT = "violationCount";
    public static final String READY_AT = "readyAt";
    public static final String EXAM_END_AT = "examEndAt";

    // STUDENT STATUS FIELDS
    public static final String STATUS = "status";
    public static final String JOINED_AT = "joinedAt";
    public static final String DOWNLOADED_AT = "downloadedAt";
    public static final String SUBMITTED_AT = "submittedAt";
    public static final String SESSION_TOKEN = "sessionToken";
    public static final String STUDENT_CODE = "studentCode";
    public static final String FULL_NAME = "fullName";
    public static final String EMAIL = "email";
    public static final String VIOLATIONS = "violations";
    public static final String ANSWERED_COUNT = "answeredCount";
    public static final String LAST_SAVE_AT = "lastSaveAt";
    public static final String LAST_HEARTBEAT = "lastHeartbeat";

    // GRADING FIELDS
    public static final String SCORE_KEY = "score";
    public static final String CORRECT_COUNT = "correctCount";
    public static final String TOTAL_QUESTIONS = "totalQuestions";
    public static final String AUTO_GRADED = "autoGraded";

    // EXAM CONTENT FIELDS
    public static final String QUESTIONS = "questions";
    public static final String QUESTION_COUNT = "questionCount";

    public static final String LAST_HEARTBEAT_AT = "lastHeartbeatAt";
    public static final String LAST_VIOLATION_TYPE = "lastViolationType";
    public static final String LAST_VIOLATION_AT = "lastViolationAt";
    public static final String LAST_VIOLATION_DESC = "lastViolationDesc";

    public static final String BATCH_GRADED_AT = "BATCH_GRADED_AT";

    private FieldConst() {

    }
  }
}
