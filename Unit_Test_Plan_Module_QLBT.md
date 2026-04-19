# CHIẾN LƯỢC UNIT TEST CHI TIẾT - MODULE QUẢN LÝ BÀI TẬP VÀ NỘP BÀI (QLBT)

*(Phiên bản rà soát nội bộ - Đã khớp 100% với các hàm thực tế trong Repository)*

---

## PHẦN I: `AssignmentServiceImpl` (~35 Test Cases)

### 1. Hàm `createAssignment`
1. **TC_QLBT_SV1_001**: [Happy Path] Tạo bài tập thành công với đầy đủ tham số hợp lệ. `saveAndFlush` trả về ID.
2. **TC_QLBT_SV1_002**: [Null Check] Bỏ trống `title` (nếu service validate) -> Ném Exception.
3. **TC_QLBT_SV1_003**: [DB] `classroomId` không tồn tại -> `ClassroomNotFoundException`.
4. **TC_QLBT_SV1_004**: [Auth] Gọi tạo bài tập khi userID không phải là `teacherId` của lớp -> `ForbiddenException`.
5. **TC_QLBT_SV1_005**: [BVA] `maxScore` < 0 -> Logic chặn điểm âm.
6. **TC_QLBT_SV1_006**: [BVA] `maxScore` > 1000 (biên trên) -> Ngoại lệ.
7. **TC_QLBT_SV1_007**: [BVA] `maxScore` = 0 -> Biên hợp lệ, pass.
8. **TC_QLBT_SV1_008**: [Time/Logic] `startDate` > `endDate` -> Khởi tạo lỗi thời gian ngược.
9. **TC_QLBT_SV1_009**: [Time] `endDate` là ngày trong quá khứ -> Lỗi Validation.
10. **TC_QLBT_SV1_010**: [Default] Trường `allowLateSubmission` bị bỏ null -> Kiểm tra service gán mặc định thành gá trị an toàn (false).
11. **TC_QLBT_SV1_011**: [Integration] Phải trigger Event/Function `createDefaultSubmissions` cho sinh viên sau khi tạo thành công.

### 2. Hàm `updateAssignment`
12. **TC_QLBT_SV2_001**: [Happy Path] Update title và content thành công.
13. **TC_QLBT_SV2_002**: [DB] Update bài tập không tồn tại (`findByAssignmentIdAndNotDeleted` rỗng) -> `NotFound`.
14. **TC_QLBT_SV2_003**: [Auth] Gọi update khi không phải teacher sở hữu lớp -> `Forbidden`.
15. **TC_QLBT_SV2_004**: [Time] Đổi `endDate` sang mức thời gian LỚN HƠN thời gian cũ -> Pass. 
16. **TC_QLBT_SV2_005**: [Time] Rút ngắn `endDate` về mức thời gian quá khứ (so với hiện tại) -> Exception.
17. **TC_QLBT_SV2_006**: [Logic] Cập nhật cờ `allowLateSubmission` từ True -> False khi đã có sinh viên nộp bài -> Cập nhật cấu hình mới, không crash.
18. **TC_QLBT_SV2_007**: [Logic] Cập nhật `maxScore` bé hơn điểm mà sinh viên ĐÃ ĐƯỢC CHẤM. -> Exception ngăn chặn logic hạch toán.
19. **TC_QLBT_SV2_008**: [Null Check] Cố tình gán title mới = null -> Ném lỗi.

### 3. Hàm `softDeleteAssignment`
20. **TC_QLBT_SV3_001**: [Happy Path] Xóa bài chưa có ai nộp -> Biến `isDeleted` = true, cập nhật Announcement liên quan.
21. **TC_QLBT_SV3_002**: [DB] `assignmentId` không có -> `NotFound`.
22. **TC_QLBT_SV3_003**: [Auth] Quyền Student cố xóa bài -> `Forbidden`.
23. **TC_QLBT_SV3_004**: [Logic] Xóa bài ĐÃ CÓ sinh viên nộp / đã chấm điểm -> Hệ thống chặn xóa (Throw Exception) hoặc xử lý cascading.

