package proxy4eresource.proxy.util;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.littleshoot.proxy.ProxyCacheManager;
import org.littleshoot.proxy.ProxyHttpResponse;
import org.littleshoot.proxy.ProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author l-O-O-l
 * @email nlwe.bsit.ecom gmail.com
 *
 */
public class EResourceHttpResponseEncoder extends HttpResponseEncoder {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ProxyCacheManager cacheManager;
    private final boolean transparent;
    public EResourceHttpResponseEncoder(final ProxyCacheManager cacheManager) {
	this.cacheManager = cacheManager;
	transparent = true;
    }
    @Override
    protected Object encode(final ChannelHandlerContext ctx,
	final Channel channel, final Object msg) throws Exception {
	if (msg instanceof ProxyHttpResponse) {
	    //log.info("Processing proxy response!!");
	    final ProxyHttpResponse proxyResponse = (ProxyHttpResponse) msg;

	    // We need the original request and response objects to adequately
	    // follow the HTTP caching rules.
	    final HttpRequest httpRequest = proxyResponse.getHttpRequest();
	    final HttpResponse httpResponse = proxyResponse.getHttpResponse();

	    // The actual response is either a chunk or a "normal" response.
	    final Object response = proxyResponse.getResponse();

	    // We do this right before encoding because we want to deal with
	    // the hop-by-hop headers elsewhere in the proxy processing logic.
	    if (!this.transparent) {
		if (response instanceof HttpResponse) {
		    final HttpResponse hr = (HttpResponse) response;
		    ProxyUtils.stripHopByHopHeaders(hr);
		    ProxyUtils.addVia(hr);
		    log.debug("Actual response going to browser: {}", hr);
		}
	    }

	    final ChannelBuffer encoded =
		(ChannelBuffer) super.encode(ctx, channel, response);

	    // The buffer will be null when it's the last chunk, for example.
	    if (encoded != null && this.cacheManager != null) {
		this.cacheManager.cache(httpRequest, httpResponse, response,
		    encoded);
	    }
	    return encoded;

	} else if (msg instanceof HttpResponse) {
	    // We can get an HttpResponse when a third-party is custom
	    // configured, for example.
	    if (!this.transparent) {
		final HttpResponse hr = (HttpResponse) msg;
		ProxyUtils.stripHopByHopHeaders(hr);
		ProxyUtils.addVia(hr);
	    }
	}
	return super.encode(ctx, channel, msg);
    }
}
