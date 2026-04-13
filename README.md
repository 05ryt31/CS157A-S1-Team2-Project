# FlowChain
## CS 157A Team 2 Project

**Course:** CS 157A (Introduction to Database Management Systems)
**Instructor:** Mike Wu
**Semester:** Spring 2026

---

## Contributors
- Ryuto Kawabata (05ryt31)
- Yug More (Yug-More)
- Marl Jonson (marljonson)

---

## Project Overview
FlowChain is a database management system designed to reduce food waste by connecting food donors with organizations that can redistribute surplus food.

The platform allows donor organizations such as restaurants and grocery stores to post available food, while recipient organizations such as food banks and shelters can claim and manage these donations efficiently.

---

## Problem
A significant amount of food is wasted daily while many communities face food insecurity. Current redistribution efforts are often unstructured and lack proper tracking systems.

FlowChain addresses this problem by providing a centralized database system that organizes food listings, claims, and pickups in a structured way.

---

## System Features
- User registration and organization membership
- Creation and management of food listings by donor organizations
- Support for multiple food items within a listing
- Categorization of food items
- Viewing and claiming of available food by recipient organizations
- Pickup scheduling and tracking
- Audit logging for system activities
- Tracking the lifecycle of a donation (available → claimed → completed)

---

## Database Design

### Main Entities

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

## ERD for FlowChain
<img width="600" alt="Screenshot 2026-03-17 at 6 51 51 PM" src="https://github.com/user-attachments/assets/14730203-200d-4a69-ad84-9e7015db324e" />

---

## Constraints and Assumptions
- Each user must belong to at least one organization
- Only donor-type organizations can create listings
- Only recipient-type organizations can submit claims
- A listing can only be finalized by one claim at a time
- Each claim is associated with at most one pickup
- Status fields are used to track lifecycle stages

---

## Technologies Used
- **Database:** MySQL 8.x (managed via MySQL Workbench)
- **Backend:** Java 11, Servlet API 4.0, JSP, JSTL 1.2 (Tomcat 9)
- **Build:** Apache Maven (WAR packaging)
- **Auth:** jBCrypt password hashing, session-based login with CSRF tokens
- **Frontend:** Static HTML/CSS, vanilla JS (XML-driven listings preview)

---

## Local Setup

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

### Auth Endpoints (current sprint)
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

## How the System Works
1. A user registers and joins an organization
2. A donor organization creates a food listing
3. Listings include multiple items with quantity and expiry details
4. Recipient organizations browse listings and submit claims
5. Donors review and approve claims
6. A pickup is scheduled and tracked until completion

---

## Purpose of the Project
This project was developed as part of a Database Management Systems course. The goal is to apply concepts such as:

- Entity Relationship (ER) modeling
- Normalization
- Relational schema design
- Constraints and relationships

to a real-world problem related to food waste management.

---

## Future Improvements
- Add delivery and logistics tracking
- Build a user interface
- Implement notifications for new listings
- Add rating and feedback system
- Improve scalability for larger datasets
