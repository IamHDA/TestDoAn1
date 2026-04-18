package com.vn.backend.utils;

public final class RedisUtils {

  private RedisUtils() {
  }

  /**
   * Exam session state Pattern: exam:session:{sessionExamId}:state Type: Hash Fields:
   * countdownStartAt, duration, joinedCount, downloadedCount, submittedCount, violationCount
   */
  public static final String EXAM_SESSION_STATE = "exam:session:%s:state";

  /**
   * Exam content (JSON nguyên khối) Pattern: exam:session:{sessionExamId}:content Type: String
   */
  public static final String EXAM_SESSION_CONTENT = "exam:session:%s:content";

  /**
   * Exam content (JSON nguyên khối) Pattern: exam:session:{sessionExamId}:{studentId}:content Type:
   * String
   */
  public static final String EXAM_SESSION_STUDENT_CONTENT = "exam:session:%s:%s:content";
  /**
   * Student answers Pattern: exam:session:{sessionExamId}:student:{studentId}:answers Type: Hash
   * Fields: {questionId} -> JSON {"selected":["c1"],"answeredAt":123456}
   */
  public static final String STUDENT_ANSWERS = "exam:session:%s:student:%s:answers";

  /**
   * Student question order (cho RANDOM mode) Pattern:
   * exam:session:{sessionExamId}:student:{studentId}:order Type: List
   */
  public static final String STUDENT_QUESTION_ORDER = "exam:session:%s:student:%s:order";

  /**
   * Student exam content (toàn bộ đề thi của sinh viên)
   * Pattern: exam:session:{sessionExamId}:student:{studentId}:exam
   * Type: String (JSON)
   */
  public static final String STUDENT_EXAM_CONTENT = "exam:session:%s:student:%s:exam";

  /**
   * Student status Pattern: exam:session:{sessionExamId}:student:{studentId}:status Type: Hash
   * Fields: status, joinedAt, downloadedAt, lastSaveAt, submittedAt, score, violations,
   * answeredCount, saveCount
   */
  public static final String STUDENT_STATUS = "exam:session:%s:student:%s:status";

  /**
   * Session token Pattern: exam:session:{sessionExamId}:token:{studentId} Type: String
   */
  public static final String SESSION_TOKEN = "exam:session:%s:token:%s";

  /**
   * Students set (danh sách tất cả SV trong ca thi) Pattern: exam:session:{sessionExamId}:students
   * Type: Set
   */
  public static final String SESSION_STUDENTS = "exam:session:%s:students";

  /**
   * Active students (sorted set với heartbeat) Pattern: exam:session:{sessionExamId}:active Type:
   * Sorted Set Score: lastActiveTimestamp
   */
  public static final String SESSION_ACTIVE_STUDENTS = "exam:session:%s:active:%s";

  /**
   * Violation logs Pattern: exam:session:{sessionExamId}:violations:{studentId} Type: List
   */
  public static final String VIOLATION_LOGS = "exam:session:%s:violations:%s";

  /**
   * Distributed lock for grading Pattern: lock:exam:session:{sessionExamId}:grade Type: String
   */
  public static final String GRADING_LOCK = "lock:exam:session:%s:grade";

  /**
   * Pattern để tìm tất cả keys của một exam session Pattern: exam:session:{sessionExamId}:*
   */
  public static final String EXAM_SESSION_ALL_PATTERN = "exam:session:%s:*";

  /**
   * Pattern để tìm tất cả student data trong một session Pattern:
   * exam:session:{sessionExamId}:student:*
   */
  public static final String EXAM_SESSION_STUDENTS_PATTERN = "exam:session:%s:student:*";

  /**
   * Pattern để tìm tất cả violations trong một session Pattern:
   * exam:session:{sessionExamId}:violations:*
   */
  public static final String EXAM_SESSION_VIOLATIONS_PATTERN = "exam:session:%s:violations:*";

  public static final String EXAM_SESSION_STUDENTS = "exam:session:%s:students";

  /**
   * Activity history cho toàn bộ ca thi (tất cả events)
   * Pattern: exam:session:{sessionExamId}:activity:history
   * Type: List
   */
  public static final String SESSION_ACTIVITY_HISTORY = "exam:session:%s:activity:history";

  public static String sessionActivityHistory(Long sessionExamId) {
    return String.format(SESSION_ACTIVITY_HISTORY, sessionExamId);
  }

