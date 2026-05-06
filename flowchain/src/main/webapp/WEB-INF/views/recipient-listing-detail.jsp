<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Listing Detail" />
<%@ include file="header.jspf" %>

<main class="dashboard-page">
  <div class="container">
    <a href="${pageContext.request.contextPath}/recipient/listings" class="btn btn-outline btn-sm detail-back-btn">
      Back to Listings
    </a>

    <section class="detail-card">
      <div class="detail-header">
        <div>
          <h1 class="dashboard-title detail-title">
            <c:out value="${listing.title}" />
          </h1>
          <p class="dashboard-sub detail-sub">
            Posted by <strong><c:out value="${listing.orgName}" /></strong>
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
          <p><strong>Created:</strong> <c:out value="${listing.createdAt}" /></p>
        </div>

        <div class="detail-block">
          <h3>Pickup Location</h3>
          <p><strong>Address:</strong> <c:out value="${listing.address}" /></p>
          <p><strong>City:</strong> <c:out value="${listing.city}" /></p>
          <p><strong>ZIP:</strong> <c:out value="${listing.zip}" /></p>
          <p><strong>Phone:</strong> <c:out value="${listing.phone}" /></p>
        </div>
      </div>
    </section>

    <section class="detail-card">
      <h2 class="section-title section-title-left">Items in this Listing</h2>

      <c:choose>
        <c:when test="${empty items}">
          <div class="dashboard-placeholder">
            <p>No item details available.</p>
          </div>
        </c:when>
        <c:otherwise>
          <div class="items-list">
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

    <section class="detail-card">
      <h2 class="section-title section-title-left">My Claim / Pickup Status</h2>

      <c:choose>
        <c:when test="${empty myClaim}">
          <div class="dashboard-placeholder">
            <p>You have not submitted a claim for this listing yet.</p>

            <c:if test="${listing.status == 'OPEN'}">
              <form method="post" action="${pageContext.request.contextPath}/recipient/claim">
                <input type="hidden" name="listingId" value="${listing.listingId}" />
                <button type="submit" class="btn btn-primary">
                  Claim Listing
                </button>
              </form>
            </c:if>
          </div>
        </c:when>

        <c:otherwise>
          <div class="detail-grid">
            <div class="detail-block">
              <h3>Claim</h3>
              <p>
                <strong>Status:</strong>
                <span class="status-pill
                  ${myClaim.claimStatus == 'APPROVED' ? 'status-approved' :
                    myClaim.claimStatus == 'PENDING' ? 'status-pending' :
                    myClaim.claimStatus == 'REJECTED' ? 'status-rejected' :
                    myClaim.claimStatus == 'CANCELLED' ? 'status-rejected' : 'status-default'}">
                  <c:out value="${myClaim.claimStatus}" />
                </span>
              </p>
              <p><strong>Claimed At:</strong> <c:out value="${myClaim.claimedAt}" /></p>

              <c:if test="${myClaim.claimStatus == 'PENDING' || myClaim.claimStatus == 'APPROVED'}">
                <form method="post" action="${pageContext.request.contextPath}/recipient/claim/cancel">
                  <input type="hidden" name="claimId" value="${myClaim.claimId}" />
                  <button type="submit" class="btn btn-danger">
                    Cancel Claim
                  </button>
                </form>
              </c:if>
            </div>

            <div class="detail-block">
              <h3>Pickup</h3>
              <c:choose>
                <c:when test="${empty myClaim.pickupStatus}">
                  <p>No pickup scheduled yet.</p>
                </c:when>
                <c:otherwise>
                  <p>
                    <strong>Status:</strong>
                    <span class="status-pill
                      ${myClaim.pickupStatus == 'COMPLETED' ? 'status-completed' :
                        myClaim.pickupStatus == 'SCHEDULED' ? 'status-scheduled' :
                        myClaim.pickupStatus == 'IN_PROGRESS' ? 'status-progress' :
                        myClaim.pickupStatus == 'MISSED' ? 'status-rejected' : 'status-default'}">
                      <c:out value="${myClaim.pickupStatus}" />
                    </span>
                  </p>
                  <p><strong>Scheduled Time:</strong> <c:out value="${myClaim.scheduledTime}" /></p>
                  <c:if test="${not empty myClaim.completedTime}">
                    <p><strong>Completed Time:</strong> <c:out value="${myClaim.completedTime}" /></p>
                  </c:if>
                </c:otherwise>
              </c:choose>
            </div>
          </div>
        </c:otherwise>
      </c:choose>
    </section>
  </div>
</main>

<%@ include file="footer.jspf" %>