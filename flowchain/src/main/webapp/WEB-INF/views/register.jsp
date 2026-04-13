<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Register" />
<%@ include file="header.jspf" %>

<main class="auth-page">
  <div class="auth-card auth-card-wide">
    <h1 class="auth-title">
      Register as
      <c:out value="${role == 'RECIPIENT' ? 'Recipient' : 'Donor'}" />
    </h1>
    <p class="auth-sub">
      Create an account for your organization to start
      <c:out value="${role == 'RECIPIENT' ? 'claiming' : 'posting'}" /> surplus food.
    </p>

    <c:if test="${not empty errors['_global']}">
      <div class="form-error-banner">
        <c:out value="${errors['_global']}" />
      </div>
    </c:if>

    <form action="${pageContext.request.contextPath}/register" method="post" class="auth-form" novalidate>
      <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
      <input type="hidden" name="role"      value="<c:out value='${role}' />">

      <fieldset class="form-section">
        <legend>Organization</legend>

        <div class="form-row">
          <label for="orgName">Organization name</label>
          <input type="text" id="orgName" name="orgName" maxlength="120" required
                 value="<c:out value='${form.orgName}' />">
          <c:if test="${not empty errors.orgName}">
            <span class="form-error"><c:out value="${errors.orgName}" /></span>
          </c:if>
        </div>

        <div class="form-row">
          <label for="orgType">Organization type</label>
          <input type="text" id="orgType" name="orgType" maxlength="60"
                 placeholder="${role == 'RECIPIENT' ? 'Food Bank, Pantry, Shelter…' : 'Grocery Store, Restaurant…'}"
                 value="<c:out value='${form.orgType}' />">
        </div>

        <div class="form-row">
          <label for="phone">Phone</label>
          <input type="tel" id="phone" name="phone" maxlength="20"
                 placeholder="408-555-0100"
                 value="<c:out value='${form.phone}' />">
        </div>

        <div class="form-row">
          <label for="address">Address</label>
          <input type="text" id="address" name="address" maxlength="200" required
                 value="<c:out value='${form.address}' />">
          <c:if test="${not empty errors.address}">
            <span class="form-error"><c:out value="${errors.address}" /></span>
          </c:if>
        </div>

        <div class="form-grid-2">
          <div class="form-row">
            <label for="city">City</label>
            <input type="text" id="city" name="city" maxlength="80" required
                   value="<c:out value='${form.city}' />">
            <c:if test="${not empty errors.city}">
              <span class="form-error"><c:out value="${errors.city}" /></span>
            </c:if>
          </div>
          <div class="form-row">
            <label for="zip">ZIP</label>
            <input type="text" id="zip" name="zip" maxlength="12" required
                   value="<c:out value='${form.zip}' />">
            <c:if test="${not empty errors.zip}">
              <span class="form-error"><c:out value="${errors.zip}" /></span>
            </c:if>
          </div>
        </div>
      </fieldset>

      <fieldset class="form-section">
        <legend>Account</legend>

        <div class="form-row">
          <label for="fullName">Your full name</label>
          <input type="text" id="fullName" name="fullName" maxlength="120" required
                 value="<c:out value='${form.fullName}' />">
          <c:if test="${not empty errors.fullName}">
            <span class="form-error"><c:out value="${errors.fullName}" /></span>
          </c:if>
        </div>

        <div class="form-row">
          <label for="email">Email</label>
          <input type="email" id="email" name="email" maxlength="180" required
                 value="<c:out value='${form.email}' />">
          <c:if test="${not empty errors.email}">
            <span class="form-error"><c:out value="${errors.email}" /></span>
          </c:if>
        </div>

        <div class="form-grid-2">
          <div class="form-row">
            <label for="password">Password</label>
            <input type="password" id="password" name="password"
                   minlength="8" maxlength="100" required>
            <c:if test="${not empty errors.password}">
              <span class="form-error"><c:out value="${errors.password}" /></span>
            </c:if>
          </div>
          <div class="form-row">
            <label for="confirmPassword">Confirm password</label>
            <input type="password" id="confirmPassword" name="confirmPassword"
                   minlength="8" maxlength="100" required>
            <c:if test="${not empty errors.confirmPassword}">
              <span class="form-error"><c:out value="${errors.confirmPassword}" /></span>
            </c:if>
          </div>
        </div>
      </fieldset>

      <button type="submit" class="btn btn-primary btn-lg auth-submit">Create account</button>

      <p class="auth-alt">
        Already have an account?
        <a href="${pageContext.request.contextPath}/login">Log in</a>
      </p>
    </form>
  </div>
</main>

<%@ include file="footer.jspf" %>
