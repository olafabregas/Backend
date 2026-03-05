-- ================================================
-- REVIEWFLOW DATABASE SEED SCRIPT
-- ================================================
-- All passwords are: Test@1234
-- BCrypt hash for Test@1234 (strength 12)
SET @password = '$2a$12$2fd9kCJl6UNMdPnzWr06NOMNkEyKtmgUyGamyYQquG2MU3o6kMGeq';

-- ================================================
-- CLEANUP EXISTING DATA (in correct order due to FK constraints)
-- ================================================
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE rubric_scores;
TRUNCATE TABLE evaluations;
TRUNCATE TABLE submissions;
TRUNCATE TABLE team_members;
TRUNCATE TABLE teams;
TRUNCATE TABLE rubric_criteria;
TRUNCATE TABLE assignments;
TRUNCATE TABLE course_enrollments;
TRUNCATE TABLE course_instructors;
TRUNCATE TABLE courses;
TRUNCATE TABLE refresh_tokens;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- ================================================
-- INSTRUCTORS (5)
-- ================================================
INSERT INTO users (id, email, password_hash, first_name, last_name, role, is_active, created_at, updated_at) VALUES
(1, 'sarah.johnson@university.edu', @password, 'Sarah',    'Johnson',  'INSTRUCTOR', true, NOW(), NOW()),
(2, 'michael.torres@university.edu', @password, 'Michael',  'Torres',   'INSTRUCTOR', true, NOW(), NOW()),
(3, 'emily.chen@university.edu',     @password, 'Emily',    'Chen',     'INSTRUCTOR', true, NOW(), NOW()),
(4, 'david.kim@university.edu',      @password, 'David',    'Kim',      'INSTRUCTOR', true, NOW(), NOW()),
(5, 'lisa.martinez@university.edu',  @password, 'Lisa',     'Martinez', 'INSTRUCTOR', true, NOW(), NOW());

-- ================================================
-- STUDENTS (30)
-- ================================================
INSERT INTO users (id, email, password_hash, first_name, last_name, role, is_active, created_at, updated_at) VALUES
(6,  'jane.smith@university.edu',       @password, 'Jane',      'Smith',      'STUDENT', true, NOW(), NOW()),
(7,  'marcus.chen@university.edu',      @password, 'Marcus',    'Chen',       'STUDENT', true, NOW(), NOW()),
(8,  'priya.patel@university.edu',      @password, 'Priya',     'Patel',      'STUDENT', true, NOW(), NOW()),
(9,  'sam.lee@university.edu',          @password, 'Sam',       'Lee',        'STUDENT', true, NOW(), NOW()),
(10, 'alex.kumar@university.edu',       @password, 'Alex',      'Kumar',      'STUDENT', true, NOW(), NOW()),
(11, 'rachel.park@university.edu',      @password, 'Rachel',    'Park',       'STUDENT', true, NOW(), NOW()),
(12, 'liam.nguyen@university.edu',      @password, 'Liam',      'Nguyen',     'STUDENT', true, NOW(), NOW()),
(13, 'tariq.ali@university.edu',        @password, 'Tariq',     'Ali',        'STUDENT', true, NOW(), NOW()),
(14, 'chloe.wang@university.edu',       @password, 'Chloe',     'Wang',       'STUDENT', true, NOW(), NOW()),
(15, 'ben.okafor@university.edu',       @password, 'Ben',       'Okafor',     'STUDENT', true, NOW(), NOW()),
(16, 'sofia.rodriguez@university.edu',  @password, 'Sofia',     'Rodriguez',  'STUDENT', true, NOW(), NOW()),
(17, 'noah.brown@university.edu',       @password, 'Noah',      'Brown',      'STUDENT', true, NOW(), NOW()),
(18, 'mia.garcia@university.edu',       @password, 'Mia',       'Garcia',     'STUDENT', true, NOW(), NOW()),
(19, 'ethan.wilson@university.edu',     @password, 'Ethan',     'Wilson',     'STUDENT', true, NOW(), NOW()),
(20, 'olivia.davis@university.edu',     @password, 'Olivia',    'Davis',      'STUDENT', true, NOW(), NOW()),
(21, 'james.miller@university.edu',     @password, 'James',     'Miller',     'STUDENT', true, NOW(), NOW()),
(22, 'emma.anderson@university.edu',    @password, 'Emma',      'Anderson',   'STUDENT', true, NOW(), NOW()),
(23, 'william.taylor@university.edu',   @password, 'William',   'Taylor',     'STUDENT', true, NOW(), NOW()),
(24, 'ava.thomas@university.edu',       @password, 'Ava',       'Thomas',     'STUDENT', true, NOW(), NOW()),
(25, 'lucas.moore@university.edu',      @password, 'Lucas',     'Moore',      'STUDENT', true, NOW(), NOW()),
(26, 'isabella.martin@university.edu',  @password, 'Isabella',  'Martin',     'STUDENT', true, NOW(), NOW()),
(27, 'mason.jackson@university.edu',    @password, 'Mason',     'Jackson',    'STUDENT', true, NOW(), NOW()),
(28, 'sophia.thompson@university.edu',  @password, 'Sophia',    'Thompson',   'STUDENT', true, NOW(), NOW()),
(29, 'logan.white@university.edu',      @password, 'Logan',     'White',      'STUDENT', true, NOW(), NOW()),
(30, 'charlotte.harris@university.edu', @password, 'Charlotte', 'Harris',     'STUDENT', true, NOW(), NOW()),
(31, 'jacob.clark@university.edu',      @password, 'Jacob',     'Clark',      'STUDENT', true, NOW(), NOW()),
(32, 'amelia.lewis@university.edu',     @password, 'Amelia',    'Lewis',      'STUDENT', true, NOW(), NOW()),
(33, 'elijah.robinson@university.edu',  @password, 'Elijah',    'Robinson',   'STUDENT', true, NOW(), NOW()),
(34, 'harper.walker@university.edu',    @password, 'Harper',    'Walker',     'STUDENT', true, NOW(), NOW()),
(35, 'aiden.hall@university.edu',       @password, 'Aiden',     'Hall',       'STUDENT', true, NOW(), NOW());

