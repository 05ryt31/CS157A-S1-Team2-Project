# FlowChain
## CS 157A Team 2 Project

- **Course:** CS 157A (Introduction to Database Management Systems), section 01
- **Instructor:** Mike Wu
- **Semester:** Spring 2026

---

## Contributors
- Ryuto Kawabata (`05ryt31`)
- Yug More (`Yug-More`)
- Marl Jonson (`marljonson`)

---

## Project overview
FlowChain is a database management system designed to reduce food waste by connecting food donors with organizations that can redistribute surplus food.

The platform allows donor organizations such as restaurants and grocery stores to post available food, while recipient organizations such as food banks and shelters can claim and manage these donations efficiently.

---

## Problem
A significant amount of food is wasted daily while many communities face food insecurity. Current redistribution efforts are often unstructured and lack proper tracking systems.

FlowChain addresses this problem by providing a centralized database system that organizes food listings, claims, and pickups in a structured way.

---

## System features
- User registration and organization membership
- Creation and management of food listings by donor organizations
- Support for multiple food items within a listing
- Categorization of food items
- Viewing and claiming of available food by recipient organizations
- Pickup scheduling and tracking
- Audit logging for system activities
- Tracking the lifecycle of a donation (available → claimed → completed)

---

## Database design

### Main entities

**Users**
Stores all registered users in the system
Attributes: `user_id (PK), name, email, password, role, created_at`

**Organizations**
Represents entities such as restaurants, grocery stores, food banks, and shelters
Attributes: `org_id (PK), org_name, org_type, phone, status`

**OrgMembers**
Associative entity linking users and organizations
Attributes: `org_id (PK, FK), user_id (PK, FK), member_role`

**Locations**
Stores address information for organizations
Attributes: `location_id (PK), org_id (FK), address, city, zip`

**FoodCategories**
Defines categories used to classify food items
Attributes: `category_id (PK), category_name`

**Listings**
Represents surplus food listings created by donor organizations
Attributes: `listing_id (PK), org_id (FK), location_id (FK), title, description, created_at, status`

**ListingItems**
Stores individual food items within a listing
Attributes: `listing_item_id (PK), listing_id (FK), category_id (FK), quantity, unit, expiry_date`

**Claims**
Represents requests made by recipient organizations
Attributes: `claim_id (PK), listing_id (FK), org_id (FK), claimed_at, status`

**Pickups**
Stores scheduling and completion details for claims
Attributes: `pickup_id (PK), claim_id (FK), scheduled_time, pickup_status, completed_time`

**AuditLogs**
Tracks important system actions for monitoring
Attributes: `log_id (PK), user_id (FK), action_type, entity_type, entity_id, action_time`

---

### Relationships
- A user belongs to an organization through **OrgMembers**
- An organization can have multiple users
- An organization can have multiple locations
- An organization creates listings
- A listing belongs to one organization and one location
- A listing contains multiple listing items
- Each listing item belongs to a food category
- An organization can submit multiple claims
- A listing can receive multiple claims
- An approved claim results in a pickup
- A user generates audit logs

---

## Entity–relationship diagram for FlowChain
<img width="600" alt="Screenshot 2026-03-17 at 6 51 51 PM" src="https://github.com/user-attachments/assets/14730203-200d-4a69-ad84-9e7015db324e" />

---

## Constraints and assumptions
- Each user must belong to at least one organization
- Only donor-type organizations can create listings
- Only recipient-type organizations can submit claims
- A listing can only be finalized by one claim at a time
- Each claim is associated with at most one pickup
- Status fields are used to track lifecycle stages

---

## Technologies used
- **Database:** MySQL 8.x (managed via MySQL Workbench)
- **Backend:** Java 11, Servlet API 4.0, JSP, JSTL 1.2 (Tomcat 9)
- **Build:** Apache Maven (WAR packaging)
- **Auth:** jBCrypt password hashing, session-based login with CSRF tokens
- **Frontend:** Static HTML/CSS, vanilla JS (XML-driven listings preview)

---

## Local setup

### 1. Database
1. Open MySQL Workbench and connect to your local server.
2. Run `create_tables.sql` to create the `flowchain` schema and tables.
3. (Optional) Run `insert_mock_data.sql` to seed sample data.

