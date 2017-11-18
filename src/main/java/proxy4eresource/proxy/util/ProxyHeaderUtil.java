package proxy4eresource.proxy.util;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
/**
 *
 * @author l-O-O-l
 * @email nlwe.bsit.ecom gmail.com
 *
 */
public class ProxyHeaderUtil {
	protected static ProxyHeaderUtil _util;
	protected Configure sysprop = Configure.getInstance();
	protected ProxyMessageDigester digester;
	protected Cache<String, String> topDomainCache;
	protected Cache<String, String> remoteDomainfBCache;

	private final Logger log = LoggerFactory.getLogger(getClass());

	protected ProxyHeaderUtil() {
		digester = ProxyMessageDigester.getInstance();
		topDomainCache = CacheBuilder.newBuilder().maximumSize(1000)
				.expireAfterAccess(10, TimeUnit.MINUTES)
				.build(new CacheLoader<String, String>() {
					public String load(String key) { // no checked exception
						return Strings.empty;
					}
				});
		remoteDomainfBCache = CacheBuilder.newBuilder().maximumSize(1000)
				.expireAfterAccess(10, TimeUnit.MINUTES)
				.build(new CacheLoader<String, String>() {
					public String load(String key) { // no checked exception
						return Strings.empty;
					}
				});

	}

	public static ProxyHeaderUtil getInstance() {
		if (_util == null)
			_util = new ProxyHeaderUtil();
		return _util;
	}

	public String getRemoteDomainFromBrowser(String url) {
		if (null == url)
			return null;
		String m = remoteDomainfBCache.getIfPresent(url);
		if (m == null) {
			m = buildRemoteDomainFromBrowser(url);
			if (null != m)
				remoteDomainfBCache.put(url, m);
		}
		return m;

	}

	/**
	 * get proxy4.eresource.org:123/new.html from
	 * proxy4.eresource.org.serverBaseseName:123/new.html
	 *
	 * @param url
	 * @return
	 */
	protected String buildRemoteDomainFromBrowser(String url) {
		int colon = url.indexOf(Strings.colon); // in case there is a port
												// number
		String test = (colon < 0) ? url : url.substring(0, colon);
		if (test.toLowerCase().endsWith(sysprop.getServerBaseName())) {
			int end = test.length() - sysprop.getServerBaseName().length() - 1;
			if (end > 0)
				return test.substring(0, end);
		}
		return null;
	}

	/**
	 * get the the position of the n's slash
	 * @param url
	 * @param n
	 * @return
	 */
	protected int getSlashPosition(String url,int n){
		int i = 0, j = 4;
		while (i < n && j < url.length()) {
			if (url.charAt(j) == Strings.slash) {
				i++;
			}
			j++;
		}
		return j;
	}

	/**
	 * Update from http://www.google.com/a/b/c to
	 * http://www.google.com.<base>/a/b/c
	 *
	 * @param url
	 * @return
	 */
	public String getRemoteDomainFromVendor(String url) {
		int position = getSlashPosition(url,3);
		StringBuilder sb;
		boolean split = true;
		if (position >= url.length()
				&& Strings.slash != url.charAt(position - 1)) {
			split = false;
		}
		if (split) {
			sb = new StringBuilder(url.substring(0, position - 1))
					.append(Strings.dot).append(sysprop.getServerBaseName())
					.append(url.substring(position - 1));
		} else {
			sb = new StringBuilder(url).append(Strings.dot).append(
					sysprop.getServerBaseName());
		}

		return sb.toString();
	}

	/**
	 * get abc.cfg.com from http://abc.cfg.com:339/abcde or https://abc.cfg.com:339/abcde
	 *
	 * @param url
	 * @return
	 */
	public String getDomainName(String url) {
		if (null == url)
			return null;
		int position = getSlashPosition(url,3);
		if (position < url.length()
				|| Strings.slash == url.charAt(position - 1)) {
			position--;
		}
		int colon = url.lastIndexOf(Strings.colon);
		if (colon > 6) {
			position = Math.min(position, colon);
		}
		int start = getSlashPosition(url,2);
		return url.substring(start, position);

	}

	/**
	 * get .vendor.com.basedomain from vendor.com
	 *
	 * @param vendorDomain
	 * @return
	 */
	public String getBrowserCookieDomain(String vendorDomain) {
		return new StringBuilder(vendorDomain).append(Strings.dot)
				.append(sysprop.getServerBaseName()).toString();
	}

	/**
	 * update from http://www.google.com.<base>/a/b/c to
	 * http://www.google.com.p.qit/a/b/c
	 *
	 * @param url
	 * @return
	 */
	public String getTransparentURL4Vendor(String url) {
		int position = getSlashPosition(url,3);
		StringBuilder sb;
		boolean split = true;
		if (position >= url.length()
				&& Strings.slash != url.charAt(position - 1)) {
			split = false;
		}
		int sbnlength = sysprop.getServerBaseName().length();
		if (split) {
			sb = new StringBuilder(url.substring(0, position - 2 - sbnlength))
					.append(url.substring(position - 1));
			return sb.toString();
		} else {
			return url.substring(0, url.length() - sbnlength - 1);
		}
	}

	public void updateResponseFromVendorToBrowser(HttpResponse response,
			String hostname) {
		log.debug("Before update\n {}\n" + hostname, response);
		if (sysprop.proxyRedirect())
			updateRedirectStatus(response, hostname);
		updateCookieFromResponse(response);
		log.debug("After update\n {}", response);
	}