-- Reset auto-increment to continue from 36
ALTER TABLE users AUTO_INCREMENT = 36;

-- ================================================
-- COURSES (6)
-- ================================================
INSERT INTO courses (code, name, term, description, is_archived, created_by, created_at) VALUES
('CS401', 'Advanced Software Engineering', 'Spring 2026', 
 'Advanced topics in software engineering including architecture, design patterns, testing, and deployment strategies.', 
 false, 1, NOW()),

('CS402', 'Database Systems', 'Spring 2026', 
 'Comprehensive study of relational and non-relational databases, query optimization, transactions, and data modeling.', 
 false, 2, NOW()),

('CS403', 'Web Application Development', 'Spring 2026', 
 'Full-stack web development covering modern frameworks, REST APIs, authentication, and frontend technologies.', 
 false, 3, NOW()),

('CS404', 'Cloud Computing Architecture', 'Spring 2026', 
 'Design and implementation of scalable cloud-based systems including microservices, containers, and serverless computing.', 
 false, 4, NOW()),

('CS405', 'Mobile App Development', 'Spring 2026', 
 'Cross-platform mobile application development using modern frameworks and best practices for iOS and Android.', 
 false, 5, NOW()),

('CS406', 'Machine Learning Systems', 'Spring 2026', 
 'Practical machine learning systems design, model deployment, MLOps, and production ML engineering.', 
 false, 1, NOW());

-- ================================================
-- ASSIGN INSTRUCTORS TO COURSES
-- ================================================
INSERT INTO course_instructors (course_id, user_id, assigned_at) VALUES
(1, 1, NOW()),  -- Sarah Johnson   → CS401
(2, 2, NOW()),  -- Michael Torres  → CS402
(3, 3, NOW()),  -- Emily Chen      → CS403
(4, 4, NOW()),  -- David Kim       → CS404
(5, 5, NOW()),  -- Lisa Martinez   → CS405
(6, 1, NOW());  -- Sarah Johnson   → CS406 (teaching two courses)

-- ================================================
-- ENROLL STUDENTS IN COURSES
-- CS401: 20 students
-- CS402: 18 students
-- CS403: 24 students
-- CS404: 15 students
-- CS405: 22 students
-- CS406: 12 students
-- ================================================
INSERT INTO course_enrollments (course_id, user_id, enrolled_at) VALUES
-- CS401 (20 students)
(1, 6, NOW()), (1, 7, NOW()), (1, 8, NOW()), (1, 9, NOW()), (1, 10, NOW()),
(1, 11, NOW()), (1, 12, NOW()), (1, 13, NOW()), (1, 14, NOW()), (1, 15, NOW()),
(1, 16, NOW()), (1, 17, NOW()), (1, 18, NOW()), (1, 19, NOW()), (1, 20, NOW()),
(1, 21, NOW()), (1, 22, NOW()), (1, 23, NOW()), (1, 24, NOW()), (1, 25, NOW()),

