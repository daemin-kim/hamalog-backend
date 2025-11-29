package com.Hamalog.logging;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * HttpServletResponse wrapper that keeps track of the most recent HTTP status code
 * even when sendError or sendRedirect is called.
 */
public class StatusAwareResponseWrapper extends HttpServletResponseWrapper {

    private int httpStatus = 0;

    public StatusAwareResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void sendError(int sc) throws IOException {
        this.httpStatus = sc;
        super.sendError(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        this.httpStatus = sc;
        super.sendError(sc, msg);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        this.httpStatus = SC_FOUND;
        super.sendRedirect(location);
    }

    @Override
    public void setStatus(int sc) {
        this.httpStatus = sc;
        super.setStatus(sc);
    }

    public int getStatusCode() {
        return this.httpStatus;
    }
}
