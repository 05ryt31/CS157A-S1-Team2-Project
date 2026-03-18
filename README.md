# FlowChain

# CS 157A Team 2 Project
- **Course:** CS 157A (Introduction to Database Management Systems)
- **Instructor:** Mike Wu
- **Semester:** Spring 2026

## Contributors
- Ryuto Kawabata (`05ryt31`)
- Yug More (`Yug-More`)
- Marl Jonson (`marljonson`)

## Project Overview
FlowChain is a database management system designed to reduce food waste by connecting food donors with organizations that can redistribute surplus food.

The platform enables donor organizations such as restaurants and grocery stores to post available food, and recipient organizations such as food banks and shelters to claim and manage these donations efficiently.

---

## Problem
A significant amount of food is wasted daily while many communities face food insecurity. Current redistribution efforts are often unstructured and lack proper tracking systems.

FlowChain addresses this issue by providing a centralized and structured database system to manage food donations, claims, and organizational interactions.

---

## System Features
- User registration and organization membership  
- Creation and management of food listings by donor organizations  
- Viewing and claiming of available food by recipient organizations  
- Tracking the lifecycle of a donation (available → claimed → completed)  
- Role-based participation through users and organizations  

---

## Database Design

### Main Entities

**Users**
- Stores all registered users in the system  
- Attributes: user_id (PK), name, email, password, role, created_at  

**Organizations**
- Represents entities such as restaurants, grocery stores, food banks, and shelters  
- Attributes: org_id (PK), org_name, org_type, phone, status  

**OrgMembers**
- Associative entity linking users and organizations  
- Supports many-to-many relationship  
- Attributes: org_id (PK, FK), user_id (PK, FK), member_role  

**FoodListings**
- Represents surplus food posted by donor organizations  
- Attributes: listing_id (PK), org_id (FK), title, description, quantity, expiry_time, status  

**Claims**
- Represents requests made by recipient organizations for food listings  
- Attributes: claim_id (PK), listing_id (FK), org_id (FK), claim_time, status  

---

## Relationships

- A **User belongs to one or more Organizations** (via OrgMembers)  
- An **Organization has many Users**  
- An **Organization creates many FoodListings**  
- A **FoodListing belongs to one Organization**  
- An **Organization can claim multiple FoodListings**  
- A **FoodListing can be claimed by one Organization (at a time)**  

---

## Constraints and Assumptions
- Each user must belong to at least one organization  
- Only donor-type organizations can create food listings  
- Only recipient-type organizations can claim listings  
- A food listing can only be claimed once at a time  
- Status fields are used to track progress (e.g., available, claimed, completed)  

---

## Technologies Used
- SQL for database creation and queries  
- ER modeling for system design  
- Relational schema design for normalization and data integrity  

---

## How the System Works
1. A user registers and is associated with an organization  
2. A donor organization creates a food listing  
3. Recipient organizations browse available listings  
4. A claim is made for a listing  
5. The system updates the listing and claim status until completion  

---

## Purpose of the Project
This project was developed as part of a Database Management Systems course. The goal is to apply concepts such as:
- Entity Relationship (ER) modeling  
- Normalization  
- Relational schema design  
- Constraints and relationships  

to a real-world problem.

---

## Future Improvements
- Add delivery and logistics tracking  
- Build a user-friendly web interface  
- Implement notifications for new listings  
- Add rating and feedback between organizations  
- Improve scalability and performance for large datasets  
