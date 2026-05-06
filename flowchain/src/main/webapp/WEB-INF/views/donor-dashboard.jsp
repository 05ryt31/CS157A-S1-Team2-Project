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
      You are signed in as a <strong>Donor</strong>. Here's where you'll
      manage surplus food listings.
    </p>

    <div class="dashboard-actions">
      <a href="${pageContext.request.contextPath}/donor/profile"
         class="btn btn-outline">
        Manage Organization
      </a>

      <a href="${pageContext.request.contextPath}/account"
         class="btn btn-outline">
        Account Settings
      </a>
    </div>

    <!-- Pending Claim Requests -->

    <section class="detail-card">

      <h2 class="section-title section-title-left">
        Pending Claim Requests
      </h2>

      <c:choose>

        <c:when test="${empty pendingClaims}">
          <div class="dashboard-placeholder">
            <p>No pending claim requests right now.</p>
          </div>
        </c:when>

        <c:otherwise>

          <div class="items-list">

            <c:forEach var="claim" items="${pendingClaims}">

              <div class="item-row">

                <div>

                  <p class="item-category">
                    <c:out value="${claim.recipientOrgName}" />
                    requested
                    <strong>
                      <c:out value="${claim.title}" />
                    </strong>
                  </p>

                  <p class="item-meta">
                    Requested at:
                    <c:out value="${claim.claimedAt}" />
                  </p>

                </div>

                <div style="display:flex; gap:10px;">

                  <form method="post"
                        action="${pageContext.request.contextPath}/donor/claim/update">

                    <input type="hidden"
                           name="claimId"
                           value="${claim.claimId}" />

                    <button type="submit"
                            name="action"
                            value="approve"
                            class="btn btn-primary btn-sm">

                      Approve

                    </button>

                  </form>

                  <form method="post"
                        action="${pageContext.request.contextPath}/donor/claim/update">

                    <input type="hidden"
                           name="claimId"
                           value="${claim.claimId}" />

                    <button type="submit"
                            name="action"
                            value="reject"
                            class="btn btn-danger btn-sm">

                      Reject

                    </button>

                  </form>

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