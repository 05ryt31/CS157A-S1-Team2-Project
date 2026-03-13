/* === XML PARSER UTILITY === */

function parseXML(xmlString) {
  const parser = new DOMParser();
  return parser.parseFromString(xmlString, "application/xml");
}

function getTagText(element, tagName) {
  const node = element.getElementsByTagName(tagName)[0];
  return node ? node.textContent : "";
}

/* === MOCK XML DATA === */

// TODO: Replace mockListingsXML with fetch('/api/listings.xml')
const mockListingsXML = `<?xml version="1.0" encoding="UTF-8"?>
<listings>
  <listing>
    <listing_id>1</listing_id>
    <title>Fresh Bread &amp; Pastries</title>
    <org_name>Bay Area Bakery</org_name>
    <category>Bakery</category>
    <quantity>30</quantity>
    <unit>boxes</unit>
    <expiry_date>2025-04-10</expiry_date>
    <city>San Jose</city>
    <status>Open</status>
  </listing>
  <listing>
    <listing_id>2</listing_id>
    <title>Organic Mixed Vegetables</title>
    <org_name>Green Valley Farm</org_name>
    <category>Produce</category>
    <quantity>50</quantity>
    <unit>lbs</unit>
    <expiry_date>2025-04-08</expiry_date>
    <city>Santa Clara</city>
    <status>Open</status>
  </listing>
  <listing>
    <listing_id>3</listing_id>
    <title>Whole Milk &amp; Yogurt</title>
    <org_name>Valley Dairy Co-op</org_name>
    <category>Dairy</category>
    <quantity>20</quantity>
    <unit>gallons</unit>
    <expiry_date>2025-04-07</expiry_date>
    <city>Milpitas</city>
    <status>Open</status>
  </listing>
  <listing>
    <listing_id>4</listing_id>
    <title>Prepared Meal Trays</title>
    <org_name>Campus Dining Hall</org_name>
    <category>Prepared</category>
    <quantity>15</quantity>
    <unit>trays</unit>
    <expiry_date>2025-04-06</expiry_date>
    <city>San Jose</city>
    <status>Open</status>
  </listing>
  <listing>
    <listing_id>5</listing_id>
    <title>Canned Soups &amp; Beans</title>
    <org_name>FoodMart Warehouse</org_name>
    <category>Canned</category>
    <quantity>100</quantity>
    <unit>cans</unit>
    <expiry_date>2025-06-15</expiry_date>
    <city>Sunnyvale</city>
    <status>Open</status>
  </listing>
  <listing>
    <listing_id>6</listing_id>
    <title>Assorted Juice Boxes</title>
    <org_name>Beverage Depot</org_name>
    <category>Beverages</category>
    <quantity>60</quantity>
    <unit>packs</unit>
    <expiry_date>2025-05-20</expiry_date>
    <city>Campbell</city>
    <status>Open</status>
  </listing>
</listings>`;

// TODO: Replace mockStatsXML with fetch('/api/stats.xml')
const mockStatsXML = `<?xml version="1.0" encoding="UTF-8"?>
<stats>
  <stat><label>Food Redistributed</label><value>1,200+ lbs</value></stat>
  <stat><label>Active Listings</label><value>48</value></stat>
  <stat><label>Verified Organizations</label><value>30+</value></stat>
</stats>`;

/* === CATEGORY BADGE MAPPING === */

const categoryBadgeClass = {
  bakery: "badge-bakery",
  produce: "badge-produce",
  dairy: "badge-dairy",
  prepared: "badge-prepared",
  canned: "badge-canned",
  beverages: "badge-beverages",
};

function getBadgeClass(category) {
  const key = category.toLowerCase();
  return categoryBadgeClass[key] || "badge-default";
}

/* === RENDER LISTINGS === */

function renderListings() {
  const grid = document.getElementById("listingsGrid");
  if (!grid) return;

  const xmlDoc = parseXML(mockListingsXML);
  const items = xmlDoc.getElementsByTagName("listing");

  const fragment = document.createDocumentFragment();

  Array.from(items).forEach(function (item) {
    const title = getTagText(item, "title");
    const orgName = getTagText(item, "org_name");
    const category = getTagText(item, "category");
    const quantity = getTagText(item, "quantity");
    const unit = getTagText(item, "unit");
    const expiryDate = getTagText(item, "expiry_date");
    const city = getTagText(item, "city");

    const card = document.createElement("div");
    card.className = "listing-card";

    card.innerHTML =
      '<div class="listing-card-header">' +
        "<h3>" + escapeHTML(title) + "</h3>" +
        '<span class="badge ' + getBadgeClass(category) + '">' + escapeHTML(category) + "</span>" +
      "</div>" +
      '<p class="listing-org">' + escapeHTML(orgName) + "</p>" +
      '<div class="listing-details">' +
        "<span>" + escapeHTML(quantity) + " " + escapeHTML(unit) + "</span>" +
        "<span>Expires: " + escapeHTML(expiryDate) + "</span>" +
        "<span>" + escapeHTML(city) + "</span>" +
      "</div>" +
      '<a href="#" class="btn btn-outline btn-sm">View Details</a>';

    fragment.appendChild(card);
  });

  grid.appendChild(fragment);
}

/* === RENDER STATS === */

function renderStats() {
  var grid = document.getElementById("statsGrid");
  if (!grid) return;

  var xmlDoc = parseXML(mockStatsXML);
  var items = xmlDoc.getElementsByTagName("stat");

  var fragment = document.createDocumentFragment();

  Array.from(items).forEach(function (item) {
    var label = getTagText(item, "label");
    var value = getTagText(item, "value");

    var div = document.createElement("div");
    div.className = "stat-item";

    div.innerHTML =
      '<span class="stat-value">' + escapeHTML(value) + "</span>" +
      '<span class="stat-label">' + escapeHTML(label) + "</span>";

    fragment.appendChild(div);
  });

  grid.appendChild(fragment);
}

/* === ESCAPE UTILITY === */

function escapeHTML(str) {
  var div = document.createElement("div");
  div.appendChild(document.createTextNode(str));
  return div.innerHTML;
}

/* === NAVBAR SCROLL BEHAVIOR === */

function initNavbarScroll() {
  var navbar = document.getElementById("navbar");
  if (!navbar) return;

  window.addEventListener("scroll", function () {
    if (window.scrollY > 40) {
      navbar.classList.add("scrolled");
    } else {
      navbar.classList.remove("scrolled");
    }
  });
}

/* === MOBILE NAV TOGGLE === */

function initMobileNav() {
  var toggle = document.getElementById("navToggle");
  var links = document.getElementById("navLinks");
  var actions = document.getElementById("navActions");
  if (!toggle || !links || !actions) return;

  toggle.addEventListener("click", function () {
    links.classList.toggle("active");
    actions.classList.toggle("active");
  });

  // Close menu when a link is clicked
  links.querySelectorAll("a").forEach(function (link) {
    link.addEventListener("click", function () {
      links.classList.remove("active");
      actions.classList.remove("active");
    });
  });
}

/* === INIT === */

document.addEventListener("DOMContentLoaded", function () {
  renderListings();
  renderStats();
  initNavbarScroll();
  initMobileNav();
});
