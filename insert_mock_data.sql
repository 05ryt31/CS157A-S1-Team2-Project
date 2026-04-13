/* ------------------------------------------------------------
   SEED MOCK DATA
   ------------------------------------------------------------ */

/* ORGANIZATIONS (10) */
INSERT INTO organizations (org_id, org_name, org_type, phone, status) VALUES
(1,'Safeway - San Jose (Almaden)','Grocery Store','408-555-1101','ACTIVE'),
(2,'Trader Joe''s - San Jose','Grocery Store','408-555-1102','ACTIVE'),
(3,'Whole Foods Market - The Alameda','Grocery Store','408-555-1103','ACTIVE'),
(4,'Panera Bread - Downtown SJ','Restaurant','408-555-1104','ACTIVE'),
(5,'Ike''s Love & Sandwiches - SJ','Restaurant','408-555-1105','ACTIVE'),
(6,'Second Harvest of Silicon Valley','Food Bank','408-555-1106','ACTIVE'),
(7,'Sacred Heart Community Service','Food Pantry','408-555-1107','ACTIVE'),
(8,'CityTeam San Jose','Shelter','408-555-1108','ACTIVE'),
(9,'Bill Wilson Center','Shelter','408-555-1109','ACTIVE'),
(10,'SJSU Spartan Food Pantry','Pantry','408-555-1110','ACTIVE');

/* USERS (14) */
INSERT INTO users (user_id, name, email, password, role, created_at) VALUES
(101,'Ryuto Kawabata','ryuto.kawabata@sjsu.edu','pw','ADMIN','2026-02-20 09:10:00'),
(102,'Yug More','yugamol.more@sjsu.edu','pw','ADMIN','2026-02-20 09:12:00'),
(103,'Marl Jonson','marlfarris.jonson@sjsu.edu','pw','ADMIN','2026-02-20 09:15:00'),
(104,'Ava Chen','ava.chen@safeway.example','pw','DONOR','2026-02-21 10:00:00'),
(105,'Noah Patel','noah.patel@traderjoes.example','pw','DONOR','2026-02-21 10:05:00'),
(106,'Mia Lopez','mia.lopez@wholefoods.example','pw','DONOR','2026-02-22 08:30:00'),
(107,'Jordan Kim','jordan.kim@panera.example','pw','DONOR','2026-02-22 09:10:00'),
(108,'Priya Singh','priya.singh@ikes.example','pw','DONOR','2026-02-22 09:30:00'),
(109,'Ethan Brooks','ethan.brooks@shfb.example','pw','RECIPIENT','2026-02-23 11:00:00'),
(110,'Sophia Nguyen','sophia.nguyen@sacredheart.example','pw','RECIPIENT','2026-02-23 11:10:00'),
(111,'Liam Johnson','liam.johnson@cityteam.example','pw','RECIPIENT','2026-02-24 13:00:00'),
(112,'Olivia Park','olivia.park@billwilson.example','pw','RECIPIENT','2026-02-24 13:20:00'),
(113,'Carlos Rivera','carlos.rivera@spartanpantry.example','pw','RECIPIENT','2026-02-24 14:00:00'),
(114,'Hannah Wright','hannah.wright@shfb.example','pw','RECIPIENT','2026-02-25 08:40:00');

/* ORGMEMBERS (14) */
INSERT INTO orgmembers (org_id, user_id, member_role) VALUES
(6,101,'SYSTEM_ADMIN'),
(6,102,'SYSTEM_ADMIN'),
(6,103,'SYSTEM_ADMIN'),
(1,104,'DONATION_COORDINATOR'),
(2,105,'STORE_MANAGER'),
(3,106,'STORE_MANAGER'),
(4,107,'SHIFT_MANAGER'),
(5,108,'OWNER'),
(6,109,'INTAKE_COORDINATOR'),
(7,110,'PANTRY_MANAGER'),
(8,111,'OPERATIONS'),
(9,112,'CASE_MANAGER'),
(10,113,'PANTRY_MANAGER'),
(6,114,'INTAKE_SPECIALIST');

