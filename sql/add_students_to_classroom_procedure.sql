-- ============================================================
-- Stored Procedure: Thêm sinh viên vào classmember của một lớp
-- ============================================================
-- Mô tả: Thêm các users trong khoảng user_id từ start_id đến end_id 
--        vào classmember của một lớp theo classroom_id
-- Nếu đã tồn tại classmember với user_id và classroom_id đó thì bỏ qua
-- ============================================================

DELIMITER $$

-- Xóa procedure cũ nếu tồn tại
DROP PROCEDURE IF EXISTS add_students_to_classroom$$

-- Tạo stored procedure
CREATE PROCEDURE add_students_to_classroom(
    IN p_classroom_id BIGINT,
    IN p_user_id_start BIGINT,
    IN p_user_id_end BIGINT,
    IN p_member_role VARCHAR(20),
    IN p_member_status VARCHAR(20)
)
BEGIN
    DECLARE v_user_id BIGINT;
    DECLARE v_inserted_count INT DEFAULT 0;
    DECLARE v_skipped_count INT DEFAULT 0;
    DECLARE v_total_count INT DEFAULT 0;
    DECLARE v_member_role VARCHAR(20);
    DECLARE v_member_status VARCHAR(20);
    DECLARE v_error_message VARCHAR(500);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    -- Set giá trị mặc định nếu NULL
    SET v_member_role = IFNULL(p_member_role, 'STUDENT');
    SET v_member_status = IFNULL(p_member_status, 'ACTIVE');
    
    -- Validate input
    IF p_classroom_id IS NULL OR p_user_id_start IS NULL OR p_user_id_end IS NULL THEN
        SET v_error_message = 'Tham số không được NULL';
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_error_message;
    END IF;
    
    IF p_user_id_start > p_user_id_end THEN
        SET v_error_message = 'user_id_start phải nhỏ hơn hoặc bằng user_id_end';
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_error_message;
    END IF;
    
    -- Kiểm tra classroom có tồn tại không
    IF NOT EXISTS (SELECT 1 FROM classroom WHERE classroom_id = p_classroom_id) THEN
        SET v_error_message = CONCAT('Classroom với ID ', CAST(p_classroom_id AS CHAR), ' không tồn tại');
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_error_message;
    END IF;
    
    -- Bắt đầu transaction
    START TRANSACTION;
    
    -- Set biến đếm
    SET v_user_id = p_user_id_start;
    SET v_total_count = p_user_id_end - p_user_id_start + 1;
    
    -- Vòng lặp qua từng user_id
    WHILE v_user_id <= p_user_id_end DO
        -- Kiểm tra user có tồn tại không
        IF EXISTS (SELECT 1 FROM users WHERE id = v_user_id) THEN
            -- Kiểm tra xem đã tồn tại classmember chưa
            IF NOT EXISTS (
                SELECT 1 FROM class_members 
                WHERE classroom_id = p_classroom_id 
                AND user_id = v_user_id
            ) THEN
                -- Insert classmember mới
                INSERT INTO class_members (
                    classroom_id,
                    user_id,
                    member_role,
                    member_status,
                    joined_at,
                    created_at,
                    updated_at
                ) VALUES (
                    p_classroom_id,
                    v_user_id,
                    v_member_role,
                    v_member_status,
                    NOW(),
                    NOW(),
                    NOW()
                );
                
                SET v_inserted_count = v_inserted_count + 1;
            ELSE
                -- Đã tồn tại, bỏ qua
                SET v_skipped_count = v_skipped_count + 1;
            END IF;
        ELSE
            -- User không tồn tại, bỏ qua
            SET v_skipped_count = v_skipped_count + 1;
        END IF;
        
        SET v_user_id = v_user_id + 1;
    END WHILE;
    
    -- Commit transaction
    COMMIT;
    
    -- Trả về kết quả
    SELECT 
        CONCAT('Hoàn thành! Đã thêm ', v_inserted_count, ' sinh viên vào lớp ', p_classroom_id) AS message,
        v_inserted_count AS inserted_count,
        v_skipped_count AS skipped_count,
        v_total_count AS total_processed;
END$$

DELIMITER ;

-- ============================================================
-- Ví dụ sử dụng
-- ============================================================

-- Thêm users từ ID 1 đến 50 vào classroom có ID = 1 (sử dụng giá trị mặc định)
-- CALL add_students_to_classroom(1, 1, 50, NULL, NULL);

-- Thêm users từ ID 1 đến 100 vào classroom có ID = 2 với role STUDENT
-- CALL add_students_to_classroom(2, 1, 100, 'STUDENT', 'ACTIVE');

-- Thêm users từ ID 51 đến 100 vào classroom có ID = 1 (sử dụng giá trị mặc định)
-- CALL add_students_to_classroom(1, 51, 100, NULL, NULL);

-- ============================================================
-- Kiểm tra kết quả
-- ============================================================

-- Xem danh sách members của một lớp
-- SELECT 
--     cm.member_id,
--     cm.classroom_id,
--     cm.user_id,
--     u.username,
--     u.full_name,
--     u.email,
--     cm.member_role,
--     cm.member_status,
--     cm.joined_at
-- FROM class_members cm
-- JOIN users u ON cm.user_id = u.id
-- WHERE cm.classroom_id = 1
-- ORDER BY cm.user_id;

-- Đếm số lượng members của một lớp
-- SELECT 
--     classroom_id,
--     COUNT(*) AS total_members,
--     COUNT(CASE WHEN member_status = 'ACTIVE' THEN 1 END) AS active_members,
--     COUNT(CASE WHEN member_role = 'STUDENT' THEN 1 END) AS student_count
-- FROM class_members
-- WHERE classroom_id = 1
-- GROUP BY classroom_id;
