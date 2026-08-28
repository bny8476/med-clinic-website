# Main Clinic Website (Project 1) & Ancillary Services (Project 2)

A modular healthcare management platform featuring a React/Vite frontend SPA and independent Spring Boot backend deployments.

---

## System Overview & Architecture

The application is structured into two independent service deployments:

### Project 1 – Main Clinic Project (Primary Web & Backend)
The core operational platform for clinic management.
* **Frontend**: Complete React/Vite SPA (UI, components, pages, workflows intact).
* **Backend Modules**:
  * User Authentication & Authorization, Role-Based Access Control (RBAC)
  * Admin & Super Admin Management
  * Doctor Management
  * Patient Management
  * Appointment Booking & Scheduling
  * Reception & Front Desk Management
  * Nurse Management
  * OP/IP Patient Workflow
  * EMR & Electronic Medical Records
  * Patient Medical History
  * Prescription Management
  * Pharmacy Management & Integration
  * Laboratory Management
  * Lab Test Requests & Reports
  * HR & Employee Management
  * Inpatient Management
  * Surgery Management
  * Radiology Management
  * Notifications & Communication

### Project 2 – Ancillary & Intelligence Services
Specialized secondary microservices operating independently from core workflows:
* AI Assistant & Clinical Guidance (`ai`)
* Clinical Decision Support, Care Pathways & Order Sets (`clinicaldecision`)
* FHIR Interoperability & Integration (`fhir`)
* Emergency Services (`emergency`)
* Finance & Billing (`finance`, `billing`)
* Home Visits (`homevisit`)
* Insurance Claims (`insurance`)
* Search & Discovery (`search`)
* Support & Ticketing (`support`)
* Audit Logging (`audit`)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.4.1 |
| Persistence | Spring Data JPA + Flyway Migrations |
| Security | Spring Security (JWT - JJWT 0.11.5) |
| Database | PostgreSQL |
| Frontend | React 19 + Vite 8 |

---

## Repository Layout

```
clinic-website/
├── backend/                  ← Project 1: Main Clinic Backend
│   ├── pom.xml
│   └── src/main/java/com/healthcare/clinic/
├── clinic-ancillary-backend/ ← Project 2: Ancillary & Intelligence Backend
│   └── src/
├── frontend/                 ← Project 1 Frontend (React/Vite SPA)
│   └── src/
├── docker-compose.yml
└── README.md
```

---

## Running Locally

### 1. Start Database
```bash
docker-compose up -d postgres
```

### 2. Start Project 1 Backend (Main Clinic Service)
```bash
cd backend
mvn spring-boot:run
```
The backend serves API requests at `http://localhost:8080/api`.

### 3. Start Frontend (Project 1 SPA)
```bash
cd frontend
npm install
npm run dev
```
The frontend runs at `http://localhost:5173`.

---

## Deployment (Render)

Project 1 and Project 2 feature separate, independent Render deployment configs.
* **Project 1 Deployment**: Deploys frontend SPA and core clinic backend service (`backend`).
* **Project 2 Deployment**: Deploys ancillary and intelligence services (`clinic-ancillary-backend`).
