# SQL Scripts - Hướng dẫn sử dụng

## File: `insert_300_students_procedure.sql`

### Mô tả
Stored procedure để insert 300 users với role STUDENT vào bảng `users`.

### Thông tin users được tạo:
- **Username**: `student1`, `student2`, ..., `student300`
- **Code**: `STUDENT001`, `STUDENT002`, ..., `STUDENT300`
- **Email**: `student1@example.com`, `student2@example.com`, ..., `student300@example.com`
- **Full Name**: `Nguyễn Văn 1`, `Nguyễn Văn 2`, ..., `Nguyễn Văn 300`
- **Password**: `$2a$10$PmJL79fl06NsXLFnT3Hj6OelAa9Fg5R4ijOx2ITLBSpHVRvmro116` (BCrypt hash)
- **Role**: `STUDENT`
- **is_active**: `TRUE`
- **is_deleted**: `FALSE`

### Cách sử dụng

#### 1. Chạy trực tiếp trong MySQL/MariaDB:
```sql
-- Mở file và chạy toàn bộ script
source sql/insert_300_students_procedure.sql;

-- Hoặc copy-paste toàn bộ nội dung vào MySQL client
```

#### 2. Chạy từ command line:
```bash
mysql -u root -p database_name < sql/insert_300_students_procedure.sql
```

#### 3. Chạy từ MySQL Workbench hoặc phpMyAdmin:
- Mở file `insert_300_students_procedure.sql`
- Chạy toàn bộ script

### Tùy chỉnh số lượng users

Để insert số lượng users khác (ví dụ: 500 users):
```sql
CALL insert_students(500);
```

### Kiểm tra kết quả

```sql
-- Đếm tổng số students đã tạo
SELECT COUNT(*) AS total_students 
FROM users 
WHERE username LIKE 'student%' AND role = 'STUDENT';

-- Xem danh sách 10 students đầu tiên
SELECT id, username, code, email, full_name, role, is_active 
FROM users 
WHERE username LIKE 'student%' AND role = 'STUDENT' 
ORDER BY username 
LIMIT 10;

-- Xem thông tin chi tiết một student
SELECT * FROM users WHERE username = 'student1';
```

### Xóa students đã tạo (nếu cần)

```sql
-- Xóa tất cả students có username bắt đầu bằng 'student'
DELETE FROM users WHERE username LIKE 'student%' AND role = 'STUDENT';
```

### Lưu ý

1. **Transaction**: Procedure sử dụng transaction để đảm bảo tính nhất quán dữ liệu
2. **ON DUPLICATE KEY UPDATE**: Nếu username/code/email đã tồn tại, sẽ cập nhật thay vì báo lỗi
3. **Error Handling**: Có xử lý lỗi và rollback nếu có vấn đề xảy ra
4. **Performance**: Với 300 records, procedure sẽ chạy nhanh. Nếu cần insert số lượng lớn hơn (1000+), nên cân nhắc sử dụng batch insert

### Troubleshooting

**Lỗi: Duplicate entry**
- Procedure đã xử lý bằng `ON DUPLICATE KEY UPDATE`, nên sẽ không báo lỗi
- Nếu muốn xóa trước khi insert, uncomment dòng DELETE trong procedure

**Lỗi: Table 'users' doesn't exist**
- Kiểm tra tên database và bảng
- Đảm bảo đã chọn đúng database: `USE your_database_name;`

**Lỗi: Access denied**
- Kiểm tra quyền của user MySQL
- Cần quyền CREATE PROCEDURE và INSERT

### Xóa procedure sau khi sử dụng

```sql
DROP PROCEDURE IF EXISTS insert_students;
```

---

## File: `add_students_to_classroom_procedure.sql`

### Mô tả
Stored procedure để thêm sinh viên vào classmember của một lớp theo `classroom_id`. Procedure sẽ thêm các users trong khoảng `user_id` từ `start_id` đến `end_id` vào lớp học. Nếu đã tồn tại classmember với `user_id` và `classroom_id` đó thì sẽ bỏ qua.

### Tham số

- `p_classroom_id` (BIGINT, required): ID của lớp học
- `p_user_id_start` (BIGINT, required): User ID bắt đầu (ví dụ: 1)
- `p_user_id_end` (BIGINT, required): User ID kết thúc (ví dụ: 50)
- `p_member_role` (VARCHAR(20), optional): Vai trò trong lớp, mặc định: `'STUDENT'`
- `p_member_status` (VARCHAR(20), optional): Trạng thái, mặc định: `'ACTIVE'`

