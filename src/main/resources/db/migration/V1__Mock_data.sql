-- --- MOCK DATA ---

-- Пользователи
-- Мы предполагаем, что при вставке в чистую таблицу:
-- 'adminuser' получит id = 1
-- 'testuser' получит id = 2
INSERT INTO users (username, email, password_hash, role) VALUES
                                                             ('adminuser', 'admin@example.com', '$2a$10$4O/6az6mhsHYT2Tr3Cnl/u2nx4yR6K2wrZ6E.W7/4Iy2gP9JyLwfy', 'ADMIN'), -- password: password
                                                             ('testuser', 'test@example.com', '$2a$10$4O/6az6mhsHYT2Tr3Cnl/u2nx4yR6K2wrZ6E.W7/4Iy2gP9JyLwfy', 'USER');    -- password: password

-- Модули
INSERT INTO modules (id, title, description) VALUES
                                                 (1, 'Introduction to Programming', 'Learn the basics of programming concepts and Python.'),
                                                 (2, 'Web Development Fundamentals', 'Understand HTML, CSS, and JavaScript to build web pages.'),
                                                 (3, 'Advanced Java Concepts', 'Deep dive into Java concurrency, streams, and more.');

-- Уроки (привязываем к модулям)
-- Module 1
INSERT INTO lessons (id, module_id, title, description, sequence_order) VALUES
                                                                            (1, 1, 'What is Programming?', 'Understanding the core idea of programming.', 1),
                                                                            (2, 1, 'Variables and Data Types', 'Learn about storing and manipulating data.', 2),
                                                                            (3, 1, 'Control Flow: If-Else', 'Making decisions in your code.', 3);

-- Module 2
INSERT INTO lessons (id, module_id, title, description, sequence_order) VALUES
                                                                            (4, 2, 'HTML Basics', 'Structure of a web page.', 1),
                                                                            (5, 2, 'CSS Styling', 'Making your web pages look good.', 2);

-- Контентные блоки уроков (привязываем к урокам)
-- Lesson 1
INSERT INTO lesson_content_blocks (lesson_id, block_type, content,  file_url, alt_text) VALUES
                                                                                                         (1, 'HEADER', 'Welcome to Programming!', NULL, NULL),
                                                                                                         (1, 'TEXT', 'Programming is the process of creating a set of instructions that tell a computer how to perform a task. It can be done using a variety of computer programming languages, such as Python, Java, C++, etc.', NULL, NULL),
                                                                                                         (1, 'IMAGE', NULL, 's3://bucket/images/programming_concept.jpg', 'Abstract image representing code');

-- Lesson 2
INSERT INTO lesson_content_blocks (lesson_id, block_type, content, file_url, alt_text) VALUES
                                                                                                         (2, 'HEADER', 'Understanding Variables', NULL, NULL),
                                                                                                         (2, 'TEXT', 'A variable is a container for storing data values. Different data types can be stored in variables.', NULL, NULL),
                                                                                                         (2, 'VIDEO', NULL, 's3://bucket/videos/variables_explained.mp4', 'Video explaining variables');

-- Lesson 4
INSERT INTO lesson_content_blocks (lesson_id, block_type, content, file_url, alt_text) VALUES
                                                                                                         (4, 'HEADER', 'HTML Introduction',NULL, NULL),
                                                                                                         (4, 'TEXT', 'HTML stands for HyperText Markup Language. It is the standard markup language for creating Web pages.',NULL, NULL),
                                                                                                         (4, 'LINK', 'https://developer.mozilla.org/en-US/docs/Web/HTML',NULL, NULL);


-- Тесты (привязываем к урокам)
INSERT INTO tests (id, lesson_id, title, pass_threshold_percentage) VALUES
                                                                        (1, 2, 'Variables Test', 75),
                                                                        (2, 4, 'HTML Basics Quiz', 60);

-- Вопросы для тестов (привязываем к тестам)
-- Test 1
INSERT INTO questions (id, test_id, text, sequence_order, max_score) VALUES
                                                                         (1, 1, 'What is a variable in programming?', 1, 2),
                                                                         (2, 1, 'Name two common data types.', 2, 2),
                                                                         (3, 1, 'Can the value of a variable change during program execution?', 3, 1);

-- Test 2
INSERT INTO questions (id, test_id, text, sequence_order, max_score) VALUES
                                                                         (4, 2, 'What does HTML stand for?', 1, 2),
                                                                         (5, 2, 'What is the basic building block of an HTML document?', 2, 2);


-- Прогресс пользователей по урокам
-- Для user_id: используем 2, предполагая, что 'testuser' получил id=2 (вторая вставка в users)
INSERT INTO user_lesson_progress (user_id, lesson_id, status, unlocked_at) VALUES
    (2, 1, 'UNLOCKED', CURRENT_TIMESTAMP - INTERVAL '2 day');
