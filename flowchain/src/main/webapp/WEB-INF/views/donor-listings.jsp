<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="My Listings" />
<%@ include file="header.jspf" %>

<main class="dashboard-page">
  <div class="container">

    <h1 class="dashboard-title">My Listings</h1>

    <div class="dashboard-actions">
      <a href="${pageContext.request.contextPath}/donor/listings/new"
         class="btn btn-primary">
        Create Listing
      </a>
    </div>

    <c:choose>
      <c:when test="${empty listings}">
        <div class="dashboard-placeholder">
          <p>No listings created yet.</p>
        </div>
      </c:when>

      <c:otherwise>
        <div class="listings-grid">
          <c:forEach var="l" items="${listings}">
            <div class="listing-card">

              <div class="listing-card-header">
                <h3><c:out value="${l.title}" /></h3>
                <span class="status-pill">
                  <c:out value="${l.status}" />
                </span>
              </div>

              <div class="listing-details">
                <span><strong>Total Qty:</strong> <c:out value="${l.totalQuantity}" /></span>
                <span><strong>Earliest Exp:</strong> <c:out value="${l.earliestExpiry}" /></span>
              </div>

              <a href="${pageContext.request.contextPath}/donor/listings/detail?id=${l.listingId}"
                 class="btn btn-outline btn-sm">
                View Details
              </a>

            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>

  </div>
</main>

<%@ include file="footer.jspf" %>