-- ============================================================
-- Script SQL cải thiện - Sử dụng INSERT IGNORE để tránh lỗi
-- ============================================================
-- Phiên bản này sử dụng INSERT IGNORE để bỏ qua các record trùng lặp
-- và đếm chính xác số lượng record được insert
-- ============================================================

DELIMITER $$

-- Xóa procedure cũ nếu tồn tại
DROP PROCEDURE IF EXISTS insert_students_improved$$

-- Tạo stored procedure cải thiện
CREATE PROCEDURE insert_students_improved(IN num_students INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE username_val VARCHAR(255);
    DECLARE code_val VARCHAR(255);
    DECLARE email_val VARCHAR(255);
    DECLARE full_name_val VARCHAR(100);
    DECLARE password_val VARCHAR(255) DEFAULT '$2a$10$PmJL79fl06NsXLFnT3Hj6OelAa9Fg5R4ijOx2ITLBSpHVRvmro116';
    DECLARE inserted_count INT DEFAULT 0;
    DECLARE skipped_count INT DEFAULT 0;
    DECLARE v_row_count INT;
    
    -- Bắt đầu transaction
    START TRANSACTION;
    
    -- Vòng lặp để insert từng student
    WHILE i <= num_students DO
        SET username_val = CONCAT('student', i);
        SET code_val = CONCAT('STUDENT', LPAD(i, 3, '0'));
        SET email_val = CONCAT('student', i, '@example.com');
        SET full_name_val = CONCAT('Nguyễn Văn ', i);
        
        -- Sử dụng INSERT IGNORE để bỏ qua duplicate
        INSERT IGNORE INTO users (username, code, email, password, full_name, role, is_active, is_deleted, created_at, updated_at)
        VALUES (username_val, code_val, email_val, password_val, full_name_val, 'STUDENT', TRUE, FALSE, NOW(), NOW());
        
        SET v_row_count = ROW_COUNT();
        
        IF v_row_count > 0 THEN
            SET inserted_count = inserted_count + 1;
        ELSE
            SET skipped_count = skipped_count + 1;
        END IF;
        
        SET i = i + 1;
    END WHILE;
    
    -- Commit transaction
    COMMIT;
    
    -- Trả về kết quả chi tiết
    SELECT 
        CONCAT('Hoàn thành! Đã xử lý ', num_students, ' students') AS message,
        inserted_count AS inserted_count,
        skipped_count AS skipped_count,
        num_students AS total_requested;
END$$

DELIMITER ;

-- ============================================================
-- Gọi procedure để insert 300 students
-- ============================================================
CALL insert_students_improved(300);

-- ============================================================
-- Kiểm tra kết quả
-- ============================================================
SELECT COUNT(*) AS total_students 
FROM users 
WHERE username LIKE 'student%' AND role = 'STUDENT';