### 4. Hàm `getAssignmentDetail`
24. **TC_QLBT_SV4_001**: [Happy Path] Trả về DTO cho Teacher.
25. **TC_QLBT_SV4_002**: [Happy Path] Trả về DTO cho Student hợp lệ trong lớp.
26. **TC_QLBT_SV4_003**: [Auth] Student ngoài lớp (`existsByClassroomIdAndUserIdAnd...` = False) cố GET detail bằng URL ID -> ném `Forbidden`.
27. **TC_QLBT_SV4_004**: [DB] Bài tập `isDeleted = true` -> Không truy cập được.

### 5. Hàm `getAssignmentList`
28. **TC_QLBT_SV5_001**: Lấy danh sách thành công có trang.
29. **TC_QLBT_SV5_002**: Filter/Sort theo thuộc tính (Sort direction).
30. **TC_QLBT_SV5_003**: Xem DS ở `classroomId` không hợp lệ -> `NotFound`.
31. **TC_QLBT_SV5_004**: Lớp học không có bài tập -> Trả về pageable rỗng (List rỗng, nhưng KHÔNG throw error).

### 6. Hàm `addAssignee` & Thống kê
32. **TC_QLBT_SV6_001**: `addAssignee` thành công với userId hợp lệ.
33. **TC_QLBT_SV6_002**: `addAssignee` - User ID không phải là Student trong lớp này -> `Forbidden` / Error.
34. **TC_QLBT_SV7_001**: Thống kê - `getAverageScoreComparison` với dữ liệu thực tế -> Pass.
35. **TC_QLBT_SV7_002**: Thống kê lớp KHÔNG có bài tập nộp nào -> Trả về mảng 0, không chia cho 0 `div by zero`.
36. **TC_QLBT_SV7_003**: Thống kê trend `getImprovementTrend` -> Trả list valid.


---

## PHẦN II: `SubmissionServiceImpl` (~35 Test Cases)

### 7. Hàm `getDetailSubmission` & `getMySubmission`
37. **TC_QLBT_SV8_001**: Lấy detail thành công (Quyền duyệt `hasPermission` = True).
38. **TC_QLBT_SV8_002**: Lấy detail bị cấm (Hacker ID 10 xem bài của ID 12).
39. **TC_QLBT_SV8_003**: Xem Submission ID khi nó đã bị xoá vật lý hoặc không có trong DB.
40. **TC_QLBT_SV8_004**: Lấy bài tập chính mình (`getMySubmission`), chưa nộp bài -> Vẫn ra status `NOT_SUBMITTED` chứ không `null`.

### 8. Hàm `createDefaultSubmissions`
41. **TC_QLBT_SV9_001**: Lớp có 10 học sinh -> `submissionRepository.save()` chạy lặp đúng 10 records.
42. **TC_QLBT_SV9_002**: Lớp 0 học sinh -> Hàm skip nhanh chóng không lỗi vặt.

### 9. Hàm `addAttachmentToSubmission` (Nộp Bài)
43. **TC_QLBT_SV10_001**: [Happy] Trước deadline -> Lưu attachment, đổi Status thành `SUBMITTED`.
44. **TC_QLBT_SV10_002**: [Time] Hạn qua, `allowLateSubmission=True` -> Chạy thành công nhưng status `LATE_SUBMITTED`.
45. **TC_QLBT_SV10_003**: [Time] Hạn qua, `allowLateSubmission=False` -> Throw Exception ngay lập tức.
46. **TC_QLBT_SV10_004**: [Re-submit] Nộp lại file thứ 2 trong hạn -> Cập nhật thành công, có file ghi đè.
47. **TC_QLBT_SV10_005**: [Validation] Tải file size = 0 byte (Tên file vẫn có) -> Chặn.
48. **TC_QLBT_SV10_006**: [Validation] List URL Attachment truyền rỗng (Không upload file nào nhưng bấm lưu) -> Throw exception.
49. **TC_QLBT_SV10_007**: [Logic/State] Cố gắng đính kèm file VÀO BÀI ĐÃ CHUYỂN TRẠNG THÁI CHẤM (`GradingStatus.GRADED`) -> Chặn bắt lỗi "Đã có điểm, cấm sửa".
50. **TC_QLBT_SV10_008**: [DB] `studentId` gọi update nhưng `findBySubmissionIdAndStudentId` không có -> Không có quyền với Submission này.

