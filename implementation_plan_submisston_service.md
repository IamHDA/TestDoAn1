# Plan: Mở rộng TestSuite SubmissionServiceImpl

## Phân tích code thực (SubmissionServiceImpl.java)

Có **9 method public** và **3 private helper**. Dựa vào đọc code thực tế, mỗi method có các nhánh (`if/else`, `throws`, loops) rõ ràng cần phải cover:

---

## Method phân tích nhánh (Branch Analysis)

### M1 – `getDetailSubmission(String submissionId)`
| Nhánh | Điều kiện |
|---|---|
| B1 | `hasPermission = false` → throw FORBIDDEN |
| B2 | `hasPermission = true` + `findById` không tồn tại → throw NOT_FOUND |
| B3 ✅ | `hasPermission = true` + tìm thấy submission + có attachment |
| B4 | `hasPermission = true` + tìm thấy submission + **không có** attachment |

### M2 – `getMySubmission(String assignmentId)`
| Nhánh | Điều kiện |
|---|---|
| B1 | `submissionOtp.isEmpty()` → trả về empty response |
| B2 ✅ | Tìm thấy submission + có attachment |
| B3 | Tìm thấy submission + không có attachment |

### M3 – `createDefaultSubmissions(Assignment assignment)`
| Nhánh | Điều kiện |
|---|---|
| B1 | `assignment == null` → return sớm (log error) |
| B2 ✅ | `studentIds` rỗng → loop 0 lần, không save gì |
| B3 | `studentIds` có nhiều SV → save nhiều Submission |

### M4 – `deleteAttachmentInSubmission(String attachmentId)` ← **phức tạp nhất**
| Nhánh | Điều kiện |
|---|---|
| B1 | Attachment không tìm thấy / không thuộc user → throw NOT_FOUND |
| B2 | Submission không tìm thấy → throw NOT_FOUND |
| B3 | Assignment không active/user không có quyền → throw NOT_FOUND |
| B4 | `isLate = true` AND `submissionClosed = true` → throw LATE_SUBMISSION_NOT_ALLOWED |
| B5 | `isLate = true` AND `submissionClosed = false` → **được xóa** |
| B6 | `isLate = false` → bình thường |
| B7 | Sau xóa: `attachments.isEmpty() = true` → reset status NOT_SUBMITTED |
| B8 | Sau xóa: `attachments.isEmpty() = false` + `isLate = true` → set LATE_SUBMITTED |
| B9 ✅ | Sau xóa: `attachments.isEmpty() = false` + `isLate = false` → không đổi status |

### M5 – `addAttachmentToSubmission(String submissionId, SubmissionUpdateRequest)`
| Nhánh | Điều kiện |
|---|---|
| B1 | Submission không tìm thấy → throw NOT_FOUND |
| B2 | Assignment không active → throw NOT_FOUND |
| B3 | `isLate = true` AND `submissionClosed = true` → throw LATE_SUBMISSION_NOT_ALLOWED |
| B4 ✅ | `isLate = false` → set SUBMITTED |
| B5 | `isLate = true` AND `submissionClosed = false` → set LATE_SUBMITTED |
| B6 | List attachment rỗ → vẫn save submission (B4/B5 vẫn apply) |

### M6 – `searchSubmission(BaseFilterSearchRequest<SubmissionSearchRequest>)`
| Nhánh | Điều kiện |
|---|---|
| B1 | `canUserViewSubmissions = false` → throw FORBIDDEN |
| B2 ✅ | Có kết quả → trả về page có data |
| B3 | Không có kết quả → trả về page rỗng |

### M7 – `markSubmission(String submissionId, SubmissionGradeUpdateRequest)`
| Nhánh | Điều kiện |
|---|---|
| B1 | `hasPermission = false` → throw FORBIDDEN |
| B2 | `hasPermission = true` + submission không tồn tại → throw NOT_FOUND |
| B3 ✅ | `hasPermission = true` + tìm thấy → update grade, set GRADED |

### M8 – `downloadAllSubmissions(String assignmentId)`
| Nhánh | Điều kiện |
|---|---|
| B1 | `canUserViewSubmissions = false` → throw FORBIDDEN |
| B2 | Submissions rỗng → throw NOT_FOUND |
| B3 | Có submission nhưng **không có attachment nào** → `hasAnyFile=false` → throw NOT_FOUND |
| B4 | `fileName == null` (attachment null url) → `continue` (skip) |
| B5 | IOException khi đọc file → throw FILE_DOWNLOAD_FAILED |
| B6 ✅ | Bình thường → trả về ByteArrayResource |