-- CS402 (18 students)
(2, 6, NOW()), (2, 7, NOW()), (2, 8, NOW()), (2, 9, NOW()), (2, 10, NOW()),
(2, 11, NOW()), (2, 12, NOW()), (2, 13, NOW()), (2, 14, NOW()), (2, 15, NOW()),
(2, 26, NOW()), (2, 27, NOW()), (2, 28, NOW()), (2, 29, NOW()), (2, 30, NOW()),
(2, 31, NOW()), (2, 32, NOW()), (2, 33, NOW()),

-- CS403 (24 students)
(3, 6, NOW()), (3, 8, NOW()), (3, 10, NOW()), (3, 12, NOW()), (3, 14, NOW()),
(3, 16, NOW()), (3, 18, NOW()), (3, 20, NOW()), (3, 22, NOW()), (3, 24, NOW()),
(3, 25, NOW()), (3, 26, NOW()), (3, 27, NOW()), (3, 28, NOW()), (3, 29, NOW()),
(3, 30, NOW()), (3, 31, NOW()), (3, 32, NOW()), (3, 33, NOW()), (3, 34, NOW()),
(3, 35, NOW()), (3, 7, NOW()), (3, 9, NOW()), (3, 11, NOW()),

-- CS404 (15 students)
(4, 6, NOW()), (4, 9, NOW()), (4, 12, NOW()), (4, 15, NOW()), (4, 18, NOW()),
(4, 21, NOW()), (4, 24, NOW()), (4, 27, NOW()), (4, 30, NOW()), (4, 33, NOW()),
(4, 7, NOW()), (4, 10, NOW()), (4, 13, NOW()), (4, 16, NOW()), (4, 19, NOW()),

-- CS405 (22 students)
(5, 8, NOW()), (5, 10, NOW()), (5, 12, NOW()), (5, 14, NOW()), (5, 16, NOW()),
(5, 18, NOW()), (5, 20, NOW()), (5, 22, NOW()), (5, 24, NOW()), (5, 26, NOW()),
(5, 28, NOW()), (5, 30, NOW()), (5, 32, NOW()), (5, 34, NOW()), (5, 35, NOW()),
(5, 7, NOW()), (5, 9, NOW()), (5, 11, NOW()), (5, 13, NOW()), (5, 15, NOW()),
(5, 17, NOW()), (5, 19, NOW()),

-- CS406 (12 students)
(6, 6, NOW()), (6, 10, NOW()), (6, 14, NOW()), (6, 18, NOW()), (6, 22, NOW()),
(6, 26, NOW()), (6, 30, NOW()), (6, 34, NOW()), (6, 8, NOW()), (6, 12, NOW()),
(6, 16, NOW()), (6, 20, NOW());

-- ================================================
-- ASSIGNMENTS (7 total)
-- CS401: 2 assignments
-- CS402: 2 assignments
-- CS403: 1 assignment
-- CS404: 1 assignment
-- CS405: 1 assignment
-- ================================================
INSERT INTO assignments (course_id, title, description, due_at, max_team_size, is_published, team_lock_at, created_at) VALUES
-- CS401 Assignments
(1, 'Project Phase 1 — System Design',
 'Design and document the system architecture for your chosen project. Include ER diagrams, API contracts, component diagrams, and tech stack justification with trade-off analysis.',
 DATE_ADD(NOW(), INTERVAL 10 DAY), 4, true, DATE_ADD(NOW(), INTERVAL 3 DAY), NOW()),

(1, 'Project Phase 2 — Backend Implementation',
 'Implement the backend REST API with authentication, database integration, error handling, and comprehensive unit tests with minimum 70% code coverage.',
 DATE_ADD(NOW(), INTERVAL 24 DAY), 4, true, DATE_ADD(NOW(), INTERVAL 6 DAY), NOW()),

-- CS402 Assignments
(2, 'Database Design Project',
 'Design and implement a normalized relational database schema for a complex business scenario. Include query optimization, indexing strategy, and sample data.',
 DATE_ADD(NOW(), INTERVAL 14 DAY), 3, true, DATE_ADD(NOW(), INTERVAL 4 DAY), NOW()),

(2, 'Database Performance Optimization',
 'Analyze and optimize database queries, implement proper indexing, and demonstrate performance improvements using EXPLAIN analysis.',
 DATE_ADD(NOW(), INTERVAL 21 DAY), 3, true, DATE_ADD(NOW(), INTERVAL 7 DAY), NOW()),

-- CS403 Assignment
(3, 'Full-Stack E-Commerce Application',
 'Build a complete e-commerce web application with user authentication, product catalog, shopping cart, and payment integration.',
 DATE_ADD(NOW(), INTERVAL 28 DAY), 4, true, DATE_ADD(NOW(), INTERVAL 7 DAY), NOW()),

