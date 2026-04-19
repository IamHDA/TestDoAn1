# Hướng Dẫn Thực Hiện Bài Tập Unit Test (Backend)

Tài liệu này tổng hợp các yêu cầu, quy chuẩn và checklist để team thực hiện bài tập Unit Test cho dự án Class System một cách chuẩn xác nhất, đáp ứng các tiêu chí chấm điểm của Giảng viên.

## 1. Yêu cầu Báo cáo Unit Testing (Định dạng Excel)

Báo cáo Unit Test là tài liệu nộp chính thức, bắt buộc bao gồm các sheet hoặc các phần sau:

### 1.1. Tools and Libraries
- **Theo dự án hiện tại:** Yêu cầu liệt kê các công cụ tham gia vào testing.
	- **Framework Test:** JUnit 5 (Spring Boot Test).
	- **Thư viện Mocking:** Mockito.
	- **Công cụ đo độ phủ (Coverage):** JaCoCo.

### 1.2. Scope of Testing (Phạm vi Kiểm thử)
- **Danh sách CẦN Test:** Liệt kê cụ thể tên các hàm, Class, hoặc File đã được viết test (Ví dụ: `AuthServiceImpl`, `ClassroomServiceImpl`, v.v...).
- **Danh sách KHÔNG CẦN Test (kèm lý do):** Cần liệt kê và giải thích (Ví dụ: Các class `Entity` không test vì chỉ là POJO chứa data, không chứa business logic; hoặc các interface `Repository` vì đây là bộ framework đã được dev & QA sẵn bởi cộng đồng mã nguồn mở Spring).

### 1.3. Hệ thống Unit Test Cases 
- Tổ chức tài liệu Excel một cách gọn gàng theo từng **Tên tệp / Tên lớp**.
- Mỗi dòng test case bắt buộc phải có đầy đủ các cột sau:
	- **Test Case ID:** (Ví dụ: `TC_AUTH_01`)
	- **Test Objective:** Mục tiêu bài test (Ví dụ: Kiểm tra đăng nhập với mật khẩu sai).
	- **Input:** Giá trị đầu vào (Mocking data/parameters).
	- **Expected Output:** Kết quả mong muốn (Trạng thái trả về, exception thrown).
	- **Notes:** (Các ghi chú thêm nếu cần).

### 1.4. Project Link
- Cung cấp đường dẫn URL kho lưu trữ GitHub (ví dụ repo `TestDoAn1` của bạn) chứa Unit Test Script.

### 1.5. Execution Report
- Tóm tắt tổng quan: 
	- Tổng số Test Cases.
	- Số Test Pass (Thành công).
	- Số Test Fail (Thất bại).
- **Yêu cầu Bắt buộc:** Cung cấp ảnh chụp màn hình terminal (hoặc IDE) chứng minh được quá trình Test đã thực thi xong (lệnh `mvn test`).

### 1.6. Code Coverage Report
- Tóm tắt kết quả độ bao phủ mã nguồn theo % do hệ thống đo được.
- **Yêu cầu Bắt buộc:** Chụp kèm báo cáo HTML dạng bảng (Report mở từ `target/site/jacoco/index.html`).

### 1.7. Tài liệu Tham khảo & Prompt AI
- Liệt kê các tài liệu giáo trình đã tham khảo.
- Cung cấp theo danh sách các câu Prompt AI đã sử dụng.

---

## 2. Yêu cầu Quy chuẩn về Code Unit Test Scripts

Các mã nguồn (Test Scripts) khi push lên GitHub phải tuân thủ nghiêm ngặt các quy tắc sau:

1. **Comment chi tiết và liên kết ID:**
	- Đảm bảo mã nguồn dễ hiểu bằng việc chèn comment mô tả.
	- **Bắt buộc:** Phía trên mỗi khối test (`@Test`), phải có comment liên kết trực tiếp với **Test Case ID** trong file Excel. (Ví dụ: `// Test case ID: TC_AUTH_01 - Đăng nhập tài khoản sai`).

2. **Quy định Đặt tên (Naming Convention):**
	- Các Test files nên có hậu tố đuôi là `Test` hoặc `Tests` (Ví dụ: `UserServiceTest.java`).
	- Tên biến, hàm mang ý nghĩa rõ ràng (Gợi ý dùng kiểu: `tênHàm_điềuKiện_kếtQuảMongĐợi`, ví dụ: `login_wrongPassword_throwsException()`).

3. **CheckDB & Rollback (Đối với Test có tác động đến Database):**
	- **Phương án nhóm sử dụng:** Dùng `Mockito`. Khi sử dụng Mockito, các tương tác lưu trữ `save()`, `delete()` không hề tác động xuống DB vật lý thực tế.
	- **Rollback:** Cơ chế Mockito chạy 100% trên bộ nhớ ảo giả lập, tự thiết lập và tự huỷ ngay lập tức khi chạy xong test -> Hoàn toàn đáp ứng được tiêu chí "DB quay về trạng thái data như TRƯỚC khi test" mà không cần code các cơ chế rollback phức tạp.