### M9 – `downloadGradeTemplate(String assignmentId)`
| Nhánh | Điều kiện |
|---|---|
| B1 | `canUserViewSubmissions = false` → throw FORBIDDEN |
| B2 ✅ | `data` rỗng → vẫn trả về excel rỗng (không throw) |
| B3 | Có data → trả về excel có dòng |

### M10 – `importSubmissionScoresFromExcel(String assignmentId, MultipartFile file)`
| Nhánh | Điều kiện |
|---|---|
| B1 | `canUserViewSubmissions = false` → throw FORBIDDEN |
| B2 | `parseSubmissionExcel` throw IOException → throw FILE_UPLOAD_FAILED |
| B3 | Một dòng excel: `row == null` → `continue` (skip) |
| B4 | `username` hoặc `code` blank → throw IMPORT_MISSING_STUDENT_INFO |
| B5 | Grade cell rỗng → grade = null |
| B6 | Grade cell có giá trị nhưng sai format số → throw IMPORT_INVALID_GRADE_FORMAT |
| B7 | `submissionMap.get(dto.getCode()) == null` → throw IMPORT_SUBMISSION_NOT_FOUND |
| B8 | `dto.getGrade() != null` + grade ≠ oldGrade → cập nhật `gradedAt` = now |
| B9 | `dto.getGrade() != null` + grade = oldGrade → `gradedAt` = giữ nguyên |
| B10 ✅ | Happy path: tất cả hợp lệ → saveAll |

---

## Danh sách Test Case Đề xuất

> **Quy tắc đặt tên**: `TC_QLBT_SSV{nhóm}_{seq}` (SSV = SubmissionService)
> - SSV1 = getDetailSubmission
> - SSV2 = getMySubmission
> - SSV3 = createDefaultSubmissions
> - SSV4 = deleteAttachmentInSubmission
> - SSV5 = addAttachmentToSubmission
> - SSV6 = searchSubmission
> - SSV7 = markSubmission
> - SSV8 = downloadAllSubmissions
> - SSV9 = downloadGradeTemplate
> - SSV10 = importSubmissionScoresFromExcel

---

### GROUP 1 – getDetailSubmission (4 TCs)

| ID | Tên Test | Input | Expected | Nhánh |
|---|---|---|---|---|
| TC_QLBT_SSV1_001 | Get detail - No permission | hasPermission=false | throw AppException FORBIDDEN | B1 |
| TC_QLBT_SSV1_002 | Get detail - Submission not found | hasPermission=true, findById=empty | throw AppException NOT_FOUND | B2 |
| TC_QLBT_SSV1_003 | Get detail - Success with attachments | hasPermission=true, found, 2 attachments | response.attachmentList.size=2 | B3 |
| TC_QLBT_SSV1_004 | Get detail - Success with no attachments | hasPermission=true, found, 0 attachments | response.attachmentList.size=0 | B4 |

---

### GROUP 2 – getMySubmission (3 TCs)

| ID | Tên Test | Input | Expected | Nhánh |
|---|---|---|---|---|
| TC_QLBT_SSV2_001 | GetMy - Submission not found → empty response | findByAssignmentIdAndStudentId=empty | return empty builder response | B1 |
| TC_QLBT_SSV2_002 | GetMy - Found with attachments | submission found, 1 attachment | response not null, attachments.size=1 | B2 |
| TC_QLBT_SSV2_003 | GetMy - Found with no attachments | submission found, 0 attachments | response not null, attachmentList empty | B3 |

---

### GROUP 3 – createDefaultSubmissions (3 TCs)

| ID | Tên Test | Input | Expected | Nhánh |
|---|---|---|---|---|
| TC_QLBT_SSV3_001 | Create defaults - Null assignment → early return | assignment=null | return; không save | B1 |
| TC_QLBT_SSV3_002 | Create defaults - Empty class (no students) | studentIds=empty Set | save gọi 0 lần | B2 |
| TC_QLBT_SSV3_003 | Create defaults - 3 students → 3 submissions | studentIds={1L,2L,3L} | save gọi 3 lần | B3 |

---

### GROUP 4 – deleteAttachmentInSubmission (9 TCs)

