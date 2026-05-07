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
      You are signed in as a <strong>Recipient</strong>. Browse listings, submit claims, and schedule pickups after approval.
    </p>

    <div class="dashboard-actions">
      <a href="${pageContext.request.contextPath}/recipient/listings" class="btn btn-primary">
        Browse Listings
      </a>
      <a href="${pageContext.request.contextPath}/recipient/profile" class="btn btn-outline">
        Manage Organization
      </a>
      <a href="${pageContext.request.contextPath}/account" class="btn btn-outline">
        Account Settings
      </a>
    </div>

    <c:if test="${not empty success}">
      <div class="dashboard-placeholder">
        <p><strong><c:out value="${success}" /></strong></p>
      </div>
    </c:if>

    <c:if test="${not empty error}">
      <div class="dashboard-placeholder">
        <p><strong><c:out value="${error}" /></strong></p>
      </div>
    </c:if>

    <section class="detail-card">
      <h2 class="section-title section-title-left">My Claims and Pickups</h2>

      <c:choose>
        <c:when test="${empty myClaims}">
          <div class="dashboard-placeholder">
            <p>You have not submitted any claims yet.</p>
          </div>
        </c:when>

        <c:otherwise>
          <div class="items-list">
            <c:forEach var="claim" items="${myClaims}">
              <div class="item-row">
                <div>
                  <p class="item-category">
                    <a href="${pageContext.request.contextPath}/recipient/listing?id=${claim.listingId}">
                      <c:out value="${claim.listingTitle}" />
                    </a>
                  </p>

                  <p class="item-meta">
                    Donor: <c:out value="${claim.donorOrgName}" />
                  </p>

                  <p class="item-meta">
                    Claimed At: <c:out value="${claim.claimedAt}" />
                  </p>

                  <p>
                    <strong>Claim Status:</strong>
                    <span class="status-pill
                      ${claim.claimStatus == 'APPROVED' ? 'status-approved' :
                        claim.claimStatus == 'PENDING' ? 'status-pending' :
                        claim.claimStatus == 'REJECTED' ? 'status-rejected' :
                        claim.claimStatus == 'CANCELLED' ? 'status-rejected' : 'status-default'}">
                      <c:out value="${claim.claimStatus}" />
                    </span>
                  </p>

                  <c:choose>
                    <c:when test="${empty claim.pickupStatus}">
                      <p><strong>Pickup:</strong> Not scheduled</p>
                    </c:when>
                    <c:otherwise>
                      <p>
                        <strong>Pickup Status:</strong>
                        <span class="status-pill
                          ${claim.pickupStatus == 'COMPLETED' ? 'status-completed' :
                            claim.pickupStatus == 'SCHEDULED' ? 'status-scheduled' :
                            claim.pickupStatus == 'PICKED_UP' ? 'status-completed' :
                            claim.pickupStatus == 'NO_SHOW' ? 'status-rejected' : 'status-default'}">
                          <c:out value="${claim.pickupStatus}" />
                        </span>
                      </p>
                      <p><strong>Scheduled Time:</strong> <c:out value="${claim.scheduledTime}" /></p>
                      <c:if test="${not empty claim.completedTime}">
                        <p><strong>Completed Time:</strong> <c:out value="${claim.completedTime}" /></p>
                      </c:if>
                    </c:otherwise>
                  </c:choose>
                </div>

                <div>
                  <c:if test="${claim.claimStatus == 'APPROVED' and
                                (empty claim.pickupStatus or claim.pickupStatus == 'SCHEDULED')}">
                    <form method="post" action="${pageContext.request.contextPath}/recipient/pickup">
                      <input type="hidden" name="csrfToken" value="${csrfToken}">
                      <input type="hidden" name="claimId" value="${claim.claimId}">
                      <input type="hidden" name="action" value="schedule">

                      <div class="form-row">
                        <label for="scheduledTime${claim.claimId}">
                          <c:choose>
                            <c:when test="${empty claim.pickupStatus}">Schedule Pickup</c:when>
                            <c:otherwise>Reschedule Pickup</c:otherwise>
                          </c:choose>
                        </label>
                        <input type="datetime-local"
                               id="scheduledTime${claim.claimId}"
                               name="scheduledTime"
                               required>
                      </div>

                      <button type="submit" class="btn btn-primary btn-sm">
                        Save Pickup Time
                      </button>
                    </form>
                  </c:if>
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