### 10. Hàm `markSubmission` (Chấm Điểm)
51. **TC_QLBT_SV11_001**: [Happy] Điểm nguyên `8.0` cho bài đã nộp -> `GradingStatus = GRADED`.
52. **TC_QLBT_SV11_002**: [Happy] Điểm thập phân `8.5` -> Pass.
53. **TC_QLBT_SV11_003**: [BVA] Biên dưới `grade = 0.0` -> Nhận dạng bình thường (không nhầm với Null).
54. **TC_QLBT_SV11_004**: [BVA] Biên trên `grade = maxScore` (Ví dụ 10.0) -> Pass.
55. **TC_QLBT_SV11_005**: [BVA/Validation] `grade > maxScore` -> Exception "Điểm quá giới hạn bài!".
56. **TC_QLBT_SV11_006**: [BVA/Validation] `grade < 0` -> Điểm âm bị throw Exception.
57. **TC_QLBT_SV11_007**: [State/Logic] Chấm điểm cho hs CÓ STATUS `NOT_SUBMITTED` -> Không cho phép chấm khống nếu sinh viên chưa nộp bài. *(Cover lỗi System)*.
58. **TC_QLBT_SV11_008**: Cập nhật lại điểm MỚI (từ 5đ lên 9đ) cho sinh viên -> DB được đè thành công.
59. **TC_QLBT_SV11_009**: [Auth] Giáo viên của lóp khác nhưng biết ID URL, mưu đồ chấm điểm hộ -> `hasPermission(submissionId, userId)` trả về FALSE -> `Forbidden`.
60. **TC_QLBT_SV11_010**: Text chứa ký tự lạ `grade = "abc"` -> Parse lỗi (Nếu xử lý ở service thì ném Exception Parse Float).

### 11. Hàm `deleteAttachmentInSubmission` (Thu Hồi Bài / Xoá File)
61. **TC_QLBT_SV12_001**: Thu hồi tốt khi vẫn còn hạn nộp, xoá vật lý link file, trả status về `NOT_SUBMITTED`.
62. **TC_QLBT_SV12_002**: [Time] Đã đóng cửa Hạn nộp & Cấm late submission -> Bấm xoá -> Throw Exception "Không xoá được vì đã chốt sổ".
63. **TC_QLBT_SV12_003**: [Logic] Bấm xoá bài TẠI THỜI ĐIỂM BA MẸ ĐÃ XEM ĐIỂM (`GRADED`) -> Chặn thu hồi tệp.
64. **TC_QLBT_SV12_004**: Cố gắng xoá file Attachment không thuộc về Submission của Sinh viên đang gọi.

### 12. Các Hàm Xuất Report / Excel (`downloadAllSubmissions`, `importSubmissionScoresFromExcel`, v.v..)
65. **TC_QLBT_SV13_001**: Hàm zip tất cả file của cả lớp `downloadAllSubmissions` -> Pass, return `Resource`.
66. **TC_QLBT_SV13_002**: Tải ZIP nhưng `assignmentId` không có -> `NotFound`.
67. **TC_QLBT_SV13_003**: Hàm kiểm tra quyền `canUserViewSubmissions` trả về FALSE (Hacker) -> `Forbidden` tải ZIP.
68. **TC_QLBT_SV13_004**: Tải template chấm điểm `downloadGradeTemplate` cho lớp -> Có xuất đủ file với tiêu đề học sinh, kể cả học sinh có status là NOT_SUBMITTED.
69. **TC_QLBT_SV13_005**: Import Excel Điểm - Format Excel rỗng -> Exception "File is empty".
70. **TC_QLBT_SV13_006**: Import Excel Điểm - Parse điểm số cho một số học sinh bị quá maxScore -> Exception báo từng dòng bị lỗi.
71. **TC_QLBT_SV13_007**: Import excel điểm nguyên dương hợp lệ, test update batch thành công.
