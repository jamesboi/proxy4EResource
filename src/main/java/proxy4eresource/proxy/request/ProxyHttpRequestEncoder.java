package proxy4eresource.proxy.request;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.littleshoot.proxy.HttpRequestFilter;
import org.littleshoot.proxy.ProxyUtils;
import org.littleshoot.proxy.HttpRelayingHandler;

/**
 * Request encoder for the proxy. This is necessary because we need to have
 * access to the most recent request message on this connection to determine
 * caching rules.
 */
public class ProxyHttpRequestEncoder extends HttpRequestEncoder {
    private final HttpRelayingHandler relayingHandler;
    private final HttpRequestFilter requestFilter;
    private final boolean keepProxyFormat;
    private final boolean transparent;

    /**
     * Creates a new request encoder.
     *
     * @param handler The class that handles relaying all data along this
     * connection. We need this to synchronize caching rules for each request
     * and response pair.
     */
    public ProxyHttpRequestEncoder(final HttpRelayingHandler handler) {
	this(handler, null, false, true);
    }

    /**
     * Creates a new request encoder.
     *
     * @param handler The class that handles relaying all data along this
     * connection. We need this to synchronize caching rules for each request
     * and response pair.
     * @param keepProxyFormat keep proxy-formatted URI (used in chaining)
     * @param requestFilter The filter for requests.
     */
    public ProxyHttpRequestEncoder(final HttpRelayingHandler handler,
	final HttpRequestFilter requestFilter,
	final boolean keepProxyFormat) {
	this(handler, requestFilter, keepProxyFormat, true);
    }

    /**
     * Creates a new request encoder.
     *
     * @param handler The class that handles relaying all data along this
     * connection. We need this to synchronize caching rules for each request
     * and response pair.
     * @param keepProxyFormat keep proxy-formatted URI (used in chaining)
     * @param requestFilter The filter for requests.
     * @param transparent Whether or not this is an transparent proxy.
     * Transparent proxies don't add extra via headers or follow normal
     * proxy rules.
     */
    public ProxyHttpRequestEncoder(final HttpRelayingHandler handler,
	final HttpRequestFilter requestFilter,
	final boolean keepProxyFormat,
	final boolean transparent) {

	this.relayingHandler = handler;
	this.requestFilter = requestFilter;
	this.keepProxyFormat = keepProxyFormat;
	this.transparent = transparent;
    }

    @Override
    protected Object encode(final ChannelHandlerContext ctx,
	final Channel channel, final Object msg) throws Exception {
	if (msg instanceof HttpRequest) {
	    // The relaying handler needs to know all the headers, including
	    // hop-by-hop headers, of the original request, particularly
	    // for determining whether or not to close the connection to the
	    // browser, so we give it the original and copy the original
	    // to modify it just before writing it on the wire.
	    final HttpRequest request = (HttpRequest) msg;
	    this.relayingHandler.requestEncoded(request);

	    // Check if we are running in proxy chain mode and modify request
	    // accordingly.
	    final HttpRequest toSend;
	    if (transparent) {
		toSend = request;
	    } else {
		toSend = ProxyUtils.copyHttpRequest(request, keepProxyFormat);
	    }
	    if (this.requestFilter != null) {
		this.requestFilter.filter(toSend);
	    }
	    //LOG.info("Writing modified request: {}", httpRequestCopy);
	    return super.encode(ctx, channel, toSend);
	}
	return super.encode(ctx, channel, msg);
    }
}