	protected void updateRedirectStatus(HttpResponse response, String hostname) {
		if (HttpResponseStatus.FOUND.equals(response.getStatus())
				|| HttpResponseStatus.MOVED_PERMANENTLY.equals(response
						.getStatus())) {
			String vl = response.getHeader(HttpHeaders.Names.LOCATION);
			String topHostname = getTopDomainName(hostname);
			String test = getDomainName(vl);
			if (test.endsWith(topHostname)) {
				String location = getRemoteDomainFromVendor(vl);
				log.debug("update location from {} to {}", vl, location);
				response.setHeader(HttpHeaders.Names.LOCATION, location);
			}
		}
	}

	protected void updateCookieFromResponse(HttpResponse response) {
		List<String> scookie = response
				.getHeaders(HttpHeaders.Names.SET_COOKIE);
		CookieEncoder encoder = new CookieEncoder();
		for (String cookies : scookie) {
			updateCookies(cookies, encoder);
		}
		response.setHeader(HttpHeaders.Names.SET_COOKIE, encoder.encode());
	}

	protected void updateCookies(String scookie, CookieEncoder encoder) {
		Set<Cookie> cookies = new CookieDecoder().decode(scookie);
		if (null == cookies || cookies.size() <= 0)
			return;
		String tmp1, tmp2;
		for (Cookie ck : cookies) {
			tmp1 = ck.getDomain();
			if (tmp1 != null) {
				tmp2 = getBrowserCookieDomain(tmp1);
				ck.setDomain(tmp2);
				log.debug("update cookies from {} to {}", tmp1, tmp2);
			}
			encoder.addCookie(ck);
		}
	}

	public void update4Vendor(HttpRequest request) {
		if (!sysprop.proxyTransparent)
			return;
		String ref = request.getHeader(HttpHeaders.Names.REFERER);
		if (null != ref)
			request.setHeader(HttpHeaders.Names.REFERER,
					getTransparentURL4Vendor(ref));
		// remove headers that varnish may add
		if (sysprop.hasVarish()) {
			request.removeHeader(Strings.http_x_ip);
			request.removeHeader(Strings.http_x_varnish);
		}
		// remove proxy cookies
		List<String> scookies = request.getHeaders(HttpHeaders.Names.COOKIE);
		CookieEncoder cookieEncoder = new CookieEncoder();
		String spCookie = this.sysprop.getAthorizeTokenPrefix();
		if (null != spCookie && spCookie.length() > 0) {
			for (String scookie : scookies) {
				Set<Cookie> cookies = new CookieDecoder().decode(scookie);
				for (Cookie c : cookies) {
					if (!c.getName().startsWith(spCookie)) {
						cookieEncoder.addCookie(c);
					}
				}
			}
			request.setHeader(HttpHeaders.Names.COOKIE, cookieEncoder.encode());
		}

	}

	public String getTopDomainName(String hostname) {
		if (null == hostname)
			return null;
		String m = topDomainCache.getIfPresent(hostname);
		if (m == null) {
			m = buildTopDomainName(hostname);
			if (null != m)
				topDomainCache.put(hostname, m);
		}
		return m;
	}

	/**
	 * get .hi.com from abc.efg.hi.com:3721
	 *
	 * @param hostname
	 */
	protected String buildTopDomainName(String hostname) {
		int colon = hostname.lastIndexOf(Strings.colon);
		colon = (colon < 0) ? hostname.length() : colon;
		int sLastDot = getSecondLastDotPosition(hostname);
		sLastDot = (sLastDot < 0) ? 0 : sLastDot;
		return hostname.substring(sLastDot, colon);
	}

	/**
	 * get 8 from abc.efg.hi.com
	 */
	protected int getSecondLastDotPosition(String s) {
		int last = s.lastIndexOf(Strings.dot);
		if (last < 0)
			return last;
		return s.substring(0, last).lastIndexOf(Strings.dot);
	}

	public String getInstitution(HttpRequest request, String ipAndPort) {
		String cookieName = getInstitutionCookieName(request, ipAndPort);
		if (null == cookieName)
			return Strings.empty;
		List<String> scookies = request.getHeaders(HttpHeaders.Names.COOKIE);
		for (String scookie : scookies) {
			Set<Cookie> cookies = new CookieDecoder().decode(scookie);
			for (Cookie c : cookies) {
				if (cookieName.equals(c.getName()))
					return c.getValue();
			}
		}
		return Strings.empty;
	}

	public String getUserIp(HttpRequest request, String ipAndPort) {
		String ip;
		if (sysprop.hasVarish()) {
			ip = request.getHeader(Strings.http_x_ip);
		} else {
			int colon = ipAndPort.lastIndexOf(Strings.colon);
			colon = (colon < 0) ? ipAndPort.length() : colon;
			ip = ipAndPort.substring(1, colon);
		}
		return ip;
	}

	protected String getInstitutionCookieName(HttpRequest request,
			String ipAndPort) {
		String prefix = sysprop.getAthorizeTokenPrefix();
		if (null == prefix)
			return null;
		StringBuilder cookieName = new StringBuilder(prefix);
		StringBuilder hash = new StringBuilder(getUserIp(request, ipAndPort))
				.append(sysprop.getCookieSalt());
		cookieName.append(digester.getMd5(hash.toString()));
		return cookieName.toString();
	}

	public String getAuthenticateCookieName(String prefix, String topDomainName) {
		StringBuilder token = new StringBuilder(prefix);
		String hash = digester.getMd5(new StringBuffer(sysprop.getCookieSalt())
				.append(topDomainName).toString());
		return token.append(hash).toString();

	}

	public long getSizeTopDomainCache() {
		return topDomainCache.size();
	}

	public long getSizeRemoteDomainfBCache() {
		return remoteDomainfBCache.size();
	}

	public boolean isHttps(HttpRequest req){
		return null != req.getHeader(Strings.http_x_ssl);
	}
}
