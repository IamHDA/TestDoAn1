# Hướng Dẫn Chạy JMeter và Selenium Tests

## Cấu trúc thư mục sau khi tích hợp

```
src/test/
├── java/com/vn/backend/
│   ├── selenium/
│   │   ├── SeleniumBaseTest.java          # Base class (WebDriver setup/teardown)
│   │   ├── SwaggerUISeleniumTest.java     # Test Swagger UI (5 test cases)
│   │   ├── LoginSeleniumTest.java         # Test luồng đăng nhập (5 test cases)
│   │   └── UserManagementSeleniumTest.java # Test User Management API (6 test cases)
│   └── (các unit test hiện có...)
└── jmeter/
    ├── 01_login_500users.jmx              # 500 users đăng nhập đồng thời
    ├── 02_load_increase_threshold.jmx     # Tăng dần 500→750→1000→1250→1500 tìm ngưỡng
    └── 03_session_exam_workflow_500users.jmx # Workflow thi cử 500 users
```

---

## 1. Chạy Selenium Tests

### Yêu cầu
- **Google Chrome** đã cài đặt (WebDriverManager tự tải ChromeDriver phù hợp)
- Backend server **đang chạy** tại `http://localhost:8080`
- Maven

### Lệnh chạy

```bash
# Chạy tất cả Selenium tests (Headless mode - mặc định, phù hợp CI/CD)
mvn test -Dtest="*SeleniumTest"

# Chạy với GUI (mở trình duyệt Chrome để quan sát) - dùng khi debug local
mvn test -Dtest="*SeleniumTest" -Dselenium.headless=false

# Chạy riêng từng class test
mvn test -Dtest="SwaggerUISeleniumTest"
mvn test -Dtest="LoginSeleniumTest"
mvn test -Dtest="UserManagementSeleniumTest"

# Chạy với URL backend khác (ví dụ staging)
mvn test -Dtest="*SeleniumTest" -Dapp.base.url=http://192.168.1.100:8080
```

### Danh sách test cases Selenium (Tổng: 91 test cases)