-- CS404 Assignment
(4, 'Cloud-Native Microservices Architecture',
 'Design and deploy a microservices-based application using containers, API gateway, service discovery, and cloud infrastructure.',
 DATE_ADD(NOW(), INTERVAL 21 DAY), 3, true, DATE_ADD(NOW(), INTERVAL 5 DAY), NOW()),

-- CS405 Assignment
(5, 'Cross-Platform Mobile App',
 'Develop a full-featured mobile application with offline support, push notifications, and backend API integration.',
 DATE_ADD(NOW(), INTERVAL 28 DAY), 3, true, DATE_ADD(NOW(), INTERVAL 7 DAY), NOW());

-- ================================================
-- RUBRIC CRITERIA
-- ================================================

-- Assignment 1: CS401 - System Design (100 pts)
INSERT INTO rubric_criteria (assignment_id, name, description, max_score, display_order) VALUES
(1, 'Architecture Diagram', 'Clear, complete system architecture diagram with all components and their interactions', 20, 1),
(1, 'API Contract Design', 'Well-defined REST API endpoints with request/response schemas and validation rules', 20, 2),
(1, 'Database Schema', 'Normalized ER diagram with proper relationships, constraints, and data types', 20, 3),
(1, 'Tech Stack Justification', 'Clear reasoning for chosen technologies with trade-off analysis and alternatives considered', 20, 4),
(1, 'Documentation Quality', 'Comprehensive documentation with clear explanations and professional presentation', 20, 5);

-- Assignment 2: CS401 - Backend Implementation (100 pts)
INSERT INTO rubric_criteria (assignment_id, name, description, max_score, display_order) VALUES
(2, 'Code Quality', 'Clean, readable, well-structured code following best practices and SOLID principles', 20, 1),
(2, 'Architecture & Design', 'Proper separation of concerns, design patterns used correctly, and maintainable structure', 20, 2),
(2, 'Testing Coverage', 'Comprehensive unit tests present with minimum 70% line coverage and meaningful assertions', 20, 3),
(2, 'API Documentation', 'Complete API documentation with Swagger/OpenAPI spec and clear usage examples', 15, 4),
(2, 'Error Handling', 'Robust error handling with appropriate HTTP status codes and meaningful error messages', 15, 5),
(2, 'Presentation/Demo', 'Live demo or video walkthrough demonstrating all features and functionality', 10, 6);

-- Assignment 3: CS402 - Database Design (100 pts)
INSERT INTO rubric_criteria (assignment_id, name, description, max_score, display_order) VALUES
(3, 'Schema Normalization', 'Fully normalized schema (3NF minimum) with correct data types and no redundancy', 25, 1),
(3, 'Relationships & Constraints', 'Proper foreign keys, unique constraints, check constraints, and referential integrity', 20, 2),
(3, 'Query Performance', 'Indexes applied correctly, queries optimized with EXPLAIN analysis showing improvements', 25, 3),
(3, 'Documentation', 'Complete data dictionary, ER diagram, and business rules documentation', 15, 4),
(3, 'Sample Data & Testing', 'Working database with realistic sample data and all constraints properly enforced', 15, 5);

-- Assignment 4: CS402 - Performance Optimization (100 pts)
INSERT INTO rubric_criteria (assignment_id, name, description, max_score, display_order) VALUES
(4, 'Query Analysis', 'Thorough analysis of slow queries using EXPLAIN and identifying bottlenecks', 25, 1),
(4, 'Indexing Strategy', 'Appropriate indexes created with justification for each index decision', 25, 2),
(4, 'Performance Improvements', 'Measurable performance improvements demonstrated with before/after metrics', 25, 3),
(4, 'Query Optimization', 'Queries refactored for better performance using joins, subqueries, and efficient patterns', 15, 4),
(4, 'Documentation', 'Clear documentation of optimization process, decisions, and results', 10, 5);

-- Assignment 5: CS403 - Full-Stack E-Commerce (100 pts)
INSERT INTO rubric_criteria (assignment_id, name, description, max_score, display_order) VALUES
(5, 'Frontend Implementation', 'Responsive, modern UI with good UX and proper component architecture', 20, 1),
(5, 'Backend API', 'RESTful API with proper authentication, authorization, and CRUD operations', 20, 2),
(5, 'Security', 'Secure authentication, input validation, XSS prevention, and CSRF protection', 15, 3),
(5, 'Database Integration', 'Proper data persistence, transactions, and database operations', 15, 4),
(5, 'Feature Completeness', 'All required features implemented and working: catalog, cart, checkout, payments', 20, 5),
(5, 'Code Quality & Testing', 'Clean code with tests and proper error handling', 10, 6);