| ID | Tên Test | Input | Expected | Nhánh |
|---|---|---|---|---|
| TC_QLBT_SSV4_001 | Delete attach - Attachment not found | findByAttachmentIdAndUploadedByAndIsDeletedEquals=empty | throw NOT_FOUND | B1 |
| TC_QLBT_SSV4_002 | Delete attach - Submission not found | attachment found, findBySubmissionIdAndStudentId=empty | throw NOT_FOUND | B2 |
| TC_QLBT_SSV4_003 | Delete attach - Assignment not active (student not eligible) | findAssignmentIfUserCanSubmit=empty | throw NOT_FOUND | B3 |
| TC_QLBT_SSV4_004 | Delete attach - Late + SubmissionClosed → throw | isLate=true, closed=true | throw LATE_SUBMISSION_NOT_ALLOWED | B4 |
| TC_QLBT_SSV4_005 | Delete attach - Late + NOT Closed → success | isLate=true, closed=false | delete OK, set LATE_SUBMITTED | B5+B8 |
| TC_QLBT_SSV4_006 | Delete attach - On time → success | isLate=false | delete OK, no status change | B6+B9 |
| TC_QLBT_SSV4_007 | Delete attach - After delete, no more files → NOT_SUBMITTED | isLate=false, after delete attachments=empty | submission.status=NOT_SUBMITTED, submittedAt=null | B7 |
| TC_QLBT_SSV4_008 | Delete attach - After delete, still has files, isLate=true → LATE_SUBMITTED | isLate=true, after delete attachments>0 | submission.status=LATE_SUBMITTED | B8 |
| TC_QLBT_SSV4_009 | Delete attach - After delete, still has files, on time → no status change | isLate=false, after delete >0 files | status unchanged | B9 |

---

### GROUP 5 – addAttachmentToSubmission (6 TCs)

| ID | Tên Test | Input | Expected | Nhánh |
|---|---|---|---|---|
| TC_QLBT_SSV5_001 | Add attach - Submission not found | findBySubmissionIdAndStudentId=empty | throw NOT_FOUND | B1 |
| TC_QLBT_SSV5_002 | Add attach - Assignment not active | submission found, findAssignmentIfUserCanSubmit=empty | throw NOT_FOUND | B2 |
| TC_QLBT_SSV5_003 | Add attach - Late + Closed → throw | isLate=true, closed=true | throw LATE_SUBMISSION_NOT_ALLOWED | B3 |
| TC_QLBT_SSV5_004 | Add attach - Success on time → SUBMITTED | isLate=false, 1 file | status=SUBMITTED, 1 attachment saved | B4 |
| TC_QLBT_SSV5_005 | Add attach - Late + NOT Closed → LATE_SUBMITTED | isLate=true, closed=false, 1 file | status=LATE_SUBMITTED | B5 |
| TC_QLBT_SSV5_006 | Add attach - Empty file list, on time → SUBMITTED (no attach saved) | isLate=false, fileList=[] | status=SUBMITTED, save(attachment) never called | B6 |

---

### GROUP 6 – searchSubmission (3 TCs)

| ID | Tên Test | Input | Expected | Nhánh |
|---|---|---|---|---|
| TC_QLBT_SSV6_001 | Search - No permission → throw FORBIDDEN | canUserViewSubmissions=false | throw FORBIDDEN | B1 |
| TC_QLBT_SSV6_002 | Search - Success with results | canView=true, results=[dto1,dto2] | response.content.size=2 | B2 |
| TC_QLBT_SSV6_003 | Search - Success no results | canView=true, results=[] | response.content.size=0 | B3 |

---

### GROUP 7 – markSubmission (3 TCs)

| ID | Tên Test | Input | Expected | Nhánh |
|---|---|---|---|---|
| TC_QLBT_SSV7_001 | Mark - No permission → throw FORBIDDEN | hasPermission=false | throw FORBIDDEN | B1 |
| TC_QLBT_SSV7_002 | Mark - Submission not found | hasPermission=true, findById=empty | throw NOT_FOUND | B2 |
| TC_QLBT_SSV7_003 | Mark - Success → grade updated, status=GRADED | hasPermission=true, found | submission.grade=set, gradingStatus=GRADED | B3 |

---

### GROUP 8 – downloadAllSubmissions (6 TCs)

| ID | Tên Test | Input | Expected | Nhánh |
|---|---|---|---|---|
| TC_QLBT_SSV8_001 | Download ZIP - No permission → throw FORBIDDEN | canView=false | throw FORBIDDEN | B1 |
| TC_QLBT_SSV8_002 | Download ZIP - No submissions | submissions=empty | throw NOT_FOUND | B2 |
| TC_QLBT_SSV8_003 | Download ZIP - Submissions exist but no attachments | submissions=[sub], attachments=[] | throw NOT_FOUND (hasAnyFile=false) | B3 |
| TC_QLBT_SSV8_004 | Download ZIP - Attachment has null fileName (bad URL) → skip+continue | fileName=null | skip, `hasAnyFile` stays false → throw NOT_FOUND | B4 |
| TC_QLBT_SSV8_005 | Download ZIP - IOException on file read → throw FILE_DOWNLOAD_FAILED | fileService throws IOException | throw AppException FILE_DOWNLOAD_FAILED | B5 |
| TC_QLBT_SSV8_006 | Download ZIP - Success | 1 sub + 1 attach, valid file | returns ByteArrayResource | B6 |

