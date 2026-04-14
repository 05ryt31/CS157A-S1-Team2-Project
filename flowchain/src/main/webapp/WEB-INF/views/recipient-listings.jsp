<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Browse Listings" />
<%@ include file="header.jspf" %>

<main class="dashboard-page">
  <div class="container">
    <h1 class="dashboard-title">Browse Open Listings</h1>
    <p class="dashboard-sub">
      Search currently available food listings by title, category, and city.
    </p>

    <form method="get" action="${pageContext.request.contextPath}/recipient/listings" class="search-panel">
      <div class="filter-grid">
        <div class="form-row">
          <label for="q">Search</label>
          <input type="text" id="q" name="q" placeholder="Search by title or donor"
                 value="<c:out value='${q}' />">
        </div>

        <div class="form-row">
          <label for="category">Category</label>
          <select id="category" name="category" class="filter-select">
            <option value="">All categories</option>
            <c:forEach var="cat" items="${categories}">
              <option value="${cat.categoryName}"
                <c:if test="${selectedCategory == cat.categoryName}">selected</c:if>>
                <c:out value="${cat.categoryName}" />
              </option>
            </c:forEach>
          </select>
        </div>

        <div class="form-row">
          <label for="city">City</label>
          <select id="city" name="city" class="filter-select">
            <option value="">All cities</option>
            <c:forEach var="cname" items="${cities}">
              <option value="${cname}"
                <c:if test="${selectedCity == cname}">selected</c:if>>
                <c:out value="${cname}" />
              </option>
            </c:forEach>
          </select>
        </div>
      </div>

      <div class="filter-actions">
        <button type="submit" class="btn btn-primary">Search</button>
        <a href="${pageContext.request.contextPath}/recipient/listings" class="btn btn-outline">Clear</a>
      </div>
    </form>

    <div class="results-summary">
      <strong><c:out value="${fn:length(listings)}" /></strong> open listing(s) found.
    </div>

    <c:choose>
      <c:when test="${empty listings}">
        <div class="dashboard-placeholder">
          <p>No listings matched your filters.</p>
        </div>
      </c:when>
      <c:otherwise>
        <div class="listings-grid">
          <c:forEach var="listing" items="${listings}">
            <div class="listing-card">
              <div class="listing-card-header">
                <h3><c:out value="${listing.title}" /></h3>
                <span class="status-pill status-open">
                  <c:out value="${listing.status}" />
                </span>
              </div>

              <p class="listing-org"><c:out value="${listing.orgName}" /></p>

              <div class="listing-details">
                <span><strong>City:</strong> <c:out value="${listing.city}" /></span>
                <span><strong>Qty:</strong> <c:out value="${listing.totalQuantity}" /></span>
                <span><strong>Expires:</strong> <c:out value="${listing.earliestExpiry}" /></span>
              </div>

              <p class="listing-categories">
                <strong>Categories:</strong>
                <c:out value="${listing.categories}" />
              </p>

              <a href="${pageContext.request.contextPath}/recipient/listing?id=${listing.listingId}"
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