# Candidate CV Processing

A service for uploading PDF resumes, automatically extracting key fields, and managing candidates (viewing, filtering, reporting).

# Features

- Upload resumes in PDF format
- Validate file format and size
- Display upload statuses and errors
- Automatically extract data from CVs
- HR review and edit before saving
- Persist candidates in the database
- View candidate list
- Sort, filter, and tag candidates
- View reports


# Tech Stack

- Backend: Java, Spring Boot, Maven  
- Frontend: React + Vite  
- Database: PostgreSQL  
- CI/CD: GitHub Actions  
- Containerization: Docker, Docker Compose


# Environment Variables

Database (PostgreSQL)
- POSTGRES_DB=recruiting  
- POSTGRES_USER=recruiting  
- POSTGRES_PASSWORD=recruiting

Backend (Spring Boot)
- SPRING_PROFILES_ACTIVE=prod
- SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/recruiting
- SPRING_DATASOURCE_USERNAME=recruiting
- SPRING_DATASOURCE_PASSWORD=recruiting
- APP_STORAGE_BASE_PATH=/data/resumes


# Project Structure

├── .github/
│   └── workflows/
│       └── ci-cd.yml           # CI/CD pipeline (lint/test/deploy)
├── backend/
│   ├── .mvn/                   # Maven Wrapper
│   ├── data/                   # (opt.) seeds/data files/local storage
│   ├── src/
│   │   ├── main/               # application sources (Java/Kotlin)
│   │   └── test/               # unit/integration tests
│   ├── Dockerfile              # backend service image
│   ├── pom.xml                 # Maven project configuration
│   ├── mvnw / mvnw.cmd         # Maven Wrapper scripts
│   └── docker-compose.yml      # local backend deps (if needed)
├── frontend/
│   ├── public/                 # static assets
│   ├── src/                    # frontend sources (components, pages)
│   ├── Dockerfile              # frontend image
│   ├── nginx.conf              # Nginx config (serve/proxy)
│   ├── package.json            # dependencies and scripts
│   └── package-lock.json       # npm lockfile
├── docker-compose.yml          # orchestration of front/back/DB for local dev
└── README.md                   # primary project documentatio


# Installation & Setup

- docker-compose up --build

- Frontend: http://localhost:8085
- Backend: http://backend:8080
  


Login & Password

- Manager credentials:
  login: manager
  password: managerPass

- HR credentials:
  login: hr
  password: hrPass


### Backend (Spring Boot)
- SPRING_PROFILES_ACTIVE=prod
- SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/recruiting
- SPRING_DATASOURCE_USERNAME=recruiting
- SPRING_DATASOURCE_PASSWORD=recruiting
- APP_STORAGE_BASE_PATH=/data/resumes

