<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Donor Listing Detail" />
<%@ include file="header.jspf" %>

<main class="dashboard-page">
  <div class="container">
    <a href="${pageContext.request.contextPath}/donor/listings"
       class="btn btn-outline btn-sm detail-back-btn">
      Back to My Listings
    </a>

    <section class="detail-card">
      <div class="detail-header">
        <div>
          <h1 class="dashboard-title detail-title">
            <c:out value="${listing.title}" />
          </h1>
          <p class="dashboard-sub detail-sub">
            Created: <c:out value="${listing.createdAt}" />
          </p>
        </div>

        <span class="status-pill ${listing.status == 'OPEN' ? 'status-open' : 'status-closed'}">
          <c:out value="${listing.status}" />
        </span>
      </div>

      <div class="detail-grid">
        <div class="detail-block">
          <h3>Listing Information</h3>
          <p><strong>Description:</strong> <c:out value="${listing.description}" /></p>
        </div>

        <div class="detail-block">
          <h3>Pickup Location</h3>
          <p><strong>Address:</strong> <c:out value="${listing.address}" /></p>
          <p><strong>City:</strong> <c:out value="${listing.city}" /></p>
          <p><strong>ZIP:</strong> <c:out value="${listing.zip}" /></p>
        </div>
      </div>

      <c:if test="${listing.status == 'OPEN'}">
        <%-- donor can cancel an open listing --%>
        <form method="post" action="${pageContext.request.contextPath}/donor/listings/detail"
              class="dashboard-actions">
          <input type="hidden" name="csrfToken" value="${csrfToken}">
          <input type="hidden" name="action" value="cancel">
          <input type="hidden" name="listingId" value="${listing.listingId}">
          <button type="submit" class="btn btn-outline">Cancel Listing</button>
        </form>
      </c:if>
    </section>

    <section class="detail-card">
      <h2 class="section-title section-title-left">Items</h2>

      <c:choose>
        <c:when test="${empty items}">
          <div class="dashboard-placeholder">
            <p>No item details available.</p>
          </div>
        </c:when>

        <c:otherwise>
          <div class="items-list">
            <%-- display each item in this listing --%>
            <c:forEach var="item" items="${items}">
              <div class="item-row">
                <div>
                  <p class="item-category"><c:out value="${item.categoryName}" /></p>
                  <p class="item-meta">
                    Quantity: <c:out value="${item.quantity}" /> <c:out value="${item.unit}" />
                  </p>
                </div>
                <div class="item-expiry">
                  Expires: <c:out value="${item.expiryDate}" />
                </div>
              </div>
            </c:forEach>
          </div>
        </c:otherwise>
      </c:choose>
    </section>
  </div>
</main>

<%@ include file="footer.jspf" %>