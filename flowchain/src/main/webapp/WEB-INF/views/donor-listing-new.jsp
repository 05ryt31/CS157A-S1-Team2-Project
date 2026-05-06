<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Create Listing" />
<%@ include file="header.jspf" %>

<main class="dashboard-page">
  <div class="container">
    <a href="${pageContext.request.contextPath}/donor/dashboard"
       class="btn btn-outline btn-sm detail-back-btn">
      Back to Dashboard
    </a>

    <h1 class="dashboard-title">Create Surplus Food Listing</h1>
    <p class="dashboard-sub">
      Add listing details and one or more food items for recipients to claim.
    </p>

    <section class="detail-card">
      <form method="post" action="${pageContext.request.contextPath}/donor/listings/new">
        <input type="hidden" name="csrfToken" value="${csrfToken}">

        <div class="form-row">
          <label for="title">Listing Title</label>
          <input type="text" id="title" name="title" maxlength="150" required>
        </div>

        <div class="form-row">
          <label for="description">Description</label>
          <textarea id="description" name="description" rows="4"></textarea>
        </div>

        <div class="form-row">
          <label for="locationId">Pickup Location</label>
          <select id="locationId" name="locationId" required>
            <option value="">Select a location</option>
            <c:forEach var="loc" items="${locations}">
              <option value="${loc.locationId}">
                <c:out value="${loc.address}" />, <c:out value="${loc.city}" /> <c:out value="${loc.zip}" />
              </option>
            </c:forEach>
          </select>
        </div>

        <h2 class="section-title section-title-left">Food Items</h2>

        <div class="items-list">
          <c:forEach var="i" begin="0" end="4">
            <div class="item-row">
              <div class="form-row">
                <label for="categoryId${i}">Category</label>
                <select id="categoryId${i}" name="categoryId">
                  <option value="">Select category</option>
                  <c:forEach var="cat" items="${categories}">
                    <option value="${cat.categoryId}">
                      <c:out value="${cat.categoryName}" />
                    </option>
                  </c:forEach>
                </select>
              </div>

              <div class="form-row">
                <label for="quantity${i}">Quantity</label>
                <input type="number" id="quantity${i}" name="quantity" min="1">
              </div>

              <div class="form-row">
                <label for="unit${i}">Unit</label>
                <input type="text" id="unit${i}" name="unit" maxlength="20" placeholder="lbs, units, trays">
              </div>

              <div class="form-row">
                <label for="expiryDate${i}">Expiry Date</label>
                <input type="date" id="expiryDate${i}" name="expiryDate">
              </div>
            </div>
          </c:forEach>
        </div>

        <p class="dashboard-sub">
          Fill in at least one item row. Extra blank item rows will be ignored.
        </p>

        <div class="dashboard-actions">
          <button type="submit" class="btn btn-primary">Create Listing</button>
          <a href="${pageContext.request.contextPath}/donor/dashboard" class="btn btn-outline">Cancel</a>
        </div>
      </form>
    </section>
  </div>
</main>

<%@ include file="footer.jspf" %>