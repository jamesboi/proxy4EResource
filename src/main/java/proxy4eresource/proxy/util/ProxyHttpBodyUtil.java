package proxy4eresource.proxy.util;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update http response body if needed
 *
 * @author l-O-O-l
 * @email nlwe.bsit.ecom gmail.com
 *
 */
public class ProxyHttpBodyUtil {
	protected static ProxyHttpBodyUtil _putil = null;
	protected Logger log = LoggerFactory.getLogger(getClass());

	protected String uType = "text/html";
	//protected String bodyRegxPrefix = "(https?://)([A-Za-z0-9\\.\\-]+)";
	//protected String bodyReplacePrefix = "http://$2";
	protected String bodyRegxPrefix = "(//)([A-Za-z0-9\\.\\-]+)";
	protected String bodyReplacePrefix = "//$2";

	public static ProxyHttpBodyUtil getInstance() {
		if (_putil == null) {
			_putil = new ProxyHttpBodyUtil();
		}
		return _putil;
	}

	public void updateBodys(HttpMessage response, String hostAndPort) {
		String type = response.getHeader(HttpHeaders.Names.CONTENT_TYPE);
		if (null == type || !type.startsWith(uType))
			return;
		String html = response.getContent().toString(CharsetUtil.UTF_8);
		String newHtml = updateStrings(html,hostAndPort);
		log.debug("Response update from\n{} to\n" + newHtml, html);
		byte[] newBytes = newHtml.getBytes();
		response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, newBytes.length);
		response.setContent(ChannelBuffers.copiedBuffer(newBytes));
	}

	public String updateStrings(String target, String hostAndPort){
		String topDomain = ProxyHeaderUtil.getInstance().getTopDomainName(
				hostAndPort);
		String regx = new StringBuilder(bodyRegxPrefix).append(topDomain)
				.append(Strings.slash).toString();
		String replace = new StringBuilder(bodyReplacePrefix).append(topDomain)
				.append(Strings.dot)
				.append(Configure.getInstance().getServerBaseName())
				.append(Strings.sslash).toString();
		return target.replaceAll(regx, replace);
	}

}
