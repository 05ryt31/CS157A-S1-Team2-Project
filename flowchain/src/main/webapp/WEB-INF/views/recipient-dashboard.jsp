<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Recipient Dashboard" />
<%@ include file="header.jspf" %>

<main class="dashboard-page">
  <div class="container">
    
    <h1 class="dashboard-title">
      Welcome, <c:out value="${sessionScope.fullName}" />
    </h1>

    <p class="dashboard-sub">
      You are signed in as a <strong>Recipient</strong>. Browse available food listings, apply filters, and view detailed information before submitting claims.
    </p>

    <!-- Main Actions -->
    <div class="dashboard-actions">
      <a href="${pageContext.request.contextPath}/recipient/listings"
         class="btn btn-primary btn-lg">
        Browse Listings
      </a>
      <a href="${pageContext.request.contextPath}/recipient/profile"
         class="btn btn-outline">
        Manage Organization
      </a>
      <a href="${pageContext.request.contextPath}/account"
         class="btn btn-outline">
        Account Settings
      </a>
    </div>

    <!-- Info Section -->
    <div class="dashboard-info">
      <h3>What you can do</h3>
      <ul class="dashboard-list">
        <li>Search listings by title or organization</li>
        <li>Filter listings by category and city</li>
        <li>View listing details including quantity and expiry</li>
        <li>Track availability of open listings</li>
      </ul>
    </div>

    <!-- Placeholder for future -->
    <div class="dashboard-placeholder">
      <p>Claim submission and pickup tracking features can be added here next.</p>
    </div>

  </div>
</main>

<%@ include file="footer.jspf" %>