| Class | Test Case | Mô tả |
|-------|-----------|-------|
| `SwaggerUISeleniumTest` (5 TCs) | TC_SWAGGER_01 | Swagger UI tải thành công |
| | TC_SWAGGER_02 | Swagger UI hiển thị API info header |
| | TC_SWAGGER_03 | Swagger UI hiển thị các API tag groups (>= 5 groups) |
| | TC_SWAGGER_04 | Nút Authorize (JWT) hiển thị |
| | TC_SWAGGER_05 | Có thể expand Authentication section |
| `LoginSeleniumTest` (5 TCs) | TC_LOGIN_01 | Tìm thấy Login endpoint trong Swagger |
| | TC_LOGIN_02 | Expand Auth section - login operation visible |
| | TC_LOGIN_03 | Authorize button mở dialog nhập token |
| | TC_LOGIN_04 | Try it out button hoạt động trên Login |
| | TC_LOGIN_05 | Swagger hiển thị đầy đủ GET/POST/PUT/DELETE |
| `UserManagementSeleniumTest` (6 TCs) | TC_USER_01 | User Management group xuất hiện |
| | TC_USER_02 | POST /api/users/create hiển thị |
| | TC_USER_03 | PUT /{id}/status hiển thị |
| | TC_USER_04 | POST /import hiển thị |
| | TC_USER_05 | GET /download/import-template hiển thị |
| | TC_USER_06 | User module có >= 5 endpoints |
| `SubjectManagementSeleniumTest` (8 TCs) | TC_SUBJECT_01 | Swagger có ít nhất 1 tag group |
| | TC_SUBJECT_02 | POST /api/subjects/create hiển thị |
| | TC_SUBJECT_03 | POST /api/subjects/search hiển thị |
| | TC_SUBJECT_04 | PUT /api/subjects/{id}/update hiển thị |
| | TC_SUBJECT_05 | DELETE /api/subjects/{id} hiển thị |
| | TC_SUBJECT_06 | Subject module có >= 4 endpoints (CRUD) |
| | TC_SUBJECT_07 | Create subject operation có thể expand để xem schema |
| | TC_SUBJECT_08 | Swagger render xong trong 20 giây |
| `TopicManagementSeleniumTest` (9 TCs) | TC_TOPIC_01 | Topic tag group tồn tại |
| | TC_TOPIC_02 | POST /api/topics/create hiển thị |
| | TC_TOPIC_03 | POST /api/topics/search hiển thị |
| | TC_TOPIC_04 | PUT /api/topics/{id}/update hiển thị |
| | TC_TOPIC_05 | DELETE /api/topics/{id} hiển thị |
| | TC_TOPIC_06 | GET /api/topics/{id}/practice-set hiển thị |
| | TC_TOPIC_07 | Topic module có >= 5 endpoints |
| | TC_TOPIC_08 | Create topic operation expandable |
| | TC_TOPIC_09 | Search topic operation expandable |
| `ClassroomManagementSeleniumTest` (10 TCs) | TC_CLASS_01 | POST /api/classrooms/create hiển thị |
| | TC_CLASS_02 | POST /api/classrooms/search hiển thị |
| | TC_CLASS_03 | GET /api/classrooms/detail/{id} hiển thị |
| | TC_CLASS_04 | GET /api/classrooms/{id}/header hiển thị |
| | TC_CLASS_05 | PUT /api/classrooms/update/{id} hiển thị |
| | TC_CLASS_06 | PUT /api/classrooms/reset-class-code/{id} hiển thị |
| | TC_CLASS_07 | GET /api/classrooms/setting/{id}/detail hiển thị |
| | TC_CLASS_08 | POST /api/classrooms/member/search hiển thị |
| | TC_CLASS_09 | PUT /api/classrooms/member/{id}/update-status hiển thị |
| | TC_CLASS_10 | Classroom module có >= 9 endpoints |
| `ExamManagementSeleniumTest` (10 TCs) | TC_EXAM_01 | POST /api/exams/create hiển thị |
| | TC_EXAM_02 | POST /api/exams/search hiển thị |
| | TC_EXAM_03 | GET /api/exams/{id} (chi tiết) hiển thị |
| | TC_EXAM_04 | PUT /api/exams/{id} (cập nhật) hiển thị |
| | TC_EXAM_05 | DELETE /api/exams/{id} hiển thị |
| | TC_EXAM_06 | POST /api/exams/{id}/questions/create hiển thị |
| | TC_EXAM_07 | POST /api/exams/{id}/duplicate hiển thị |
| | TC_EXAM_08 | GET /api/exams/{id}/statistic hiển thị |
| | TC_EXAM_09 | POST /api/exams/available-questions/search hiển thị |
| | TC_EXAM_10 | Exam module có >= 9 endpoints |
| `QuestionManagementSeleniumTest` (10 TCs) | TC_QUESTION_01 | POST /api/questions/create hiển thị |
| | TC_QUESTION_02 | POST /api/questions/bulk-create hiển thị |
| | TC_QUESTION_03 | POST /api/questions/search hiển thị |
| | TC_QUESTION_04 | GET /api/questions/detail/{id} hiển thị |
| | TC_QUESTION_05 | PUT /api/questions/update/{id} hiển thị |
| | TC_QUESTION_06 | DELETE /api/questions/delete/{id} hiển thị |
| | TC_QUESTION_07 | POST /api/questions/import-excel hiển thị |
| | TC_QUESTION_08 | POST /api/questions/export-excel hiển thị |
| | TC_QUESTION_09 | GET /api/questions/import-template hiển thị |
| | TC_QUESTION_10 | Question module có >= 9 endpoints |
| `AssignmentManagementSeleniumTest` (10 TCs) | TC_ASSIGN_01 | Assignment tag group tồn tại |
| | TC_ASSIGN_02 | POST /api/assignments/create/{classroomId} hiển thị |
| | TC_ASSIGN_03 | GET /api/assignments/detail/{id} hiển thị |
| | TC_ASSIGN_04 | PUT /api/assignments/update/{id} hiển thị |
| | TC_ASSIGN_05 | POST /api/assignments/delete/{id} (soft) hiển thị |
| | TC_ASSIGN_06 | POST /api/assignments/list/{classroomId} hiển thị |
| | TC_ASSIGN_07 | PUT /api/assignments/add-assignee/{id} hiển thị |
| | TC_ASSIGN_08 | GET /api/assignments/{id}/statistics hiển thị |
| | TC_ASSIGN_09 | GET /api/assignments/{id}/improvement-trend hiển thị |
| | TC_ASSIGN_10 | Assignment module có >= 9 endpoints |
| `SessionExamSeleniumTest` (14 TCs) | TC_SESSION_01 | POST /api/session-exams/create hiển thị |
| | TC_SESSION_02 | PUT /api/session-exams/update/{id} hiển thị |
| | TC_SESSION_03 | GET /api/session-exams/detail/{id} hiển thị |
| | TC_SESSION_04 | DELETE /api/session-exams/delete/{id} hiển thị |
| | TC_SESSION_05 | POST /api/session-exams/teacher/search hiển thị |
| | TC_SESSION_06 | POST /api/session-exams/student/search hiển thị |
| | TC_SESSION_07 | POST /api/session-exams/{id}/join hiển thị |
| | TC_SESSION_08 | GET /api/session-exams/{id}/download hiển thị |
| | TC_SESSION_09 | POST /api/session-exams/{id}/submit hiển thị |
| | TC_SESSION_10 | GET /api/session-exams/{id}/monitoring hiển thị |
| | TC_SESSION_11 | GET /api/session-exams/{id}/descriptive-statistic hiển thị |
| | TC_SESSION_12 | GET /api/session-exams/result/{id} hiển thị |
| | TC_SESSION_13 | Session Exam module có >= 12 endpoints |
| | TC_SESSION_14 | POST /api/session-exams/{id}/save hiển thị |
| `ApiSecuritySeleniumTest` (12 TCs) | TC_SEC_01 | Swagger UI không cần auth để xem |
| | TC_SEC_02 | JWT Bearer security scheme hiển thị |
| | TC_SEC_03 | Tổng số API operations >= 50 |
| | TC_SEC_04 | Số POST operations >= 15 |
| | TC_SEC_05 | Số GET operations >= 10 |
| | TC_SEC_06 | Số PUT operations >= 5 |
| | TC_SEC_07 | Số DELETE operations >= 3 |
| | TC_SEC_08 | Page title không blank |
| | TC_SEC_09 | UI render đúng ở zoom 80% |
| | TC_SEC_10 | Trang có thể cuộn (tất cả API hiển thị) |
| | TC_SEC_11 | Swagger có info block với content |
| | TC_SEC_12 | Actuator health endpoint kiểm tra server UP |

