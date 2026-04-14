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
      You are signed in as a <strong>Recipient</strong>. Use the listings browser to search available food donations and view details.
    </p>

    <div class="dashboard-actions">
      <a href="${pageContext.request.contextPath}/recipient/listings" class="btn btn-primary btn-lg">
        Browse Listings
      </a>
    </div>

    <div class="dashboard-placeholder">
      <p>Your claim and pickup tools can be added here next.</p>
    </div>
  </div>
</main>

<%@ include file="footer.jspf" %>