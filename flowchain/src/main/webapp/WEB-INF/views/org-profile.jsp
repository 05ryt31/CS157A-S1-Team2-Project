<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Organization Profile" />
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

    <h1 class="dashboard-title">Organization Profile</h1>
    <p class="dashboard-sub">
      Manage your organization's information.
      Fields marked with * are required.
    </p>

    <%-- Global error --%>
    <c:if test="${not empty errors['_global']}">
      <div class="form-error-banner">
        <c:out value="${errors['_global']}" />
      </div>
    </c:if>

    <%-- Determine correct form action URL --%>
    <c:choose>
      <c:when test="${isAdmin and not empty param.id}">
        <c:set var="formAction" value="${pageContext.request.contextPath}/admin/org?id=${param.id}" />
      </c:when>
      <c:when test="${sessionScope.role == 'RECIPIENT'}">
        <c:set var="formAction" value="${pageContext.request.contextPath}/recipient/profile" />
      </c:when>
      <c:otherwise>
        <c:set var="formAction" value="${pageContext.request.contextPath}/donor/profile" />
      </c:otherwise>
    </c:choose>

    <form action="${formAction}" method="post" class="auth-form" novalidate>
      <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">

      <%-- Organization section --%>
      <fieldset class="form-section">
        <legend>Organization</legend>

        <div class="form-row">
          <label for="orgName">Organization name *</label>
          <input type="text" id="orgName" name="orgName" maxlength="120" required
                 value="<c:out value='${org.orgName}' />">
          <c:if test="${not empty errors.orgName}">
            <span class="form-error"><c:out value="${errors.orgName}" /></span>
          </c:if>
        </div>

        <div class="form-row">
          <label for="orgType">Organization type</label>
          <input type="text" id="orgType" name="orgType" maxlength="60"
                 value="<c:out value='${org.orgType}' />">
        </div>

        <div class="form-row">
          <label for="phone">Phone</label>
          <input type="tel" id="phone" name="phone" maxlength="20"
                 value="<c:out value='${org.phone}' />">
        </div>

        <div class="form-row">
          <label>Status</label>
          <input type="text" disabled
                 value="<c:out value='${org.status}' />"
                 class="input-readonly">
        </div>
      </fieldset>

      <%-- Location section --%>
      <fieldset class="form-section">
        <legend>Location</legend>

        <c:if test="${not empty location}">
          <input type="hidden" name="locationId" value="${location.locationId}">
        </c:if>

        <div class="form-row">
          <label for="address">Address *</label>
          <input type="text" id="address" name="address" maxlength="200" required
                 value="<c:out value='${location.address}' />">
          <c:if test="${not empty errors.address}">
            <span class="form-error"><c:out value="${errors.address}" /></span>
          </c:if>
        </div>

        <div class="form-grid-2">
          <div class="form-row">
            <label for="city">City *</label>
            <input type="text" id="city" name="city" maxlength="80" required
                   value="<c:out value='${location.city}' />">
            <c:if test="${not empty errors.city}">
              <span class="form-error"><c:out value="${errors.city}" /></span>
            </c:if>
          </div>
          <div class="form-row">
            <label for="zip">ZIP *</label>
            <input type="text" id="zip" name="zip" maxlength="12" required
                   value="<c:out value='${location.zip}' />">
            <c:if test="${not empty errors.zip}">
              <span class="form-error"><c:out value="${errors.zip}" /></span>
            </c:if>
          </div>
        </div>
      </fieldset>

      <%-- Read-only account section --%>
      <fieldset class="form-section">
        <legend>Account (read-only)</legend>

        <div class="form-row">
          <label>Email</label>
          <input type="email" disabled
                 value="<c:out value='${userEmail}' />"
                 class="input-readonly">
        </div>
      </fieldset>

      <button type="submit" class="btn btn-primary btn-lg auth-submit">Save changes</button>
    </form>

    <%-- Members table --%>
    <section class="profile-members">
      <h2 class="section-title section-title-left">Organization Members</h2>

      <c:choose>
        <c:when test="${empty members}">
          <div class="dashboard-placeholder">
            <p>No members found.</p>
          </div>
        </c:when>
        <c:otherwise>
          <div class="members-table-wrap">
            <table class="members-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="m" items="${members}">
                  <tr>
                    <td><c:out value="${m.name}" /></td>
                    <td><c:out value="${m.email}" /></td>
                    <td><c:out value="${m.memberRole}" /></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </c:otherwise>
      </c:choose>
    </section>

  </div>
</main>

<%@ include file="footer.jspf" %>