---

## 2. Chạy JMeter Tests

### Yêu cầu
- Backend server **đang chạy** tại `http://localhost:8080`
- Có user `admin` / `admin123` trong database (hoặc sửa biến trong file `.jmx`)
- Maven (jmeter-maven-plugin sẽ tự tải JMeter)

### Lệnh chạy qua Maven

```bash
# Chạy TẤT CẢ file .jmx trong src/test/jmeter/
mvn jmeter:jmeter

# Chạy rồi generate HTML report (cần JMeter cài sẵn để generate report)
mvn jmeter:jmeter jmeter:results
```

### Chạy thủ công qua JMeter GUI (khuyến nghị khi debug)

```bash
# Mở file trong JMeter GUI để xem realtime
jmeter -t src/test/jmeter/01_login_500users.jmx
jmeter -t src/test/jmeter/02_load_increase_threshold.jmx
jmeter -t src/test/jmeter/03_session_exam_workflow_500users.jmx

# Chạy non-GUI (nhanh hơn) và lưu kết quả
jmeter -n -t src/test/jmeter/01_login_500users.jmx -l results/01_result.jtl
jmeter -n -t src/test/jmeter/02_load_increase_threshold.jmx -l results/02_result.jtl

# Generate HTML report từ file .jtl
jmeter -g results/01_result.jtl -o reports/01_report/
```

