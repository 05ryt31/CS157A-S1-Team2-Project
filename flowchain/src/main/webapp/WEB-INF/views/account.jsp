<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Account Settings" />
<%@ include file="header.jspf" %>

<main class="dashboard-page">
  <div class="container">

    <%-- Back to dashboard --%>
    <c:choose>
      <c:when test="${sessionScope.role == 'RECIPIENT'}">
        <c:set var="dashUrl" value="${pageContext.request.contextPath}/recipient/dashboard" />
      </c:when>
      <c:when test="${sessionScope.role == 'ADMIN'}">
        <c:set var="dashUrl" value="${pageContext.request.contextPath}/admin/dashboard" />
      </c:when>
      <c:otherwise>
        <c:set var="dashUrl" value="${pageContext.request.contextPath}/donor/dashboard" />
      </c:otherwise>
    </c:choose>
    <a href="${dashUrl}" class="btn btn-outline btn-sm detail-back-btn">Back to Dashboard</a>

    <h1 class="dashboard-title">Account Settings</h1>
    <p class="dashboard-sub">
      Manage your password or permanently delete your account.
    </p>

    <%-- Success banner (after password change) --%>
    <c:if test="${not empty successMessage}">
      <div class="form-success-banner">
        <c:out value="${successMessage}" />
      </div>
    </c:if>

    <%-- ====================================================== --%>
    <%-- Change Password                                         --%>
    <%-- ====================================================== --%>
    <div class="auth-card auth-card-wide account-section">
      <h2 class="auth-title">Change Password</h2>
      <p class="auth-sub">
        You'll need your current password to set a new one. Minimum 8 characters.
      </p>

      <c:if test="${not empty passwordError}">
        <div class="form-error-banner">
          <c:out value="${passwordError}" />
        </div>
      </c:if>

      <form action="${pageContext.request.contextPath}/account/password"
            method="post" class="auth-form" novalidate>
        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">

        <div class="form-row">
          <label for="currentPassword">Current password</label>
          <input type="password" id="currentPassword" name="currentPassword"
                 maxlength="100" required autocomplete="current-password">
        </div>

        <div class="form-grid-2">
          <div class="form-row">
            <label for="newPassword">New password</label>
            <input type="password" id="newPassword" name="newPassword"
                   minlength="8" maxlength="100" required autocomplete="new-password">
          </div>
          <div class="form-row">
            <label for="confirmNewPassword">Confirm new password</label>
            <input type="password" id="confirmNewPassword" name="confirmNewPassword"
                   minlength="8" maxlength="100" required autocomplete="new-password">
          </div>
        </div>

        <button type="submit" class="btn btn-primary auth-submit">Update Password</button>
      </form>
    </div>

    <%-- ====================================================== --%>
    <%-- Danger Zone — Delete Account                            --%>
    <%-- ====================================================== --%>
    <div class="auth-card auth-card-wide account-danger-zone">
      <h2 class="auth-title danger-title">Delete Account</h2>
      <p class="auth-sub">
        This will permanently delete your account and remove you from your
        organization. This action <strong>cannot be undone</strong>.
        Your organization itself will not be deleted.
      </p>

      <c:if test="${not empty deleteError}">
        <div class="form-error-banner">
          <c:out value="${deleteError}" />
        </div>
      </c:if>

      <form action="${pageContext.request.contextPath}/account/delete"
            method="post" class="auth-form" novalidate
            onsubmit="return confirm('Are you sure you want to permanently delete your account? This cannot be undone.');">
        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">

        <div class="form-row">
          <label for="confirmPassword">Enter your password to confirm</label>
          <input type="password" id="confirmPassword" name="confirmPassword"
                 maxlength="100" required autocomplete="current-password">
        </div>

        <button type="submit" class="btn btn-danger auth-submit">Delete My Account</button>
      </form>
    </div>

  </div>
</main>

<%@ include file="footer.jspf" %>
