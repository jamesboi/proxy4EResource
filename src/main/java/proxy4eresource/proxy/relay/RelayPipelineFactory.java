package proxy4eresource.proxy.relay;

import static org.jboss.netty.channel.Channels.pipeline;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.Timer;
import org.littleshoot.proxy.ChainProxyManager;
import org.littleshoot.proxy.HttpFilter;
import org.littleshoot.proxy.HttpRequestFilter;
import org.littleshoot.proxy.HttpResponseFilters;
import org.littleshoot.proxy.IdleAwareHandler;
import org.littleshoot.proxy.RelayListener;
import org.littleshoot.proxy.HttpRelayingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import proxy4eresource.proxy.request.ProxyHttpRequestEncoder;
import proxy4eresource.proxy.util.Configure;
/**
 *
 * @author l-O-O-l
 * @email nlwe.bsit.ecom gmail.com
 *
 */
public class RelayPipelineFactory implements ChannelPipelineFactory {
	private static final Logger LOG = LoggerFactory
			.getLogger(RelayPipelineFactory.class);

	protected final String hostAndPort;
	protected final HttpRequest httpRequest;
	protected final RelayListener relayListener;
	protected final Channel browserToProxyChannel;

	protected final ChannelGroup channelGroup;
	protected final HttpRequestFilter requestFilter;
	protected final ChainProxyManager chainProxyManager;
	protected final boolean filtersOff;
	protected final HttpResponseFilters responseFilters;

	protected final Timer timer;
	protected SslHandler sslhandler;

	public RelayPipelineFactory(final String hostAndPort,
			final HttpRequest httpRequest, final RelayListener relayListener,
			final Channel browserToProxyChannel,
			final ChannelGroup channelGroup,
			final HttpResponseFilters responseFilters,
			final HttpRequestFilter requestFilter,
			final ChainProxyManager chainProxyManager, final Timer timer,
			final boolean ssl) {
		this.hostAndPort = hostAndPort;
		this.httpRequest = httpRequest;
		this.relayListener = relayListener;
		this.browserToProxyChannel = browserToProxyChannel;

		this.channelGroup = channelGroup;
		this.responseFilters = responseFilters;
		this.requestFilter = requestFilter;
		this.chainProxyManager = chainProxyManager;
		this.timer = timer;
		this.filtersOff = responseFilters == null;
		if (ssl) {
			SSLContext sslctx;
			try {
				sslctx = SSLContext.getInstance("TLS");
				sslctx.init(null, SSLTrustManagerFactory.getTrustManagers(),
						null);
				SSLEngine engine = sslctx.createSSLEngine();
				engine.setUseClientMode(true);
				sslhandler = new SslHandler(engine);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (KeyManagementException e) {
				e.printStackTrace();
			}

		}
	}

	public ChannelPipeline getPipeline() throws Exception {
		// Create a default pipeline implementation.
		final ChannelPipeline pipeline = pipeline();

		// We always include the request and response decoders
		// regardless of whether or not this is a URL we're
		// filtering responses for. The reason is that we need to
		// follow connection closing rules based on the response
		// headers and HTTP version.
		//
		// We also importantly need to follow the cache directives
		// in the HTTP response.
		final HttpResponseDecoder decoder;
		if (httpRequest.getMethod() == HttpMethod.HEAD) {
			decoder = new HttpResponseDecoder(8192, 8192 * 2, 8192 * 2) {
				@Override
				protected boolean isContentAlwaysEmpty(final HttpMessage msg) {
					return true;
				}
			};
		} else {
			decoder = new HttpResponseDecoder(8192, 8192 * 2, 8192 * 2);
		}
		if (null != sslhandler) {
			pipeline.addLast("ssl", sslhandler);
		}
		pipeline.addLast("decoder", decoder);
		if (Configure.getInstance().needRewriteUrl()) {
			pipeline.addLast("aggregator", new HttpChunkAggregator(
					Integer.MAX_VALUE));
			pipeline.addLast("deflater", new HttpContentDecompressor());
		}

		LOG.debug("Querying for host and port: {}", hostAndPort);
		final boolean shouldFilter;
		final HttpFilter filter;
		if (filtersOff) {
			shouldFilter = false;
			filter = null;
		} else {
			filter = responseFilters.getFilter(hostAndPort);
			if (filter == null) {
				LOG.debug("No filter found");
				shouldFilter = false;
			} else {
				LOG.debug("Using filter: {}", filter);
				shouldFilter = filter.filterResponses(httpRequest);
				// We decompress and aggregate chunks for responses from
				// sites we're applying rules to.
				if (shouldFilter) {
					pipeline.addLast("aggregator", new HttpChunkAggregator(
							filter.getMaxResponseSize()));// 2048576));
				}
			}
			LOG.debug("Filtering: " + shouldFilter);
		}

		// The trick here is we need to determine whether or not
		// to cache responses based on the full URI of the request.
		// This request encoder will only get the URI without the
		// host, so we just have to be aware of that and construct
		// the original.
		final HttpRelayingHandler handler;
		if (shouldFilter) {
			LOG.debug("Creating relay handler with filter");
			handler = new HttpRelayingHandler(browserToProxyChannel,
					channelGroup, filter, relayListener, hostAndPort);
		} else {
			LOG.debug("Creating non-filtering relay handler");
			handler = new HttpRelayingHandler(browserToProxyChannel,
					channelGroup, relayListener, hostAndPort);
		}

		final ProxyHttpRequestEncoder encoder = new ProxyHttpRequestEncoder(
				handler, requestFilter, chainProxyManager != null
						&& chainProxyManager.getChainProxy(httpRequest) != null);
		pipeline.addLast("encoder", encoder);

		// We close idle connections to remote servers after the
		// specified timeouts in seconds. If we're sending data, the
		// write timeout should be reasonably low. If we're reading
		// data, however, the read timeout is more relevant.
		final HttpMethod method = httpRequest.getMethod();

		// Could be any protocol if it's connect, so hard to say what the
		// timeout should be, if any.
		if (!method.equals(HttpMethod.CONNECT)) {
			final int readTimeoutSeconds;
			final int writeTimeoutSeconds;
			if (method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT)) {
				readTimeoutSeconds = 0;
				writeTimeoutSeconds = Configure.getInstance()
						.getTimeoutSeconds();
			} else {
				readTimeoutSeconds = Configure.getInstance()
						.getTimeoutSeconds();
				writeTimeoutSeconds = 0;
			}
			pipeline.addLast("idle", new IdleStateHandler(this.timer,
					readTimeoutSeconds, writeTimeoutSeconds, 0));
			pipeline.addLast("idleAware", new IdleAwareHandler("Relay-Handler"));
		}
		pipeline.addLast("handler", handler);
		return pipeline;
	}
}
