Course pack: Full teaching materials for "Task Management Platform"

This folder contains a full course pack intended for use in a multi-hour classroom or lab session. Files:

- syllabus.md — session-by-session syllabus with learning objectives
- lecture_setup.md — step-by-step setup for student machines (Windows and Linux)
- lecture_architecture.md — deep dive on microservices architecture used by the project
- lab1_create_api.md — full hands-on lab to implement POST /tasks (student instructions)
- solutions/lab1_solution.md — full solution source snippets and instructions
- security.md — JWT-based security integration explained and implemented
- ci_cd.md — GitHub Actions workflows for CI, building and generating the course PDF

How to use:
- Provide this folder to students before the lab.
- Ask students to follow `lecture_setup.md` before class to ensure environments are ready.
- Use `lab1_create_api.md` as a timed exercise; provide `solutions/lab1_solution.md` afterwards for review.

Convert to PDF: use Pandoc or CI workflow (see `ci_cd.md`).
