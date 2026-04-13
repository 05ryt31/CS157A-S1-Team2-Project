/* ============================================================
   FlowChain
   - creates database and 10 tables
   - seed realistic mock data
   ============================================================ */

CREATE DATABASE IF NOT EXISTS flowchain;
USE flowchain;

/* ------------------------------------------------------------
   1) CREATE TABLES (10)
   ------------------------------------------------------------ */

CREATE TABLE IF NOT EXISTS organizations (
  org_id INT PRIMARY KEY,
  org_name VARCHAR(120) NOT NULL,
  org_type VARCHAR(60) NOT NULL,
  phone VARCHAR(20),
  status VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
  user_id INT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(180) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS orgmembers (
  org_id INT NOT NULL,
  user_id INT NOT NULL,
  member_role VARCHAR(40),
  PRIMARY KEY (org_id, user_id),
  CONSTRAINT fk_orgmembers_org
    FOREIGN KEY (org_id) REFERENCES organizations(org_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_orgmembers_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS location (
  location_id INT PRIMARY KEY,
  org_id INT NOT NULL,
  address VARCHAR(200) NOT NULL,
  city VARCHAR(80) NOT NULL,
  zip VARCHAR(12) NOT NULL,
  CONSTRAINT fk_location_org
    FOREIGN KEY (org_id) REFERENCES organizations(org_id)
    ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS foodcategories (
  category_id INT PRIMARY KEY,
  category_name VARCHAR(80) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS listings (
  listing_id INT PRIMARY KEY,
  org_id INT NOT NULL,
  location_id INT NOT NULL,
  title VARCHAR(150) NOT NULL,
  description TEXT,
  created_at DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL,
  CONSTRAINT fk_listings_org
    FOREIGN KEY (org_id) REFERENCES organizations(org_id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_listings_location
    FOREIGN KEY (location_id) REFERENCES location(location_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS listingitems (
  listing_item_id INT PRIMARY KEY,
  listing_id INT NOT NULL,
  category_id INT NOT NULL,
  quantity INT NOT NULL,
  unit VARCHAR(20) NOT NULL,
  expiry_date DATE NOT NULL,
  CONSTRAINT fk_listingitems_listing
    FOREIGN KEY (listing_id) REFERENCES listings(listing_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_listingitems_category
    FOREIGN KEY (category_id) REFERENCES foodcategories(category_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS claims (
  claim_id INT PRIMARY KEY,
  listing_id INT NOT NULL,
  org_id INT NOT NULL, /* recipient org */
  claimed_at DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL,
  CONSTRAINT fk_claims_listing
    FOREIGN KEY (listing_id) REFERENCES listings(listing_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_claims_org
    FOREIGN KEY (org_id) REFERENCES organizations(org_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS pickups (
  pickup_id INT PRIMARY KEY,
  claim_id INT UNIQUE NOT NULL, /* 1:1 claim -> pickup */
  scheduled_time DATETIME NOT NULL,
  pickup_status VARCHAR(20) NOT NULL,
  completed_time DATETIME NULL,
  CONSTRAINT fk_pickups_claim
    FOREIGN KEY (claim_id) REFERENCES claims(claim_id)
    ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS auditlogs (
  log_id INT PRIMARY KEY,
  user_id INT NOT NULL,
  action_type VARCHAR(30) NOT NULL,
  entity_type VARCHAR(30) NOT NULL,
  entity_id INT NOT NULL,
  action_time DATETIME NOT NULL,
  CONSTRAINT fk_auditlogs_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE CASCADE ON UPDATE CASCADE
);