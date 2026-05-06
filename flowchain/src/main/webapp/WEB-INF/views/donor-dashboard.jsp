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
      You are signed in as a <strong>Donor</strong>. Manage listings, review claims, and complete scheduled pickups.
    </p>

    <div class="dashboard-actions">
      <a href="${pageContext.request.contextPath}/donor/listing/new" class="btn btn-primary">
        Create Listing
      </a>
      <a href="${pageContext.request.contextPath}/donor/listings" class="btn btn-outline">
        My Listings
      </a>
      <a href="${pageContext.request.contextPath}/donor/profile" class="btn btn-outline">
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
      <h2 class="section-title section-title-left">Claims and Pickups</h2>

      <c:choose>
        <c:when test="${empty claims}">
          <div class="dashboard-placeholder">
            <p>No claims have been submitted for your listings yet.</p>
          </div>
        </c:when>

        <c:otherwise>
          <div class="items-list">
            <c:forEach var="claim" items="${claims}">
              <div class="item-row">
                <div>
                  <p class="item-category">
                    <a href="${pageContext.request.contextPath}/donor/listing?id=${claim.listingId}">
                      <c:out value="${claim.listingTitle}" />
                    </a>
                  </p>

                  <p class="item-meta">
                    Recipient: <c:out value="${claim.recipientOrgName}" />
                  </p>

                  <p class="item-meta">
                    Claimed At: <c:out value="${claim.claimedAt}" />
                  </p>

                  <p>
                    <strong>Claim Status:</strong>
                    <span class="status-pill status-default">
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
                        <span class="status-pill status-default">
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
                  <c:if test="${claim.claimStatus == 'APPROVED' and claim.pickupStatus == 'SCHEDULED'}">
                    <div class="dashboard-actions">
                      <form method="post" action="${pageContext.request.contextPath}/donor/pickup">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="claimId" value="${claim.claimId}">
                        <input type="hidden" name="action" value="pickedUp">
                        <button type="submit" class="btn btn-primary btn-sm">
                          Picked Up
                        </button>
                      </form>

                      <form method="post" action="${pageContext.request.contextPath}/donor/pickup">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="claimId" value="${claim.claimId}">
                        <input type="hidden" name="action" value="noShow">
                        <button type="submit" class="btn btn-outline btn-sm">
                          No Show
                        </button>
                      </form>
                    </div>
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