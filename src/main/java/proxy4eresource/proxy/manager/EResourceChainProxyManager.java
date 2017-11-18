package proxy4eresource.proxy.manager;

import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.littleshoot.proxy.ChainProxyManager;
/**
 *
 * @author l-O-O-l
 * @email nlwe.bsit.ecom gmail.com
 *
 */
public class EResourceChainProxyManager implements ChainProxyManager {

	@Override
	public String getChainProxy(HttpRequest request) {
		return HttpHeaders.getHost(request);
	}

	@Override
	public void onCommunicationError(String arg0) {
	}

}
