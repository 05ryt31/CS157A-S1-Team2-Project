package com.flowchain.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Guards role restricted areas:
 *   /donor/*     → DONOR
 *   /recipient/* → RECIPIENT
 *   /admin/*     → ADMIN
 *   /account/*   → any authenticated user
 *
 * Unauthenticated requests are redirected to login.
 */
@WebFilter(urlPatterns = {
        "/donor/*",
        "/recipient/*",
        "/admin/*",
        "/account",
        "/account/*"
})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req,
                         ServletResponse res,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpRes = (HttpServletResponse) res;

        HttpSession session = httpReq.getSession(false);

        String role =
                (session != null)
                        ? (String) session.getAttribute("role")
                        : null;

        if (role == null) {

            String next = httpReq.getRequestURI();

            String query = httpReq.getQueryString();

            if (query != null) {
                next = next + "?" + query;
            }

            httpRes.sendRedirect(
                    httpReq.getContextPath()
                            + "/login?next="
                            + java.net.URLEncoder.encode(next, "UTF-8")
            );

            return;
        }

        String requiredRole =
                roleForPath(httpReq.getServletPath());

        /*
         * TEMPORARY:
         * Allow any authenticated user
         * to access admin pages for testing/demo.
         */
        if (requiredRole != null
                && !requiredRole.equals(role)
                && !httpReq.getServletPath().startsWith("/admin")) {

            httpRes.sendError(HttpServletResponse.SC_FORBIDDEN);

            return;
        }

        chain.doFilter(req, res);
    }

    private static String roleForPath(String path) {

        if (path == null) {
            return null;
        }

        if (path.startsWith("/donor")) {
            return "DONOR";
        }

        if (path.startsWith("/recipient")) {
            return "RECIPIENT";
        }

        if (path.startsWith("/admin")) {
            return "ADMIN";
        }

        return null;
    }
}