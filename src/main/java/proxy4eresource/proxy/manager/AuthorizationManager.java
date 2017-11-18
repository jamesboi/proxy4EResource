package proxy4eresource.proxy.manager;

import java.util.Set;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.littleshoot.proxy.ProxyAuthorizationHandler;
import org.littleshoot.proxy.ProxyAuthorizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import proxy4eresource.proxy.util.Configure;
import proxy4eresource.proxy.util.ProxyHeaderUtil;
/**
 *
 * @author l-O-O-l
 * @email nlwe.bsit.ecom gmail.com
 *
 */
public class AuthorizationManager implements ProxyAuthorizationManager{
    protected static final Logger log =
	    LoggerFactory.getLogger(AuthorizationManager.class);
    protected static final String tokenPrefix = Configure.getInstance().getAthorizeTokenPrefix();
    protected static final String [] whiteListHosts = Configure.getInstance().getWhiteListHosts();
    protected static final ProxyHeaderUtil phu = ProxyHeaderUtil.getInstance();

	@Override
	public void addHandler(ProxyAuthorizationHandler arg0) {
	}

	@Override
	public boolean handleProxyAuthorization(HttpRequest req,
			ChannelHandlerContext chc) {
		String hostname = phu.getTopDomainName(phu.getRemoteDomainFromBrowser(
				HttpHeaders.getHost(req)));
		System.out.println(HttpHeaders.getHost(req) + "\txxxxxxx" + hostname);
		log.debug("check hostname: " + hostname);
		if(null == hostname) return false;
		boolean b1 = checkUser(req.getHeader(com.google.common.net.HttpHeaders.COOKIE),hostname);
		boolean b2 = checkHosts(hostname);
		return b1 && b2;
	}

	protected boolean checkHosts(String host){
		if(null == whiteListHosts || null == host) return true;
		for(String h : whiteListHosts){
			if(host.endsWith(h)){
				return true;
			}
		}
		return false;
	}
	/**
	 * Check if the request contains the cookie with name tokenPrefix_vendorhostname
	 * @param cookiesString
	 * @param hostname
	 * @return
	 */
	protected boolean checkUser(final String cookiesString,String hostname){
		System.out.println(cookiesString + tokenPrefix);
		if(null == tokenPrefix || tokenPrefix.length()<=0) return true;
		if(null != cookiesString){
			String token = phu.getAuthenticateCookieName(tokenPrefix,hostname);
			log.debug("Looking for cookie name: " + token);
			Set<Cookie> cookies = new CookieDecoder().decode(cookiesString);
			if(cookies.size()<=0) return false;
			for(Cookie cookie : cookies){
				if(token.equals(cookie.getName())) return true;
			}
		}
		return false;
	}

}
