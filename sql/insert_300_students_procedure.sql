-- ============================================================
-- Script SQL sử dụng stored procedure để insert 300 students
-- ============================================================
-- Mật khẩu: '$2a$10$PmJL79fl06NsXLFnT3Hj6OelAa9Fg5R4ijOx2ITLBSpHVRvmro116'
-- Username: student{n} (student1, student2, ..., student300)
-- Code: STUDENT001, STUDENT002, ..., STUDENT300
-- Email: student{n}@example.com
-- Full name: Nguyễn Văn {n}
-- Role: STUDENT
-- ============================================================

DELIMITER $$

-- Xóa procedure cũ nếu tồn tại
DROP PROCEDURE IF EXISTS insert_students$$

-- Tạo stored procedure để insert students
CREATE PROCEDURE insert_students(IN num_students INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE username_val VARCHAR(255);
    DECLARE code_val VARCHAR(255);
    DECLARE email_val VARCHAR(255);
    DECLARE full_name_val VARCHAR(100);
    DECLARE password_val VARCHAR(255) DEFAULT '$2a$10$PmJL79fl06NsXLFnT3Hj6OelAa9Fg5R4ijOx2ITLBSpHVRvmro116';
    DECLARE inserted_count INT DEFAULT 0;
    DECLARE updated_count INT DEFAULT 0;
    DECLARE error_count INT DEFAULT 0;
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
    BEGIN
        SET error_count = error_count + 1;
        -- Tiếp tục xử lý các record tiếp theo thay vì dừng lại
    END;
    
    -- Bắt đầu transaction để đảm bảo tính nhất quán
    START TRANSACTION;
    
    -- Xóa các students cũ nếu cần (uncomment nếu muốn xóa trước khi insert)
    -- DELETE FROM users WHERE username LIKE 'student%' AND role = 'STUDENT';
    
    -- Vòng lặp để insert từng student
    WHILE i <= num_students DO
        SET username_val = CONCAT('student', i);
        SET code_val = CONCAT('STUDENT', LPAD(i, 3, '0'));
        SET email_val = CONCAT('student', i, '@example.com');
        SET full_name_val = CONCAT('Nguyễn Văn ', i);
        
        -- Kiểm tra xem đã tồn tại chưa
        IF NOT EXISTS (
            SELECT 1 FROM users 
            WHERE username = username_val 
            OR code = code_val 
            OR email = email_val
        ) THEN
            -- Insert mới
            INSERT INTO users (username, code, email, password, full_name, role, is_active, is_deleted, created_at, updated_at)
            VALUES (username_val, code_val, email_val, password_val, full_name_val, 'STUDENT', TRUE, FALSE, NOW(), NOW());
            
            IF ROW_COUNT() > 0 THEN
                SET inserted_count = inserted_count + 1;
            END IF;
        ELSE
            -- Cập nhật nếu đã tồn tại
            UPDATE users 
            SET code = code_val,
                email = email_val,
                full_name = full_name_val,
                password = password_val,
                role = 'STUDENT',
                is_active = TRUE,
                is_deleted = FALSE,
                updated_at = NOW()
            WHERE username = username_val;
            
            IF ROW_COUNT() > 0 THEN
                SET updated_count = updated_count + 1;
            END IF;
        END IF;
        
        SET i = i + 1;
    END WHILE;
    
    -- Commit transaction
    COMMIT;
    
    -- Trả về thông báo kết quả chi tiết
    SELECT 
        CONCAT('Hoàn thành! Đã xử lý ', num_students, ' students') AS message,
        inserted_count AS inserted_count,
        updated_count AS updated_count,
        error_count AS error_count,
        (inserted_count + updated_count) AS total_processed;
END$$

DELIMITER ;

-- ============================================================
-- Gọi procedure để insert 300 students
-- ============================================================
CALL insert_students(300);

-- ============================================================
-- Kiểm tra kết quả
-- ============================================================
-- SELECT COUNT(*) AS total_students FROM users WHERE username LIKE 'student%' AND role = 'STUDENT';
-- SELECT * FROM users WHERE username LIKE 'student%' AND role = 'STUDENT' ORDER BY username LIMIT 10;

-- ============================================================
-- Xóa procedure sau khi sử dụng (optional - uncomment nếu muốn)
-- ============================================================
-- DROP PROCEDURE IF EXISTS insert_students;
