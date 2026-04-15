package com.flowchain.servlet;

import com.flowchain.util.CsrfUtil;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * GET /account → render the account settings page (password change + account deletion)
 *
 * AuthFilter has already verified the user is logged in; any authenticated user
 * (DONOR / RECIPIENT / ADMIN) can manage their own account here.
 */
@WebServlet("/account")
public class AccountServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
        req.getRequestDispatcher("/WEB-INF/views/account.jsp").forward(req, res);
    }
}