-- Assignment 6: CS404 - Cloud Microservices (100 pts)
INSERT INTO rubric_criteria (assignment_id, name, description, max_score, display_order) VALUES
(6, 'Microservices Design', 'Well-designed microservices with clear boundaries and single responsibility', 20, 1),
(6, 'Containerization', 'Proper Docker containers with optimized images and configuration', 20, 2),
(6, 'Service Communication', 'Effective inter-service communication with API gateway and service discovery', 15, 3),
(6, 'Cloud Deployment', 'Successfully deployed to cloud platform with proper configuration', 20, 4),
(6, 'Scalability & Resilience', 'Load balancing, health checks, and fault tolerance implemented', 15, 5),
(6, 'Documentation', 'Architecture documentation, deployment guide, and API documentation', 10, 6);

-- Assignment 7: CS405 - Mobile App (100 pts)
INSERT INTO rubric_criteria (assignment_id, name, description, max_score, display_order) VALUES
(7, 'UI/UX Design', 'Intuitive, platform-appropriate UI with smooth navigation and good user experience', 20, 1),
(7, 'Core Functionality', 'All core features implemented and working correctly on both platforms', 20, 2),
(7, 'Backend Integration', 'Proper API integration with error handling and loading states', 15, 3),
(7, 'Offline Support', 'Local data persistence and offline functionality with proper sync', 15, 4),
(7, 'Push Notifications', 'Push notifications implemented and working correctly', 10, 5),
(7, 'Code Quality', 'Clean, maintainable code following platform best practices', 10, 6),
(7, 'Testing & Performance', 'App performance optimized with reasonable test coverage', 10, 7);

-- ================================================
-- TEAMS
-- ================================================

-- CS401 Assignment 1 Teams (5 teams, 20 students)
INSERT INTO teams (assignment_id, name, is_locked, created_by, created_at) VALUES
(1, 'Team Alpha',   false, 6,  NOW()),
(1, 'Team Beta',    false, 10, NOW()),
(1, 'Team Gamma',   false, 14, NOW()),
(1, 'Team Delta',   false, 18, NOW()),
(1, 'Team Epsilon', false, 22, NOW());

-- CS401 Assignment 2 Teams (5 teams, 20 students)
INSERT INTO teams (assignment_id, name, is_locked, created_by, created_at) VALUES
(2, 'Team Velocity',  false, 6,  NOW()),
(2, 'Team Phoenix',   false, 10, NOW()),
(2, 'Team Nexus',     false, 14, NOW()),
(2, 'Team Quantum',   false, 18, NOW()),
(2, 'Team Horizon',   false, 22, NOW());

-- CS402 Assignment 3 Teams (6 teams, 18 students)
INSERT INTO teams (assignment_id, name, is_locked, created_by, created_at) VALUES
(3, 'Team Schema',    false, 6,  NOW()),
(3, 'Team Indexers',  false, 9,  NOW()),
(3, 'Team Relations', false, 12, NOW()),
(3, 'Team Queries',   false, 26, NOW()),
(3, 'Team DataFlow',  false, 29, NOW()),
(3, 'Team Optimizers',false, 32, NOW());

-- CS402 Assignment 4 Teams (6 teams, 18 students)
INSERT INTO teams (assignment_id, name, is_locked, created_by, created_at) VALUES
(4, 'Team Turbo',     false, 6,  NOW()),
(4, 'Team SpeedDB',   false, 9,  NOW()),
(4, 'Team FastQuery', false, 12, NOW()),
(4, 'Team Lightning', false, 26, NOW()),
(4, 'Team Boost',     false, 29, NOW()),
(4, 'Team Accelerate',false, 32, NOW());

-- CS403 Assignment 5 Teams (6 teams, 24 students)
INSERT INTO teams (assignment_id, name, is_locked, created_by, created_at) VALUES
(5, 'Team ShopMaster', false, 6,  NOW()),
(5, 'Team CartPro',    false, 10, NOW()),
(5, 'Team BuyNow',     false, 14, NOW()),
(5, 'Team MarketPlace',false, 18, NOW()),
(5, 'Team EcomHub',    false, 25, NOW()),
(5, 'Team TradeWave',  false, 29, NOW());

-- CS404 Assignment 6 Teams (5 teams, 15 students)
INSERT INTO teams (assignment_id, name, is_locked, created_by, created_at) VALUES
(6, 'Team CloudNine',  false, 6,  NOW()),
(6, 'Team Kubernetes', false, 9,  NOW()),
(6, 'Team Serverless', false, 12, NOW()),
(6, 'Team MicroStack', false, 18, NOW()),
(6, 'Team CloudForge', false, 24, NOW());