### Mô tả các kịch bản JMeter

| File | Users | Mục tiêu | Thời gian ước tính |
|------|-------|----------|-------------------|
| `01_login_500users.jmx` | 500 CCU | Baseline performance (login + get me) | ~2 phút |
| `02_load_increase_threshold.jmx` | 500→750→1000→1250→1500 | Tìm ngưỡng hệ thống | ~10 phút |
| `03_session_exam_workflow_500users.jmx` | 500 CCU | Workflow thi cử (login → search exam → view subjects) | ~5 phút |

### Tiêu chí đánh giá ngưỡng (Threshold Criteria)

| Metric | Ngưỡng Pass | Ngưỡng Fail (đã chạm ngưỡng) |
|--------|------------|------------------------------|
| Error Rate | < 1% | ≥ 5% |
| Avg Response Time | < 2000ms | ≥ 5000ms |
| 95th Percentile | < 5000ms | ≥ 10000ms |
| Throughput (RPS) | > baseline | Giảm mạnh so với step trước |

> **Cách đọc kết quả `02_load_increase_threshold.jmx`:**
> Quan sát Summary Report trong JMeter GUI, theo dõi từng step (500/750/1000/1250/1500 users).
> Bước nào mà Error Rate đột ngột tăng vượt 5% hoặc response time > 5000ms = ngưỡng hệ thống.

### Chỉnh sửa biến JMeter

Mở file `.jmx` bằng JMeter GUI → Chọn **"User Defined Variables"** để sửa:
- `HOST`: địa chỉ server (mặc định: `localhost`)
- `PORT`: cổng server (mặc định: `8080`)
- `USERNAME`: tên đăng nhập (mặc định: `admin`)
- `PASSWORD`: mật khẩu (mặc định: `admin123`)
- `STEP_DURATION`: thời gian mỗi step trong kịch bản 02 (mặc định: `60` giây)

---

## 3. Chạy trong CI/CD (Jenkins)

File `Jenkinsfile` đã có sẵn trong project. Để thêm Selenium và JMeter vào pipeline:

```groovy
// Thêm vào Jenkinsfile
stage('Selenium Tests') {
    steps {
        sh 'mvn test -Dtest="*SeleniumTest" -Dselenium.headless=true'
    }
}

stage('JMeter Performance Tests') {
    steps {
        sh 'mvn jmeter:jmeter'
    }
    post {
        always {
            // Lưu kết quả JMeter
            archiveArtifacts artifacts: 'target/jmeter/results/*.jtl', allowEmptyArchive: true
        }
    }
}
```

---

## 4. Troubleshooting

### Selenium: ChromeDriver version mismatch
```
WebDriverManager tự tải ChromeDriver phù hợp - thường không cần làm gì.
Nếu bị lỗi, thử: mvn test -Dwdm.forceDownload=true -Dtest="*SeleniumTest"
```

### Selenium: Swagger UI không tải được
```
Kiểm tra backend đang chạy: curl http://localhost:8080/swagger-ui/index.html
Tăng timeout: -Dselenium.wait=30
```

### JMeter: Kết nối từ chối (Connection refused)
```
Kiểm tra backend đang chạy tại đúng port.
Kiểm tra firewall không block port 8080.
```

### JMeter: OutOfMemoryError khi chạy 1500 users
```
Tăng JVM heap size cho JMeter:
export HEAP="-Xms1g -Xmx4g -XX:MaxMetaspaceSize=256m"
jmeter -n -t src/test/jmeter/02_load_increase_threshold.jmx
```