/* LOCATION (12) */
INSERT INTO location (location_id, org_id, address, city, zip) VALUES
(201,1,'6477 Almaden Expy','San Jose','95120'),
(202,2,'635 Coleman Ave','San Jose','95110'),
(203,3,'777 The Alameda','San Jose','95126'),
(204,4,'86 S 1st St','San Jose','95113'),
(205,5,'2211 The Alameda','San Jose','95126'),
(206,6,'750 Curtner Ave','San Jose','95125'),
(207,6,'590 Wool Creek Dr','San Jose','95112'),
(208,7,'1381 S First St','San Jose','95110'),
(209,8,'580 Charles St','San Jose','95112'),
(210,9,'3490 The Alameda','San Jose','95117'),
(211,10,'1 Washington Sq','San Jose','95112'),
(212,1,'1708 Branham Ln','San Jose','95118');

/* FOOD CATEGORIES (10) */
INSERT INTO foodcategories (category_id, category_name) VALUES
(301,'Produce'),
(302,'Dairy'),
(303,'Bakery'),
(304,'Prepared Meals'),
(305,'Canned Goods'),
(306,'Beverages'),
(307,'Meat/Protein'),
(308,'Frozen'),
(309,'Snacks'),
(310,'Grains/Pasta');

/* LISTINGS (12) */
INSERT INTO listings (listing_id, org_id, location_id, title, description, created_at, status) VALUES
(401,1,201,'Surplus Produce Crates','Mixed apples, bananas, lettuce, and carrots.','2026-03-10 17:45:00','OPEN'),
(402,1,212,'Dairy Close-to-Date','Milk, yogurt, and cheese near best-by date.','2026-03-11 09:05:00','OPEN'),
(403,2,202,'Bakery End-of-Day','Assorted bread and pastries (same-day).','2026-03-11 20:10:00','OPEN'),
(404,3,203,'Prepared Deli Meals','Packaged deli meals; keep refrigerated.','2026-03-12 08:40:00','OPEN'),
(405,4,204,'Sandwiches (Lunch Overrun)','Boxed sandwiches from lunch service.','2026-03-12 14:10:00','OPEN'),
(406,5,205,'Catering Leftovers','Sealed trays of sandwiches and chips.','2026-03-09 18:30:00','CLOSED'),
(407,2,202,'Beverage Multipack','Unopened juices and bottled water.','2026-03-10 12:20:00','OPEN'),
(408,3,203,'Frozen Items','Frozen veggies and ready-to-heat meals.','2026-03-10 10:15:00','OPEN'),
(409,1,201,'Canned Goods Bundle','Canned beans, soup, and tomatoes.','2026-03-08 11:00:00','CLOSED'),
(410,4,204,'Breakfast Pastries','Bagels and muffins (morning overage).','2026-03-12 11:05:00','OPEN'),
(411,5,205,'Snack Boxes','Individual snack packs, sealed.','2026-03-11 16:35:00','OPEN'),
(412,3,203,'Meat/Protein Pack','Chicken and tofu (refrigerated).','2026-03-11 13:00:00','OPEN');

/* LISTING ITEMS (24) */
INSERT INTO listingitems (listing_item_id, listing_id, category_id, quantity, unit, expiry_date) VALUES
(451,401,301,60,'lbs','2026-03-14'),
(452,401,301,40,'lbs','2026-03-13'),
(453,402,302,40,'units','2026-03-15'),
(454,402,302,25,'units','2026-03-16'),
(455,403,303,80,'units','2026-03-12'),
(456,403,303,50,'units','2026-03-12'),
(457,404,304,35,'units','2026-03-13'),
(458,404,306,20,'units','2026-04-01'),
(459,405,304,45,'units','2026-03-12'),
(460,405,309,30,'units','2026-06-01'),
(461,406,304,30,'units','2026-03-10'),
(462,406,309,30,'units','2026-06-01'),
(463,407,306,48,'units','2026-05-15'),
(464,407,306,24,'units','2026-06-15'),
(465,408,308,40,'units','2026-09-01'),
(466,408,308,25,'units','2026-09-10'),
(467,409,305,90,'units','2027-01-15'),
(468,409,305,60,'units','2027-02-10'),
(469,410,303,70,'units','2026-03-13'),
(470,411,309,100,'units','2026-08-01'),
(471,411,306,20,'units','2026-06-15'),
(472,412,307,30,'lbs','2026-03-13'),
(473,412,307,20,'lbs','2026-03-14'),
(474,412,304,12,'units','2026-03-13');

