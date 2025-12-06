package com.Hamalog.logging;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * HttpServletResponse wrapper that keeps track of the most recent HTTP status code
 * even when sendError or sendRedirect is called.
 */
public class StatusAwareResponseWrapper extends HttpServletResponseWrapper {

    private int httpStatus;
    private boolean statusSet;

    public StatusAwareResponseWrapper(HttpServletResponse response) {
        super(response);
        this.httpStatus = 0;
        this.statusSet = false;
    }

    @Override
    public void sendError(int sc) throws IOException {
        this.httpStatus = sc;
        this.statusSet = true;
        super.sendError(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        this.httpStatus = sc;
        this.statusSet = true;
        super.sendError(sc, msg);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        this.httpStatus = SC_FOUND;
        this.statusSet = true;
        super.sendRedirect(location);
    }

    @Override
    public void setStatus(int sc) {
        this.httpStatus = sc;
        this.statusSet = true;
        super.setStatus(sc);
    }

    @Override
    public int getStatus() {
        return statusSet ? httpStatus : super.getStatus();
    }

    public int getStatusCode() {
        return statusSet ? httpStatus : 0;
    }
}