  /**
   * Students set (all eligible students for this exam session)
   * exam:session:{sessionExamId}:students
   */
  public static String examSessionStudents(Long sessionExamId) {
    return String.format(EXAM_SESSION_STUDENTS, sessionExamId);
  }

  /**
   * Build exam session state key
   */
  public static String examSessionState(Long sessionExamId) {
    return String.format(EXAM_SESSION_STATE, sessionExamId);
  }

  /**
   * Build exam session content key
   */
  public static String examSessionContent(Long sessionExamId) {
    return String.format(EXAM_SESSION_CONTENT, sessionExamId);
  }

  /**
   * Build exam session content key for student
   */
  public static String examSessionStudentContent(Long sessionExamId, Long studentId) {
    return String.format(EXAM_SESSION_STUDENT_CONTENT, sessionExamId, studentId);
  }

  /**
   * Build student answers key
   */
  public static String studentAnswers(Long sessionExamId, Long studentId) {
    return String.format(STUDENT_ANSWERS, sessionExamId, studentId);
  }

  /**
   * Build student question order key
   */
  public static String studentQuestionOrder(Long sessionExamId, Long studentId) {
    return String.format(STUDENT_QUESTION_ORDER, sessionExamId, studentId);
  }

  /**
   * Build student exam content key
   */
  public static String studentExamContent(Long sessionExamId, Long studentId) {
    return String.format(STUDENT_EXAM_CONTENT, sessionExamId, studentId);
  }

  /**
   * Build student status key
   */
  public static String studentStatus(Long sessionExamId, Long studentId) {
    return String.format(STUDENT_STATUS, sessionExamId, studentId);
  }

  /**
   * Build session token key
   */
  public static String sessionToken(Long sessionExamId, Long studentId) {
    return String.format(SESSION_TOKEN, sessionExamId, studentId);
  }

  /**
   * Build session students key
   */
  public static String sessionStudents(Long sessionExamId) {
    return String.format(SESSION_STUDENTS, sessionExamId);
  }

  /**
   * Build session active students key
   */
  public static String sessionActiveStudents(Long sessionExamId, Long studentId) {
    return String.format(SESSION_ACTIVE_STUDENTS, sessionExamId, studentId);
  }

  /**
   * Build violation logs key
   */
  public static String violationLogs(Long sessionExamId, Long studentId) {
    return String.format(VIOLATION_LOGS, sessionExamId, studentId);
  }

  /**
   * Build grading lock key
   */
  public static String gradingLock(Long sessionExamId) {
    return String.format(GRADING_LOCK, sessionExamId);
  }

  /**
   * Build pattern để tìm tất cả keys của exam session
   */
  public static String examSessionAllPattern(Long sessionExamId) {
    return String.format(EXAM_SESSION_ALL_PATTERN, sessionExamId);
  }

  /**
   * Build pattern để tìm tất cả student data
   */
  public static String examSessionStudentsPattern(Long sessionExamId) {
    return String.format(EXAM_SESSION_STUDENTS_PATTERN, sessionExamId);
  }

  /**
   * Build pattern để tìm tất cả violations
   */
  public static String examSessionViolationsPattern(Long sessionExamId) {
    return String.format(EXAM_SESSION_VIOLATIONS_PATTERN, sessionExamId);
  }

  /**
   * Session token with token string
   * exam:session:{sessionExamId}:token:{tokenString}
   */
  public static String examSessionToken(Long sessionExamId, String token) {
    return String.format("exam:session:%s:token:%s", sessionExamId, token);
  }

  public static String examSessionStudentStatus(Long sessionExamId, Long studentId) {
    return "exam:session:" + sessionExamId + ":student:" + studentId + ":status";
  }

  public static String examSessionAnswerKey(Long sessionExamId) {
    return String.format("exam:session:%d:answer_key", sessionExamId);
  }
  // Danh sách sinh viên đủ điều kiện tham gia ca thi
  public static String examSessionEligibleStudents(Long sessionExamId) {
    return String.format("exam:session:%d:eligible_students", sessionExamId);
  }

  // Danh sách sinh viên đã join vào phòng chờ
  public static String examSessionJoinedStudents(Long sessionExamId) {
    return String.format("exam:session:%d:joined_students", sessionExamId);
  }

  public static String examSessionValidation(Long sessionExamId) {
    return String.format("exam:session:%d:validation", sessionExamId);
  }

  public static String studentViolationHistory(Long sessionExamId, Long studentId) {
    return String.format("exam:session:%d:student:%d:violation:history", sessionExamId, studentId);
  }
}