/* CLAIMS (12) */
INSERT INTO claims (claim_id, listing_id, org_id, claimed_at, status) VALUES
(501,401,6,'2026-03-10 18:10:00','APPROVED'),
(502,401,7,'2026-03-10 18:25:00','PENDING'),
(503,402,8,'2026-03-11 09:30:00','APPROVED'),
(504,403,10,'2026-03-11 20:25:00','APPROVED'),
(505,404,6,'2026-03-12 09:05:00','APPROVED'),
(506,405,7,'2026-03-12 14:35:00','REJECTED'),
(507,407,9,'2026-03-10 12:45:00','APPROVED'),
(508,408,8,'2026-03-10 10:35:00','PENDING'),
(509,410,10,'2026-03-12 11:20:00','APPROVED'),
(510,411,6,'2026-03-11 17:00:00','APPROVED'),
(511,412,8,'2026-03-11 13:20:00','APPROVED'),
(512,403,7,'2026-03-11 20:40:00','REJECTED');

/* PICKUPS (8) */
INSERT INTO pickups (pickup_id, claim_id, scheduled_time, pickup_status, completed_time) VALUES
(601,501,'2026-03-11 09:00:00','COMPLETED','2026-03-11 09:18:00'),
(602,503,'2026-03-12 12:30:00','IN_PROGRESS',NULL),
(603,504,'2026-03-12 08:00:00','SCHEDULED',NULL),
(604,505,'2026-03-12 15:15:00','SCHEDULED',NULL),
(605,507,'2026-03-11 13:30:00','COMPLETED','2026-03-11 13:55:00'),
(606,509,'2026-03-12 16:45:00','SCHEDULED',NULL),
(607,510,'2026-03-12 10:00:00','MISSED',NULL),
(608,511,'2026-03-12 14:00:00','COMPLETED','2026-03-12 14:22:00');

/* AUDITLOGS (18) */
INSERT INTO auditlogs (log_id, user_id, action_type, entity_type, entity_id, action_time) VALUES
(701,104,'CREATE','LISTING',401,'2026-03-10 17:45:10'),
(702,104,'CREATE','LISTING',402,'2026-03-11 09:05:15'),
(703,105,'CREATE','LISTING',403,'2026-03-11 20:10:11'),
(704,106,'CREATE','LISTING',404,'2026-03-12 08:40:09'),
(705,107,'CREATE','LISTING',405,'2026-03-12 14:10:06'),
(706,108,'CREATE','LISTING',411,'2026-03-11 16:35:05'),
(707,109,'SUBMIT','CLAIM',501,'2026-03-10 18:10:02'),
(708,110,'SUBMIT','CLAIM',502,'2026-03-10 18:25:13'),
(709,111,'SUBMIT','CLAIM',503,'2026-03-11 09:30:22'),
(710,113,'SUBMIT','CLAIM',504,'2026-03-11 20:25:10'),
(711,109,'SUBMIT','CLAIM',505,'2026-03-12 09:05:07'),
(712,110,'SUBMIT','CLAIM',506,'2026-03-12 14:35:33'),
(713,101,'APPROVE','CLAIM',501,'2026-03-10 18:40:00'),
(714,102,'APPROVE','CLAIM',503,'2026-03-11 09:55:00'),
(715,103,'APPROVE','CLAIM',504,'2026-03-11 20:50:00'),
(716,103,'SCHEDULE','PICKUP',601,'2026-03-10 18:55:00'),
(717,103,'SCHEDULE','PICKUP',602,'2026-03-11 10:05:00'),
(718,103,'SCHEDULE','PICKUP',608,'2026-03-12 13:35:00');