-- CS405 Assignment 7 Teams (7 teams, 21 students)
INSERT INTO teams (assignment_id, name, is_locked, created_by, created_at) VALUES
(7, 'Team MobileFirst', false, 8,  NOW()),
(7, 'Team AppCrafters', false, 11, NOW()),
(7, 'Team ReactNative', false, 14, NOW()),
(7, 'Team FlutterDev',  false, 17, NOW()),
(7, 'Team CrossPlat',   false, 20, NOW()),
(7, 'Team AppBuilder',  false, 26, NOW()),
(7, 'Team MobiTech',    false, 32, NOW());

-- ================================================
-- TEAM MEMBERS
-- ================================================

-- CS401 Assignment 1 Team Members (4 per team)
INSERT INTO team_members (team_id, user_id, assignment_id, joined_at, invited_by, status) VALUES
-- Team Alpha (Jane, Marcus, Priya, Sam)
(1, 6,  1, NOW(), 6,  'ACCEPTED'),
(1, 7,  1, NOW(), 6,  'ACCEPTED'),
(1, 8,  1, NOW(), 6,  'ACCEPTED'),
(1, 9,  1, NOW(), 6,  'ACCEPTED'),
-- Team Beta (Alex, Rachel, Liam, Tariq)
(2, 10, 1, NOW(), 10, 'ACCEPTED'),
(2, 11, 1, NOW(), 10, 'ACCEPTED'),
(2, 12, 1, NOW(), 10, 'ACCEPTED'),
(2, 13, 1, NOW(), 10, 'ACCEPTED'),
-- Team Gamma (Chloe, Ben, Sofia, Noah)
(3, 14, 1, NOW(), 14, 'ACCEPTED'),
(3, 15, 1, NOW(), 14, 'ACCEPTED'),
(3, 16, 1, NOW(), 14, 'ACCEPTED'),
(3, 17, 1, NOW(), 14, 'ACCEPTED'),
-- Team Delta (Mia, Ethan, Olivia, James)
(4, 18, 1, NOW(), 18, 'ACCEPTED'),
(4, 19, 1, NOW(), 18, 'ACCEPTED'),
(4, 20, 1, NOW(), 18, 'ACCEPTED'),
(4, 21, 1, NOW(), 18, 'ACCEPTED'),
-- Team Epsilon (Emma, William, Ava, Lucas)
(5, 22, 1, NOW(), 22, 'ACCEPTED'),
(5, 23, 1, NOW(), 22, 'ACCEPTED'),
(5, 24, 1, NOW(), 22, 'ACCEPTED'),
(5, 25, 1, NOW(), 22, 'ACCEPTED');

-- CS401 Assignment 2 Team Members (same grouping, 4 per team)
INSERT INTO team_members (team_id, user_id, assignment_id, joined_at, invited_by, status) VALUES
-- Team Velocity
(6, 6,  2, NOW(), 6,  'ACCEPTED'),
(6, 7,  2, NOW(), 6,  'ACCEPTED'),
(6, 8,  2, NOW(), 6,  'ACCEPTED'),
(6, 9,  2, NOW(), 6,  'ACCEPTED'),
-- Team Phoenix
(7, 10, 2, NOW(), 10, 'ACCEPTED'),
(7, 11, 2, NOW(), 10, 'ACCEPTED'),
(7, 12, 2, NOW(), 10, 'ACCEPTED'),
(7, 13, 2, NOW(), 10, 'ACCEPTED'),
-- Team Nexus
(8, 14, 2, NOW(), 14, 'ACCEPTED'),
(8, 15, 2, NOW(), 14, 'ACCEPTED'),
(8, 16, 2, NOW(), 14, 'ACCEPTED'),
(8, 17, 2, NOW(), 14, 'ACCEPTED'),
-- Team Quantum
(9, 18, 2, NOW(), 18, 'ACCEPTED'),
(9, 19, 2, NOW(), 18, 'ACCEPTED'),
(9, 20, 2, NOW(), 18, 'ACCEPTED'),
(9, 21, 2, NOW(), 18, 'ACCEPTED'),
-- Team Horizon
(10, 22, 2, NOW(), 22, 'ACCEPTED'),
(10, 23, 2, NOW(), 22, 'ACCEPTED'),
(10, 24, 2, NOW(), 22, 'ACCEPTED'),
(10, 25, 2, NOW(), 22, 'ACCEPTED');