### Cách sử dụng

#### 1. Chạy trực tiếp trong MySQL/MariaDB:
```sql
-- Mở file và chạy toàn bộ script để tạo procedure
source sql/add_students_to_classroom_procedure.sql;

-- Sau đó gọi procedure
CALL add_students_to_classroom(1, 1, 50);
```

#### 2. Ví dụ sử dụng:

```sql
-- Thêm users từ ID 1 đến 50 vào classroom có ID = 1 (sử dụng giá trị mặc định: STUDENT, ACTIVE)
CALL add_students_to_classroom(1, 1, 50, NULL, NULL);

-- Thêm users từ ID 51 đến 100 vào classroom có ID = 1 (sử dụng giá trị mặc định)
CALL add_students_to_classroom(1, 51, 100, NULL, NULL);

-- Thêm users từ ID 1 đến 100 vào classroom có ID = 2
CALL add_students_to_classroom(2, 1, 100, NULL, NULL);

-- Thêm với role và status tùy chỉnh
CALL add_students_to_classroom(1, 1, 50, 'STUDENT', 'ACTIVE');

-- Thêm với role ASSISTANT
CALL add_students_to_classroom(1, 1, 50, 'ASSISTANT', 'ACTIVE');
```

**Lưu ý:** Để sử dụng giá trị mặc định (STUDENT, ACTIVE), truyền `NULL` cho các tham số `p_member_role` và `p_member_status`.

### Kết quả trả về

Procedure sẽ trả về:
- `message`: Thông báo kết quả
- `inserted_count`: Số lượng users đã được thêm thành công
- `skipped_count`: Số lượng users bị bỏ qua (đã tồn tại hoặc user không tồn tại)
- `total_processed`: Tổng số users đã xử lý

### Kiểm tra kết quả

```sql
-- Xem danh sách members của một lớp
SELECT 
    cm.member_id,
    cm.classroom_id,
    cm.user_id,
    u.username,
    u.full_name,
    u.email,
    cm.member_role,
    cm.member_status,
    cm.joined_at
FROM class_members cm
JOIN users u ON cm.user_id = u.id
WHERE cm.classroom_id = 1
ORDER BY cm.user_id;

-- Đếm số lượng members của một lớp
SELECT 
    classroom_id,
    COUNT(*) AS total_members,
    COUNT(CASE WHEN member_status = 'ACTIVE' THEN 1 END) AS active_members,
    COUNT(CASE WHEN member_role = 'STUDENT' THEN 1 END) AS student_count
FROM class_members
WHERE classroom_id = 1
GROUP BY classroom_id;
```

### Tính năng

1. **Kiểm tra tồn tại**: Tự động bỏ qua nếu đã tồn tại classmember với cùng `user_id` và `classroom_id`
2. **Validation**: Kiểm tra classroom và user có tồn tại không
3. **Transaction**: Sử dụng transaction để đảm bảo tính nhất quán
4. **Error Handling**: Có xử lý lỗi và rollback nếu có vấn đề

### Lưu ý

- Procedure sẽ bỏ qua các users không tồn tại trong bảng `users`
- Procedure sẽ bỏ qua các classmember đã tồn tại (không báo lỗi)
- `joined_at` sẽ được set là thời gian hiện tại (NOW())
- Mặc định `member_role = 'STUDENT'` và `member_status = 'ACTIVE'`

### Troubleshooting

**Lỗi: Classroom với ID X không tồn tại**
- Kiểm tra `classroom_id` có đúng không
- Đảm bảo classroom đã được tạo trong bảng `classroom`

**Lỗi: user_id_start phải nhỏ hơn hoặc bằng user_id_end**
- Kiểm tra tham số truyền vào
- Đảm bảo `p_user_id_start <= p_user_id_end`

**Không có users nào được thêm**
- Kiểm tra các users trong khoảng ID có tồn tại không
- Kiểm tra xem các users đã là member của lớp chưa (sẽ bị bỏ qua)

### Xóa procedure sau khi sử dụng

```sql
DROP PROCEDURE IF EXISTS add_students_to_classroom;
```
