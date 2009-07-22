package edu.sc.seis.receiverFunction.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


public class MockHttpServletRequest implements HttpServletRequest {

    public String getAuthType() {
        throw new RuntimeException("Not implmented");
    }

    public String getContextPath() {
        throw new RuntimeException("Not implmented");
    }

    public Cookie[] getCookies() {
        throw new RuntimeException("Not implmented");
    }

    public long getDateHeader(String arg0) {
        throw new RuntimeException("Not implmented");
    }

    public String getHeader(String arg0) {
        throw new RuntimeException("Not implmented");
    }

    public Enumeration getHeaderNames() {
        throw new RuntimeException("Not implmented");
    }

    public Enumeration getHeaders(String arg0) {
        throw new RuntimeException("Not implmented");
    }

    public int getIntHeader(String arg0) {
        throw new RuntimeException("Not implmented");
    }

    public String getMethod() {
        throw new RuntimeException("Not implmented");
    }

    public String getPathInfo() {
        throw new RuntimeException("Not implmented");
    }

    public String getPathTranslated() {
        throw new RuntimeException("Not implmented");
    }

    public String getQueryString() {
        throw new RuntimeException("Not implmented");
    }

    public String getRemoteUser() {
        throw new RuntimeException("Not implmented");
    }

    public String getRequestURI() {
        throw new RuntimeException("Not implmented");
    }

    public StringBuffer getRequestURL() {
        throw new RuntimeException("Not implmented");
    }

    public String getRequestedSessionId() {
        throw new RuntimeException("Not implmented");
    }

    public String getServletPath() {
        throw new RuntimeException("Not implmented");
    }

    public HttpSession getSession() {
        throw new RuntimeException("Not implmented");
    }

    public HttpSession getSession(boolean arg0) {
        throw new RuntimeException("Not implmented");
    }

    public Principal getUserPrincipal() {
        throw new RuntimeException("Not implmented");
    }

    public boolean isRequestedSessionIdFromCookie() {
        throw new RuntimeException("Not implmented");
    }

    public boolean isRequestedSessionIdFromURL() {
        throw new RuntimeException("Not implmented");
    }

    public boolean isRequestedSessionIdFromUrl() {
        throw new RuntimeException("Not implmented");
    }

    public boolean isRequestedSessionIdValid() {
        throw new RuntimeException("Not implmented");
    }

    public boolean isUserInRole(String arg0) {
        throw new RuntimeException("Not implmented");
    }

    public Object getAttribute(String arg0) {
        throw new RuntimeException("Not implmented");
    }

    public Enumeration getAttributeNames() {
        throw new RuntimeException("Not implmented");
    }

    public String getCharacterEncoding() {
        throw new RuntimeException("Not implmented");
    }

    public int getContentLength() {
        throw new RuntimeException("Not implmented");
    }

    public String getContentType() {
        throw new RuntimeException("Not implmented");
    }

    public ServletInputStream getInputStream() throws IOException {
        throw new RuntimeException("Not implmented");
    }

    public Locale getLocale() {
        throw new RuntimeException("Not implmented");
    }

    public Enumeration getLocales() {
        throw new RuntimeException("Not implmented");
    }

    public String getParameter(String name) {
        return map.get(name);
    }

    public void setParameter(String name, String value) {
        map.put(name, value);
    }

    public Map getParameterMap() {
        throw new RuntimeException("Not implmented");
    }

    public Enumeration getParameterNames() {
        throw new RuntimeException("Not implmented");
    }

    public String[] getParameterValues(String arg0) {
        throw new RuntimeException("Not implmented");
    }

    public String getProtocol() {
        throw new RuntimeException("Not implmented");
    }

    public BufferedReader getReader() throws IOException {
        throw new RuntimeException("Not implmented");
    }

    public String getRealPath(String arg0) {
        throw new RuntimeException("Not implmented");
    }

    public String getRemoteAddr() {
        throw new RuntimeException("Not implmented");
    }

    public String getRemoteHost() {
        throw new RuntimeException("Not implmented");
    }

    public RequestDispatcher getRequestDispatcher(String arg0) {
        throw new RuntimeException("Not implmented");
    }

    public String getScheme() {
        throw new RuntimeException("Not implmented");
    }

    public String getServerName() {
        throw new RuntimeException("Not implmented");
    }

    public int getServerPort() {
        throw new RuntimeException("Not implmented");
    }

    public boolean isSecure() {
        throw new RuntimeException("Not implmented");
    }

    public void removeAttribute(String arg0) {
        throw new RuntimeException("Not implmented");
    }

    public void setAttribute(String arg0, Object arg1) {
        throw new RuntimeException("Not implmented");
    }

    public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
        throw new RuntimeException("Not implmented");
    }
    
    protected HashMap<String, String> map = new HashMap<String, String>();
}
