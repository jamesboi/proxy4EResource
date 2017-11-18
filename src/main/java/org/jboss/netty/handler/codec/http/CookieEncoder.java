package org.jboss.netty.handler.codec.http;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Modified from org.jboss.netty.handler.codec.http.CookieEncoder
 * @author l-O-O-l
 * @email nlwe.bsit.ecom gmail.com
 */
public class CookieEncoder {

    private final Set<Cookie> cookies;

    /**
     * Creates a new encoder.
     *
     * @param server {@code true} if and only if this encoder is supposed to
     *		     encode server-side cookies.  {@code false} if and only if
     *		     this encoder is supposed to encode client-side cookies.
     */
    public CookieEncoder() {
	cookies = new TreeSet<Cookie>();
    }
    /**
     * Adds a new {@link Cookie} created with the specified name and value to
     * this encoder.
     */
    public void addCookie(String name, String value) {
	cookies.add(new DefaultCookie(name, value));
    }

    /**
     * Adds the specified {@link Cookie} to this encoder.
     */
    public void addCookie(Cookie cookie) {
	cookies.add(cookie);
    }

    /**
     * Encodes the {@link Cookie}s which were added by {@link #addCookie(Cookie)}
     * so far into an HTTP header value.  If no {@link Cookie}s were added,
     * an empty string is returned.
     *
     * <strong>Be aware that calling this method will clear the content of the {@link CookieEncoder}</strong>
     */
    public Iterable<?> encode() {
	List<String> it = new LinkedList<String>();
	for(Cookie cookie:cookies){
		it.add(encodeServerSide(cookie));
	}
	return it;
    }

    private String encodeServerSide(Cookie cookie) {
	StringBuilder sb = new StringBuilder();
	    add(sb, cookie.getName(), cookie.getValue());

	    if (cookie.getMaxAge() != Integer.MIN_VALUE) {
		if (cookie.getVersion() == 0) {
		    addUnquoted(sb, CookieHeaderNames.EXPIRES,
			    new CookieDateFormat().format(
				    new Date(System.currentTimeMillis() +
					     cookie.getMaxAge() * 1000L)));
		} else {
		    add(sb, CookieHeaderNames.MAX_AGE, cookie.getMaxAge());
		}
	    }

	    if (cookie.getPath() != null) {
		if (cookie.getVersion() > 0) {
		    add(sb, CookieHeaderNames.PATH, cookie.getPath());
		} else {
		    addUnquoted(sb, CookieHeaderNames.PATH, cookie.getPath());
		}
	    }

	    if (cookie.getDomain() != null) {
		if (cookie.getVersion() > 0) {
		    add(sb, CookieHeaderNames.DOMAIN, cookie.getDomain());
		} else {
		    addUnquoted(sb, CookieHeaderNames.DOMAIN, cookie.getDomain());
		}
	    }
	    if (cookie.isSecure()) {
		sb.append(CookieHeaderNames.SECURE);
		sb.append((char) HttpConstants.SEMICOLON);
		sb.append((char) HttpConstants.SP);
	    }
	    if (cookie.isHttpOnly()) {
		sb.append(CookieHeaderNames.HTTPONLY);
		sb.append((char) HttpConstants.SEMICOLON);
		sb.append((char) HttpConstants.SP);
	    }
	    if (cookie.getVersion() >= 1) {
		if (cookie.getComment() != null) {
		    add(sb, CookieHeaderNames.COMMENT, cookie.getComment());
		}

		add(sb, CookieHeaderNames.VERSION, 1);

		if (cookie.getCommentUrl() != null) {
		    addQuoted(sb, CookieHeaderNames.COMMENTURL, cookie.getCommentUrl());
		}

		if (!cookie.getPorts().isEmpty()) {
		    sb.append(CookieHeaderNames.PORT);
		    sb.append((char) HttpConstants.EQUALS);
		    sb.append((char) HttpConstants.DOUBLE_QUOTE);
		    for (int port: cookie.getPorts()) {
			sb.append(port);
			sb.append((char) HttpConstants.COMMA);
		    }
		    sb.setCharAt(sb.length() - 1, (char) HttpConstants.DOUBLE_QUOTE);
		    sb.append((char) HttpConstants.SEMICOLON);
		    sb.append((char) HttpConstants.SP);
		}
		if (cookie.isDiscard()) {
		    sb.append(CookieHeaderNames.DISCARD);
		    sb.append((char) HttpConstants.SEMICOLON);
		    sb.append((char) HttpConstants.SP);
		}
	    }
	if (sb.length() > 0) {
	    sb.setLength(sb.length() - 2);
	}

	return sb.toString();
    }
    private static void add(StringBuilder sb, String name, String val) {
	if (val == null) {
	    addQuoted(sb, name, "");
	    return;
	}

	for (int i = 0; i < val.length(); i ++) {
	    char c = val.charAt(i);
	    switch (c) {
	    case '\t': case ' ': case '"': case '(':  case ')': case ',':
	    case '/':  case ':': case ';': case '<':  case '=': case '>':
	    case '?':  case '@': case '[': case '\\': case ']':
	    case '{':  case '}':
		addQuoted(sb, name, val);
		return;
	    }
	}

	addUnquoted(sb, name, val);
    }

    private static void addUnquoted(StringBuilder sb, String name, String val) {
	sb.append(name);
	sb.append((char) HttpConstants.EQUALS);
	sb.append(val);
	sb.append((char) HttpConstants.SEMICOLON);
	sb.append((char) HttpConstants.SP);
    }

    private static void addQuoted(StringBuilder sb, String name, String val) {
	if (val == null) {
	    val = "";
	}

	sb.append(name);
	sb.append((char) HttpConstants.EQUALS);
	sb.append((char) HttpConstants.DOUBLE_QUOTE);
	sb.append(val.replace("\\", "\\\\").replace("\"", "\\\""));
	sb.append((char) HttpConstants.DOUBLE_QUOTE);
	sb.append((char) HttpConstants.SEMICOLON);
	sb.append((char) HttpConstants.SP);
    }

    private static void add(StringBuilder sb, String name, int val) {
	sb.append(name);
	sb.append((char) HttpConstants.EQUALS);
	sb.append(val);
	sb.append((char) HttpConstants.SEMICOLON);
	sb.append((char) HttpConstants.SP);
    }
}
