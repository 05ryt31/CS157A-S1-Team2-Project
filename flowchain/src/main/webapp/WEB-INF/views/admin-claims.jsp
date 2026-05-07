<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="Admin Claims" />
<%@ include file="header.jspf" %>

<main class="dashboard-page">

  <div class="container">

    <h1 class="dashboard-title">
      Admin Claim Management
    </h1>

    <p class="dashboard-sub">
      View and manage all recipient claims.
    </p>

    <div class="listings-grid">

      <c:forEach var="claim" items="${claims}">

        <div class="listing-card">

          <div class="listing-card-header">

            <h3>
              <c:out value="${claim.listingTitle}" />
            </h3>

            <span class="status-pill
              ${claim.status == 'APPROVED' ? 'status-approved' :
                claim.status == 'PENDING' ? 'status-pending' :
                claim.status == 'REQUESTED' ? 'status-pending' :
                claim.status == 'REJECTED' ? 'status-rejected' :
                claim.status == 'CANCELLED' ? 'status-rejected' : 'status-default'}">
              <c:out value="${claim.status}" />
            </span>

          </div>

          <p>
            <strong>Donor:</strong>
            <c:out value="${claim.donorOrg}" />
          </p>

          <p>
            <strong>Recipient:</strong>
            <c:out value="${claim.recipientOrg}" />
          </p>

          <p>
            <strong>Claim ID:</strong>
            <c:out value="${claim.claimId}" />
          </p>

          <c:if test="${claim.status == 'REQUESTED'}">

            <div class="filter-actions">

              <form method="post"
                    action="${pageContext.request.contextPath}/donor/claim-action">

                <input type="hidden"
                       name="csrfToken"
                       value="<c:out value='${sessionScope.csrfToken}' />">

                <input type="hidden"
                       name="claimId"
                       value="<c:out value='${claim.claimId}' />">

                <input type="hidden"
                       name="action"
                       value="approve">

                <button type="submit"
                        class="btn btn-primary btn-sm">

                  Approve

                </button>

              </form>

              <form method="post"
                    action="${pageContext.request.contextPath}/donor/claim-action">

                <input type="hidden"
                       name="csrfToken"
                       value="<c:out value='${sessionScope.csrfToken}' />">

                <input type="hidden"
                       name="claimId"
                       value="<c:out value='${claim.claimId}' />">

                <input type="hidden"
                       name="action"
                       value="reject">

                <button type="submit"
                        class="btn btn-outline btn-sm">

                  Reject

                </button>

              </form>

            </div>

          </c:if>

        </div>

      </c:forEach>

    </div>

  </div>

</main>

<%@ include file="footer.jspf" %>