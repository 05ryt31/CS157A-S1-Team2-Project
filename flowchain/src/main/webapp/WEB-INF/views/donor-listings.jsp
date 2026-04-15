<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Manage Listings" />
<%@ include file="header.jspf" %>

<main class="dashboard-page">
  <div class="container">
    <div class="dashboard-actions">
      <a href="${pageContext.request.contextPath}/donor/listings/new" class="btn btn-primary">Create new listing</a>
    </div>

    <h1 class="dashboard-title">Your Listings</h1>
    <p class="dashboard-sub">View and manage all listings posted by your organization.</p>

    <c:if test="${empty listings}">
      <div class="dashboard-placeholder">
        <p>You have not created any listings yet. Start by creating a new listing.</p>
      </div>
    </c:if>

    <c:if test="${not empty listings}">
      <div class="listings-grid">
        <c:forEach var="listing" items="${listings}">
          <div class="listing-card">
            <div class="listing-card-header">
              <h3><c:out value="${listing.title}" /></h3>
              <c:choose>
                <c:when test="${listing.status == 'OPEN'}">
                  <span class="status-pill status-open"><c:out value="${listing.status}" /></span>
                </c:when>
                <c:otherwise>
                  <span class="status-pill status-closed"><c:out value="${listing.status}" /></span>
                </c:otherwise>
              </c:choose>
            </div>
            <p class="listing-org"><strong>Location:</strong> <c:out value="${listing.city}" /></p>
            <p class="listing-org"><strong>Address:</strong> <c:out value="${listing.address}" />, <c:out value="${listing.zip}" /></p>
            <div class="listing-details">
              <span><strong>Qty:</strong> <c:out value="${listing.totalQuantity}" /></span>
              <span><strong>Expires:</strong> <c:out value="${listing.earliestExpiry}" /></span>
              <span><strong>Posted:</strong> <c:out value="${listing.createdAt}" /></span>
            </div>
            <div class="listing-actions">
              <c:if test="${listing.status == 'OPEN'}">
                <form method="post" action="${pageContext.request.contextPath}/donor/listings" class="inline-form">
                  <input type="hidden" name="csrfToken" value="${csrfToken}">
                  <input type="hidden" name="action" value="cancel">
                  <input type="hidden" name="listingId" value="${listing.listingId}">
                  <button type="submit" class="btn btn-outline btn-sm">Cancel listing</button>
                </form>
              </c:if>
            </div>
          </div>
        </c:forEach>
      </div>
    </c:if>
  </div>
</main>

<%@ include file="footer.jspf" %>
