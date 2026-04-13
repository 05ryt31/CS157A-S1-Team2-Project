<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Admin Dashboard" />
<%@ include file="header.jspf" %>

<main class="dashboard-page">
  <div class="container">
    <h1 class="dashboard-title">
      Welcome, <c:out value="${sessionScope.fullName}" />
    </h1>
    <p class="dashboard-sub">
      You are signed in as an <strong>Administrator</strong>. Here's
      where you'll verify organizations and review platform activity.
    </p>
    <div class="dashboard-placeholder">
      <p>Admin tools are coming soon.</p>
    </div>
  </div>
</main>

<%@ include file="footer.jspf" %>
