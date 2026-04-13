<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Log In" />
<%@ include file="header.jspf" %>

<main class="auth-page">
  <div class="auth-card">
    <h1 class="auth-title">Log In</h1>
    <p class="auth-sub">Welcome back to FlowChain.</p>

    <c:if test="${not empty error}">
      <div class="form-error-banner">
        <c:out value="${error}" />
      </div>
    </c:if>

    <form action="${pageContext.request.contextPath}/login" method="post" class="auth-form" novalidate>
      <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
      <c:if test="${not empty next}">
        <input type="hidden" name="next" value="<c:out value='${next}' />">
      </c:if>

      <div class="form-row">
        <label for="email">Email</label>
        <input type="email" id="email" name="email" maxlength="180" required autofocus
               value="<c:out value='${email}' />">
      </div>

      <div class="form-row">
        <label for="password">Password</label>
        <input type="password" id="password" name="password" maxlength="100" required>
      </div>

      <button type="submit" class="btn btn-primary btn-lg auth-submit">Log in</button>

      <p class="auth-alt">
        New to FlowChain?
        <a href="${pageContext.request.contextPath}/register">Register</a>
      </p>
    </form>
  </div>
</main>

<%@ include file="footer.jspf" %>
