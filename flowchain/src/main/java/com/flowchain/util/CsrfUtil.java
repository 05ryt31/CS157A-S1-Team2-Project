package com.flowchain.util;

import java.security.SecureRandom;
import java.util.Base64;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Per-session CSRF token. Generated lazily, sent to the browser as a
 * hidden form field, validated on every state-changing POST.
 */
public final class CsrfUtil {

    public static final String SESSION_ATTR = "csrfToken";
    public static final String FORM_FIELD   = "csrfToken";

    private static final SecureRandom RNG = new SecureRandom();

    private CsrfUtil() {
        // utility class
    }

    /**
     * Returns the session's CSRF token, generating one if absent.
     * Creates the session if it does not exist.
     */
    public static String getOrCreate(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        String token = (String) session.getAttribute(SESSION_ATTR);
        if (token == null) {
            byte[] bytes = new byte[32];
            RNG.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(SESSION_ATTR, token);
        }
        return token;
    }

    /**
     * Constant-time check of the form's csrfToken against the session's.
     * Returns false (rather than throwing) when the session is missing
     * or the token does not match.
     */
    public static boolean isValid(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        String expected = (String) session.getAttribute(SESSION_ATTR);
        String actual   = req.getParameter(FORM_FIELD);
        if (expected == null || actual == null) return false;
        return constantTimeEquals(expected, actual);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
