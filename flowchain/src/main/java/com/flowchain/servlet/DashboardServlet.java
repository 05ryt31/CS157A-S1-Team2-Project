package com.flowchain.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Forwards role-specific dashboard URLs to their JSP views.
 *
 * AuthFilter has already enforced the matching role for each path,
 * so this servlet just dispatches.
 */
@WebServlet(urlPatterns = {
    "/donor/dashboard",
    "/recipient/dashboard",
    "/admin/dashboard"
})
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String path = req.getServletPath();
        String view;
        if (path.startsWith("/donor"))          view = "/WEB-INF/views/donor-dashboard.jsp";
        else if (path.startsWith("/recipient")) view = "/WEB-INF/views/recipient-dashboard.jsp";
        else                                    view = "/WEB-INF/views/admin-dashboard.jsp";

        req.getRequestDispatcher(view).forward(req, res);
    }
}