---

### GROUP 9 – downloadGradeTemplate (3 TCs)

| ID | Tên Test | Input | Expected | Nhánh |
|---|---|---|---|---|
| TC_QLBT_SSV9_001 | Template - No permission → throw FORBIDDEN | canView=false | throw FORBIDDEN | B1 |
| TC_QLBT_SSV9_002 | Template - Empty data → excel rỗng (not throw) | data=[] | returns ByteArrayResource (empty excel) | B2 |
| TC_QLBT_SSV9_003 | Template - Has data → excel có dòng | data=[dto1,dto2] | returns ByteArrayResource | B3 |

---

### GROUP 10 – importSubmissionScoresFromExcel (10 TCs)

| ID | Tên Test | Input | Expected | Nhánh |
|---|---|---|---|---|
| TC_QLBT_SSV10_001 | Import - No permission → throw FORBIDDEN | canView=false | throw FORBIDDEN | B1 |
| TC_QLBT_SSV10_002 | Import - File IOException → throw FILE_UPLOAD_FAILED | file throws IOException | throw FILE_UPLOAD_FAILED | B2 |
| TC_QLBT_SSV10_003 | Import - Row null → skip row (continue) | row=null at index 1 | no error, result empty | B3 |
| TC_QLBT_SSV10_004 | Import - Username blank → throw IMPORT_MISSING_STUDENT_INFO | username="" | throw IMPORT_MISSING_STUDENT_INFO | B4 |
| TC_QLBT_SSV10_005 | Import - Code blank → throw IMPORT_MISSING_STUDENT_INFO | code="" | throw IMPORT_MISSING_STUDENT_INFO | B4 |
| TC_QLBT_SSV10_006 | Import - Grade cell empty → grade=null, no gradingStatus | gradeStr="" | grade field null, no grade | B5 |
| TC_QLBT_SSV10_007 | Import - Grade not a number → throw IMPORT_INVALID_GRADE_FORMAT | gradeStr="abc" | throw IMPORT_INVALID_GRADE_FORMAT | B6 |
| TC_QLBT_SSV10_008 | Import - Student code not in system → throw IMPORT_SUBMISSION_NOT_FOUND | submissionMap no key | throw IMPORT_SUBMISSION_NOT_FOUND | B7 |
| TC_QLBT_SSV10_009 | Import - Grade changed → gradedAt updated to now | dto.grade ≠ old grade | gradedAt = LocalDateTime.now() | B8 |
| TC_QLBT_SSV10_010 | Import - Grade same as before → gradedAt unchanged | dto.grade == old grade | gradedAt = original | B9 |
| TC_QLBT_SSV10_011 | Import - Happy path (valid Excel) → saveAll | all valid | saveAll called, all grades updated | B10 |

---

## Tổng kết

| Service Method | Số branch | Số TC đề xuất |
|---|---|---|
| getDetailSubmission | 4 | 4 |
| getMySubmission | 3 | 3 |
| createDefaultSubmissions | 3 | 3 |
| deleteAttachmentInSubmission | 9 | 9 |
| addAttachmentToSubmission | 6 | 6 |
| searchSubmission | 3 | 3 |
| markSubmission | 3 | 3 |
| downloadAllSubmissions | 6 | 6 |
| downloadGradeTemplate | 3 | 3 |
| importSubmissionScoresFromExcel | 10 | 11 |
| **TỔNG** | **50 nhánh** | **51 TC** |

> [!NOTE]
> File test cũ (`SubmissionServiceImplTest.java`) hiện chỉ có **9 TC**, cover rất ít branch. Plan này đề xuất **51 TC** bám sát từng `if/else/try-catch` trong code thực tế để tăng Branch Coverage lên đáng kể.

> [!IMPORTANT]
> Một số TC phức tạp (SSV8_005, SSV8_006, SSV10 series) cần mock `MultipartFile` và `WorkbookFactory` – cần dùng `@MockitoBean` hoặc PowerMock. Nên thảo luận với Claude về cách mock `ExcelUtils.createWorkbook(file)` vì đây là static call.

> [!WARNING]
> `deleteAttachmentInSubmission` (GROUP 4) là method phức tạp nhất với 9 nhánh, cần setup mock cẩn thận: `isLate` phụ thuộc vào `dueDate` của Assignment + `DateUtils.isAfter()` – nên set `dueDate` trong quá khứ để `isLate=true`, tương lai để `isLate=false`.
