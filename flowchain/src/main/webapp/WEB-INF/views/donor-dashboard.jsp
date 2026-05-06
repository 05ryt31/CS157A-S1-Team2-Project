<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Donor Dashboard" />
<%@ include file="header.jspf" %>

<main class="dashboard-page">
  <div class="container">
    <h1 class="dashboard-title">
      Welcome, <c:out value="${sessionScope.fullName}" />
    </h1>
    <p class="dashboard-sub">
      You are signed in as a <strong>Donor</strong>. Create and manage surplus food listings.
    </p>

    <div class="dashboard-actions">
      <a href="${pageContext.request.contextPath}/donor/listings/new"
         class="btn btn-primary">
        Create Listing
      </a>
      <a href="${pageContext.request.contextPath}/donor/profile"
         class="btn btn-outline">
        Manage Organization
      </a>
      <a href="${pageContext.request.contextPath}/account"
         class="btn btn-outline">
        Account Settings
      </a>
    </div>

    <div class="dashboard-placeholder">
      <p>Use the Create Listing button to start adding surplus food donations.</p>
    </div>
  </div>
</main>

<%@ include file="footer.jspf" %>