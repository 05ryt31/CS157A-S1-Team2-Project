<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Create Listing" />
<%@ include file="header.jspf" %>

<main class="auth-page">
  <div class="auth-card auth-card-wide">
    <h1 class="auth-title">Create a new listing</h1>
    <p class="auth-sub">Add listing details and at least one food item for your organization.</p>

    <c:if test="${not empty errors['_global']}">
      <div class="form-error-banner">
        <c:out value="${errors['_global']}" />
      </div>
    </c:if>

    <form action="${pageContext.request.contextPath}/donor/listings/new" method="post" class="auth-form" novalidate>
      <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">

      <fieldset class="form-section">
        <legend>Listing details</legend>

        <div class="form-row">
          <label for="title">Title</label>
          <input type="text" id="title" name="title" maxlength="150"
                 value="${fn:escapeXml(form.title)}" required>
          <c:if test="${not empty errors.title}">
            <span class="form-error"><c:out value="${errors.title}" /></span>
          </c:if>
        </div>

        <div class="form-row">
          <label for="description">Description</label>
          <textarea id="description" name="description" rows="4" maxlength="500">${fn:escapeXml(form.description)}</textarea>
        </div>

        <div class="form-row">
          <label for="locationId">Location</label>
          <select id="locationId" name="locationId" required>
            <option value="">Select a location</option>
            <c:forEach var="location" items="${locations}">
              <option value="${location.locationId}"
                      <c:if test="${location.locationId == form.locationId}">selected</c:if>>
                <c:out value="${location.address}" /> • <c:out value="${location.city}" />, <c:out value="${location.zip}" />
              </option>
            </c:forEach>
          </select>
          <c:if test="${not empty errors.locationId}">
            <span class="form-error"><c:out value="${errors.locationId}" /></span>
          </c:if>
        </div>
      </fieldset>

      <fieldset class="form-section">
        <legend>Items</legend>
        <c:if test="${not empty errors.items}">
          <div class="form-error-banner">
            <c:out value="${errors.items}" />
          </div>
        </c:if>

        <c:forEach var="item" items="${form.itemRows}" varStatus="status">
          <div class="form-section">
            <h2 class="section-title">Item ${status.index + 1}</h2>

            <div class="form-row">
              <label for="itemCategory${status.index}">Category</label>
              <select id="itemCategory${status.index}" name="itemCategory">
                <option value="">Select a category</option>
                <c:forEach var="category" items="${categories}">
                  <option value="${category.categoryId}"
                          <c:if test="${category.categoryId == item.categoryId}">selected</c:if>>
                    <c:out value="${category.categoryName}" />
                  </option>
                </c:forEach>
              </select>
            </div>

            <div class="form-grid-2">
              <div class="form-row">
                <label for="itemQuantity${status.index}">Quantity</label>
                <input type="number" id="itemQuantity${status.index}" name="itemQuantity"
                       min="1" value="${fn:escapeXml(item.quantity)}">
              </div>
              <div class="form-row">
                <label for="itemUnit${status.index}">Unit</label>
                <input type="text" id="itemUnit${status.index}" name="itemUnit"
                       maxlength="20" value="${fn:escapeXml(item.unit)}">
              </div>
            </div>

            <div class="form-row">
              <label for="itemExpiry${status.index}">Expiry date</label>
              <input type="date" id="itemExpiry${status.index}" name="itemExpiry"
                     value="${fn:escapeXml(item.expiryDate)}">
            </div>

            <c:set var="itemErrorKey" value="${'item' + status.index}" />
            <c:if test="${not empty errors[itemErrorKey]}">
              <span class="form-error"><c:out value="${errors[itemErrorKey]}" /></span>
            </c:if>
          </div>
        </c:forEach>
      </fieldset>

      <button type="submit" class="btn btn-primary btn-lg auth-submit">Post listing</button>
    </form>
  </div>
</main>

<%@ include file="footer.jspf" %>