-- CS402 Assignment 3 Team Members (3 per team)
INSERT INTO team_members (team_id, user_id, assignment_id, joined_at, invited_by, status) VALUES
-- Team Schema
(11, 6,  3, NOW(), 6,  'ACCEPTED'),
(11, 7,  3, NOW(), 6,  'ACCEPTED'),
(11, 8,  3, NOW(), 6,  'ACCEPTED'),
-- Team Indexers
(12, 9,  3, NOW(), 9,  'ACCEPTED'),
(12, 10, 3, NOW(), 9,  'ACCEPTED'),
(12, 11, 3, NOW(), 9,  'ACCEPTED'),
-- Team Relations
(13, 12, 3, NOW(), 12, 'ACCEPTED'),
(13, 13, 3, NOW(), 12, 'ACCEPTED'),
(13, 14, 3, NOW(), 12, 'ACCEPTED'),
-- Team Queries
(14, 26, 3, NOW(), 26, 'ACCEPTED'),
(14, 27, 3, NOW(), 26, 'ACCEPTED'),
(14, 28, 3, NOW(), 26, 'ACCEPTED'),
-- Team DataFlow
(15, 29, 3, NOW(), 29, 'ACCEPTED'),
(15, 30, 3, NOW(), 29, 'ACCEPTED'),
(15, 31, 3, NOW(), 29, 'ACCEPTED'),
-- Team Optimizers
(16, 32, 3, NOW(), 32, 'ACCEPTED'),
(16, 33, 3, NOW(), 32, 'ACCEPTED'),
(16, 15, 3, NOW(), 32, 'ACCEPTED');

-- CS402 Assignment 4 Team Members (3 per team)
INSERT INTO team_members (team_id, user_id, assignment_id, joined_at, invited_by, status) VALUES
-- Team Turbo
(17, 6,  4, NOW(), 6,  'ACCEPTED'),
(17, 7,  4, NOW(), 6,  'ACCEPTED'),
(17, 8,  4, NOW(), 6,  'ACCEPTED'),
-- Team SpeedDB
(18, 9,  4, NOW(), 9,  'ACCEPTED'),
(18, 10, 4, NOW(), 9,  'ACCEPTED'),
(18, 11, 4, NOW(), 9,  'ACCEPTED'),
-- Team FastQuery
(19, 12, 4, NOW(), 12, 'ACCEPTED'),
(19, 13, 4, NOW(), 12, 'ACCEPTED'),
(19, 14, 4, NOW(), 12, 'ACCEPTED'),
-- Team Lightning
(20, 26, 4, NOW(), 26, 'ACCEPTED'),
(20, 27, 4, NOW(), 26, 'ACCEPTED'),
(20, 28, 4, NOW(), 26, 'ACCEPTED'),
-- Team Boost
(21, 29, 4, NOW(), 29, 'ACCEPTED'),
(21, 30, 4, NOW(), 29, 'ACCEPTED'),
(21, 31, 4, NOW(), 29, 'ACCEPTED'),
-- Team Accelerate
(22, 32, 4, NOW(), 32, 'ACCEPTED'),
(22, 33, 4, NOW(), 32, 'ACCEPTED'),
(22, 15, 4, NOW(), 32, 'ACCEPTED');

-- CS403 Assignment 5 Team Members (4 per team)
INSERT INTO team_members (team_id, user_id, assignment_id, joined_at, invited_by, status) VALUES
-- Team ShopMaster
(23, 6,  5, NOW(), 6,  'ACCEPTED'),
(23, 8,  5, NOW(), 6,  'ACCEPTED'),
(23, 10, 5, NOW(), 6,  'ACCEPTED'),
(23, 12, 5, NOW(), 6,  'ACCEPTED'),
-- Team CartPro
(24, 14, 5, NOW(), 14, 'ACCEPTED'),
(24, 16, 5, NOW(), 14, 'ACCEPTED'),
(24, 18, 5, NOW(), 14, 'ACCEPTED'),
(24, 20, 5, NOW(), 14, 'ACCEPTED'),
-- Team BuyNow
(25, 22, 5, NOW(), 22, 'ACCEPTED'),
(25, 24, 5, NOW(), 22, 'ACCEPTED'),
(25, 26, 5, NOW(), 22, 'ACCEPTED'),
(25, 28, 5, NOW(), 22, 'ACCEPTED'),
-- Team MarketPlace
(26, 30, 5, NOW(), 30, 'ACCEPTED'),
(26, 32, 5, NOW(), 30, 'ACCEPTED'),
(26, 34, 5, NOW(), 30, 'ACCEPTED'),
(26, 7,  5, NOW(), 30, 'ACCEPTED'),
-- Team EcomHub
(27, 9,  5, NOW(), 25, 'ACCEPTED'),
(27, 11, 5, NOW(), 25, 'ACCEPTED'),
(27, 13, 5, NOW(), 25, 'ACCEPTED'),
(27, 15, 5, NOW(), 25, 'ACCEPTED'),
-- Team TradeWave
(28, 17, 5, NOW(), 29, 'ACCEPTED'),
(28, 19, 5, NOW(), 29, 'ACCEPTED'),
(28, 35, 5, NOW(), 29, 'ACCEPTED'),
(28, 25, 5, NOW(), 29, 'ACCEPTED');

