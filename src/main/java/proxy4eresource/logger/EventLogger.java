package proxy4eresource.logger;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import proxy4eresource.proxy.util.ProxyHeaderUtil;
/**
 *
 * @author l-O-O-l
 * @email nlwe.bsit.ecom gmail.com
 *
 */
public class EventLogger {
	protected static Logger log = LoggerFactory.getLogger(EventLogger.class);
	protected static ProxyHeaderUtil phu = ProxyHeaderUtil.getInstance();

	public static void debug(String s) {
		log.debug(s);
	}

	public static void info(String s) {
		log.info(s);
	}

	public static void logRequest(String ipAndPort, HttpRequest request,
			String url) {
		String ip = phu.getUserIp(request, ipAndPort);
		String inst = phu.getInstitution(request, ipAndPort);
		log.debug("{}\t{}\t{}\t{}",ip,inst,request.getMethod(),url);

	}
}
