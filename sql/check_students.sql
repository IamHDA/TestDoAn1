-- ============================================================
-- Script kiểm tra số lượng students đã được tạo
-- ============================================================

-- Đếm tổng số students
SELECT COUNT(*) AS total_students 
FROM users 
WHERE username LIKE 'student%' AND role = 'STUDENT';

-- Xem danh sách students với ID
SELECT id, username, code, email, full_name, role, is_active, created_at
FROM users 
WHERE username LIKE 'student%' AND role = 'STUDENT' 
ORDER BY CAST(SUBSTRING(username, 8) AS UNSIGNED)
LIMIT 100;

-- Kiểm tra khoảng trống (tìm các số bị thiếu)
SELECT 
    t.n AS missing_number,
    CONCAT('student', t.n) AS missing_username
FROM (
    SELECT @row := @row + 1 AS n
    FROM (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
         (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3) t3,
         (SELECT @row := 0) r
) t
WHERE t.n BETWEEN 1 AND 300
AND NOT EXISTS (
    SELECT 1 FROM users 
    WHERE username = CONCAT('student', t.n) 
    AND role = 'STUDENT'
)
ORDER BY t.n;

-- Kiểm tra duplicate username
SELECT username, COUNT(*) AS count
FROM users
WHERE username LIKE 'student%' AND role = 'STUDENT'
GROUP BY username
HAVING COUNT(*) > 1;

-- Kiểm tra duplicate code
SELECT code, COUNT(*) AS count
FROM users
WHERE code LIKE 'STUDENT%' AND role = 'STUDENT'
GROUP BY code
HAVING COUNT(*) > 1;

-- Kiểm tra duplicate email
SELECT email, COUNT(*) AS count
FROM users
WHERE email LIKE 'student%@example.com' AND role = 'STUDENT'
GROUP BY email
HAVING COUNT(*) > 1;

-- Xem 10 students đầu tiên và 10 students cuối cùng
(SELECT id, username, code, email, full_name 
FROM users 
WHERE username LIKE 'student%' AND role = 'STUDENT' 
ORDER BY CAST(SUBSTRING(username, 8) AS UNSIGNED)
LIMIT 10)
UNION ALL
(SELECT id, username, code, email, full_name 
FROM users 
WHERE username LIKE 'student%' AND role = 'STUDENT' 
ORDER BY CAST(SUBSTRING(username, 8) AS UNSIGNED) DESC
LIMIT 10);