### 2. Configure DB credentials
```bash
cd flowchain
cp src/main/resources/db.properties.example src/main/resources/db.properties
# Edit db.properties — set db.user and db.password to your MySQL credentials
```
`db.properties` is gitignored; never commit real credentials.

### 3. Build the WAR
```bash
cd flowchain
mvn clean package
# → target/flowchain.war
```

### 4. Deploy to Tomcat 9
- Copy `flowchain/target/flowchain.war` into Tomcat's `webapps/` directory.
- Start Tomcat (`bin/startup.sh` or `bin/catalina.bat run`).
- Visit <http://localhost:8080/flowchain/>.

### Auth endpoints (current sprint)
| Method | Path | Purpose |
| --- | --- | --- |
| `GET`  | `/register` | Render registration form (accepts `?role=donor\|recipient`) |
| `POST` | `/register` | Create org + location + user + orgmember + audit log; auto-login |
| `GET`  | `/login` | Render login form |
| `POST` | `/login` | Verify bcrypt password, regenerate session, redirect by role |
| `POST` | `/logout` | Invalidate session, write audit log, redirect to home |

After login, users are redirected to `/donor/dashboard`,
`/recipient/dashboard`, or `/admin/dashboard` based on their `role`.
`AuthFilter` enforces these role boundaries.

---

## How the system works
1. A user registers and joins an organization
2. A donor organization creates a food listing
3. Listings include multiple items with quantity and expiry details
4. Recipient organizations browse listings and submit claims
5. Donors review and approve claims
6. A pickup is scheduled and tracked until completion

---

## Purpose of the project
This project was developed as part of our Database Management Systems course. The goal is to apply concepts such an entity–relationship (ER) modeling, normalization, relational schema design, and constraints and relationships to a real-world problem related to food waste management.

---
## Contributions  

### Yug More (`Yug-More`)
Contributed across system design, documentation, frontend development, backend implementation, and deployment. Proposed the FlowChain concept based on research on food waste challenges and helped define the overall system workflow.

Worked on major sections of the project proposal, database design report, and final report, including functional requirements, ER modeling, Project Implementation, Lesson Learned, and Individual Contribution.

Primarily implemented the Recipient and Administrator functionality of the application while also contributing to parts of the Donor workflow. Developed features including listing browsing, searching and filtering, listing detail pages, recipient claim workflows, donor approval and rejection functionality, and administrator claim management. Also contributed to frontend UI development, database configuration, deployment troubleshooting using Apache Tomcat and Maven, and maintenance of the project `README.md`.

Functional requirements (GitHub Issues) owned:
- Food Categories Setup
- Recipient: Browse & Search Listings
- Recipient: Claim a Listing
- Listing Detail Page & Status Display
- Admin Claim Management Dashboard

### Ryuto Kawabata (`05ryt31`)
Contributed to backend development, including implementation of authentication and user management features. Worked on servlets related to user registration and login, and supported integration of database operations with the application.

Functional requirements (GitHub Issues) owned:
- Admin Dashboard & Access Control
- Account Deletion & Password Change
- User Registration & Login (Auth)

### Marl Jonson (`marljonson`)
Contributed to system development and testing, including supporting frontend and backend integration. Revised and polished codebase and non-technical materials (``README.md``, video, reports).

Functional requirements (GitHub Issues) owned:
- Donor: Post and manage listings
- Recipient: Claim a listing
- Pickup Scheduling & Completion

---

## Future improvements to initial plan
### Add delivery and logistics tracking
### Build a mobile-friendly user interface
### Implement notifications for new listings
Currently, FlowChain does not support a notification system (whether via push, email, or in-app) to notify users of status updates in a timely manner.

### Add rating and feedback system
Currently, FlowChain does not support a rating system for food donors and recipients to provide feedback on the services they receive from one another. A review system is crucial aspect of a business-centric platform; for FlowChain, it can validate trust in the platform's users and influence the decisions made by recipients to accept or reject the donations available to them.

### Improve scalability for larger datasets
At the moment, FlowChain is supported on a small number (3) of machines. Our application has the potential to scale by utilizing the power of a cloud computing service such as Amazon Web Services (AWS), Microsoft Azure, or Google Cloud Platform.
