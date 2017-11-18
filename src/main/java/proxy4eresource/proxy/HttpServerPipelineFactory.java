package proxy4eresource.proxy;

import static org.jboss.netty.channel.Channels.pipeline;

import java.util.concurrent.Future;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.Timer;
import org.littleshoot.proxy.AllConnectionData;
import org.littleshoot.proxy.ChainProxyManager;
import org.littleshoot.proxy.HttpRequestHandler;
import org.littleshoot.proxy.IdleRequestHandler;
import org.littleshoot.proxy.ProxyAuthorizationManager;
import org.littleshoot.proxy.ProxyCacheManager;
import org.littleshoot.proxy.ProxyHttpResponseEncoder;
import org.littleshoot.proxy.RelayPipelineFactoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import proxy4eresource.proxy.request.EResourceHttpRequestHandler;
/**
 *
 * @author l-O-O-l
 * @email nlwe.bsit.ecom gmail.com
 *
 */
public class HttpServerPipelineFactory implements
		ChannelPipelineFactory, AllConnectionData {
	protected int numHandlers;
	protected final RelayPipelineFactoryFactory relayPipelineFactoryFactory;
	protected final Timer timer;
	protected final ClientSocketChannelFactory clientChannelFactory;

	private final ChainProxyManager chainProxyManager;
	protected final ProxyCacheManager cacheManager;

	protected final ProxyAuthorizationManager authorizationManager;
    private static final Logger log =
	    LoggerFactory.getLogger(HttpServerPipelineFactory.class);

	public HttpServerPipelineFactory(final ProxyAuthorizationManager authorizationManager, ChannelGroup channelGroup,
			RelayPipelineFactoryFactory relayPipelineFactoryFactory,final ChainProxyManager chainProxyManager,
			Timer timer, ClientSocketChannelFactory clientChannelFactory) {
		this.authorizationManager = authorizationManager;
		this.relayPipelineFactoryFactory = relayPipelineFactoryFactory;
		this.timer = timer;
		this.clientChannelFactory = clientChannelFactory;
		this.chainProxyManager = chainProxyManager;
	cacheManager = new ProxyCacheManager() {
	    public boolean returnCacheHit(final HttpRequest request,
		final Channel channel) {
		return false;
	    }
	    public Future<String> cache(final HttpRequest originalRequest,
		final HttpResponse httpResponse,
		final Object response, final ChannelBuffer encoded) {
		return null;
	    }
	};

	}

	@Override
	public int getNumRequestHandlers() {
		return this.numHandlers;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {
	final ChannelPipeline pipeline = pipeline();

	log.debug("Accessing pipeline");
	// We want to allow longer request lines, headers, and chunks
	// respectively.
	pipeline.addLast("decoder",
	    new HttpRequestDecoder(8192, 8192*2, 8192*2));
	pipeline.addLast("encoder", new ProxyHttpResponseEncoder(cacheManager,true));
	final HttpRequestHandler httpRequestHandler =
	    new EResourceHttpRequestHandler(this.cacheManager, authorizationManager,
	    null,chainProxyManager,
	    relayPipelineFactoryFactory, this.clientChannelFactory);
	pipeline.addLast("idle", new IdleStateHandler(this.timer, 0, 0, 70));
	//pipeline.addLast("idleAware", new IdleAwareHandler("Client-Pipeline"));
	pipeline.addLast("idleAware", new IdleRequestHandler(httpRequestHandler));
	pipeline.addLast("handler", httpRequestHandler);
	this.numHandlers++;
	return pipeline;
	}

}