-- CS404 Assignment 6 Team Members (3 per team)
INSERT INTO team_members (team_id, user_id, assignment_id, joined_at, invited_by, status) VALUES
-- Team CloudNine
(29, 6,  6, NOW(), 6,  'ACCEPTED'),
(29, 9,  6, NOW(), 6,  'ACCEPTED'),
(29, 12, 6, NOW(), 6,  'ACCEPTED'),
-- Team Kubernetes
(30, 7,  6, NOW(), 9,  'ACCEPTED'),
(30, 10, 6, NOW(), 9,  'ACCEPTED'),
(30, 13, 6, NOW(), 9,  'ACCEPTED'),
-- Team Serverless
(31, 15, 6, NOW(), 12, 'ACCEPTED'),
(31, 18, 6, NOW(), 12, 'ACCEPTED'),
(31, 21, 6, NOW(), 12, 'ACCEPTED'),
-- Team MicroStack
(32, 16, 6, NOW(), 18, 'ACCEPTED'),
(32, 19, 6, NOW(), 18, 'ACCEPTED'),
(32, 27, 6, NOW(), 18, 'ACCEPTED'),
-- Team CloudForge
(33, 30, 6, NOW(), 24, 'ACCEPTED'),
(33, 33, 6, NOW(), 24, 'ACCEPTED'),
(33, 24, 6, NOW(), 24, 'ACCEPTED');

-- CS405 Assignment 7 Team Members (3 per team)
INSERT INTO team_members (team_id, user_id, assignment_id, joined_at, invited_by, status) VALUES
-- Team MobileFirst
(34, 8,  7, NOW(), 8,  'ACCEPTED'),
(34, 10, 7, NOW(), 8,  'ACCEPTED'),
(34, 12, 7, NOW(), 8,  'ACCEPTED'),
-- Team AppCrafters
(35, 11, 7, NOW(), 11, 'ACCEPTED'),
(35, 14, 7, NOW(), 11, 'ACCEPTED'),
(35, 16, 7, NOW(), 11, 'ACCEPTED'),
-- Team ReactNative
(36, 18, 7, NOW(), 18, 'ACCEPTED'),
(36, 20, 7, NOW(), 18, 'ACCEPTED'),
(36, 22, 7, NOW(), 18, 'ACCEPTED'),
-- Team FlutterDev
(37, 24, 7, NOW(), 24, 'ACCEPTED'),
(37, 26, 7, NOW(), 24, 'ACCEPTED'),
(37, 28, 7, NOW(), 24, 'ACCEPTED'),
-- Team CrossPlat
(38, 30, 7, NOW(), 30, 'ACCEPTED'),
(38, 32, 7, NOW(), 30, 'ACCEPTED'),
(38, 34, 7, NOW(), 30, 'ACCEPTED'),
-- Team AppBuilder
(39, 7,  7, NOW(), 26, 'ACCEPTED'),
(39, 9,  7, NOW(), 26, 'ACCEPTED'),
(39, 13, 7, NOW(), 26, 'ACCEPTED'),
-- Team MobiTech
(40, 15, 7, NOW(), 32, 'ACCEPTED'),
(40, 17, 7, NOW(), 32, 'ACCEPTED'),
(40, 19, 7, NOW(), 32, 'ACCEPTED');

-- ================================================
-- VERIFY SEED DATA
-- ================================================
SELECT 'Users'             AS table_name, COUNT(*) AS total FROM users
UNION ALL
SELECT 'Instructors',      COUNT(*) FROM users WHERE role = 'INSTRUCTOR'
UNION ALL
SELECT 'Students',         COUNT(*) FROM users WHERE role = 'STUDENT'
UNION ALL
SELECT 'Courses',          COUNT(*) FROM courses
UNION ALL
SELECT 'Course Instructors', COUNT(*) FROM course_instructors
UNION ALL
SELECT 'Enrollments',      COUNT(*) FROM course_enrollments
UNION ALL
SELECT 'Assignments',      COUNT(*) FROM assignments
UNION ALL
SELECT 'Rubric Criteria',  COUNT(*) FROM rubric_criteria
UNION ALL
SELECT 'Teams',            COUNT(*) FROM teams
UNION ALL
SELECT 'Team Members',     COUNT(*) FROM team_members;