INSERT INTO user_lesson_progress (user_id, lesson_id, status, unlocked_at, started_at) VALUES
    (2, 2, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '12 hour');
INSERT INTO user_lesson_progress (user_id, lesson_id, status, unlocked_at, started_at, test_pending_at) VALUES
    (2, 4, 'TEST_PENDING', CURRENT_TIMESTAMP - INTERVAL '6 hour', CURRENT_TIMESTAMP - INTERVAL '5 hour', CURRENT_TIMESTAMP - INTERVAL '1 hour');
INSERT INTO user_lesson_progress (user_id, lesson_id, status, unlocked_at, started_at, test_pending_at, completed_at) VALUES
    (2, 5, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '10 hour', CURRENT_TIMESTAMP - INTERVAL '9 hour', CURRENT_TIMESTAMP - INTERVAL '8 hour', CURRENT_TIMESTAMP - INTERVAL '7 hour');

-- Попытки прохождения тестов пользователями
-- Для user_id: используем 2 для 'testuser'
-- Test Attempt ID 1
INSERT INTO user_test_attempts (id, user_id, test_id, attempt_number, score_achieved, max_possible_score, is_passed, attempted_at) VALUES
    (1, 2, 1, 1, 3.00, 5.00, FALSE, CURRENT_TIMESTAMP - INTERVAL '11 hour');

-- Test Attempt ID 2
INSERT INTO user_test_attempts (id, user_id, test_id, attempt_number, score_achieved, max_possible_score, is_passed, attempted_at) VALUES
    (2, 2, 2, 1, 4.00, 4.00, TRUE, CURRENT_TIMESTAMP - INTERVAL '30 minute');


-- Ответы пользователей на вопросы в рамках попыток
-- Test Attempt 1 (id=1)
INSERT INTO user_answers (user_test_attempt_id, question_id, answer_text, score_awarded, points_earned, ai_feedback) VALUES
                                                                                                                         (1, 1, 'A placeholder for data.', 'PARTIALLY_CORRECT', 1, 'Good start, but can you be more specific about its role?'),
                                                                                                                         (1, 3, 'No, it cannot.', 'INCORRECT', 0, 'Actually, the value of a variable can indeed change.');
-- Заметил, что пропустил ответ на question_id=2 для attempt_id=1, добавим его
INSERT INTO user_answers (user_test_attempt_id, question_id, answer_text, score_awarded, points_earned, ai_feedback) VALUES
    (1, 2, 'Integers and Strings.', 'CORRECT', 2, 'Excellent! Those are very common.');


-- Test Attempt 2 (id=2)
INSERT INTO user_answers (user_test_attempt_id, question_id, answer_text, score_awarded, points_earned, ai_feedback) VALUES
                                                                                                                         (2, 4, 'HyperText Markup Language', 'CORRECT', 2, 'Correct!'),
                                                                                                                         (2, 5, 'An element', 'CORRECT', 2, 'Spot on!');
-- --- UPDATE SEQUENCES (PostgreSQL specific) ---
-- Обновляем значения последовательностей для таблиц с авто-генерируемыми ID,
-- чтобы предотвратить конфликты после вставки данных с явно указанными ID
-- или после вставок, где ID генерировались базой данных.

-- Таблица 'users'
-- (ID для users генерировались автоматически при INSERT выше, эта команда синхронизирует последовательность)
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 0) + 1, false);

-- Таблица 'modules'
-- (ID для modules были указаны явно)
SELECT setval(pg_get_serial_sequence('modules', 'id'), COALESCE((SELECT MAX(id) FROM modules), 0) + 1, false);

-- Таблица 'lessons'
-- (ID для lessons были указаны явно)
SELECT setval(pg_get_serial_sequence('lessons', 'id'), COALESCE((SELECT MAX(id) FROM lessons), 0) + 1, false);

-- Таблица 'lesson_content_blocks'
-- (Предполагается, что 'lesson_content_blocks' имеет авто-генерируемый PK 'id'.
-- ID для lesson_content_blocks генерировались автоматически при INSERT выше, эта команда синхронизирует последовательность)
SELECT setval(pg_get_serial_sequence('lesson_content_blocks', 'id'), COALESCE((SELECT MAX(id) FROM lesson_content_blocks), 0) + 1, false);

-- Таблица 'tests'
-- (ID для tests были указаны явно)
SELECT setval(pg_get_serial_sequence('tests', 'id'), COALESCE((SELECT MAX(id) FROM tests), 0) + 1, false);

-- Таблица 'questions'
-- (ID для questions были указаны явно)
SELECT setval(pg_get_serial_sequence('questions', 'id'), COALESCE((SELECT MAX(id) FROM questions), 0) + 1, false);

-- Таблица 'user_test_attempts'
-- (ID для user_test_attempts были указаны явно)
SELECT setval(pg_get_serial_sequence('user_test_attempts', 'id'), COALESCE((SELECT MAX(id) FROM user_test_attempts), 0) + 1, false);
