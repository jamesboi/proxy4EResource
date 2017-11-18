package proxy4eresource.proxy.relay;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.util.Timer;
import org.littleshoot.proxy.ChainProxyManager;
import org.littleshoot.proxy.HttpRequestFilter;
import org.littleshoot.proxy.HttpResponseFilters;
import org.littleshoot.proxy.ProxyUtils;
import org.littleshoot.proxy.RelayListener;
import org.littleshoot.proxy.RelayPipelineFactoryFactory;
/**
 *
 * @author l-O-O-l
 * @email nlwe.bsit.ecom gmail.com
 *
 */
public class EResourceRelayPipelineFactoryFactory implements
		RelayPipelineFactoryFactory {

	protected final ChainProxyManager chainProxyManager;
	protected final ChannelGroup channelGroup;
	protected final HttpRequestFilter requestFilter;
	protected final HttpResponseFilters responseFilters;
	protected final Timer timer;
	protected final boolean ssl;

	public EResourceRelayPipelineFactoryFactory(
			final ChainProxyManager chainProxyManager,
			final HttpResponseFilters responseFilters,
			final HttpRequestFilter requestFilter,
			final ChannelGroup channelGroup, final Timer timer,boolean ssl) {
		this.chainProxyManager = chainProxyManager;
		this.responseFilters = responseFilters;
		this.channelGroup = channelGroup;
		this.requestFilter = requestFilter;
		this.timer = timer;
		this.ssl = ssl;
	}

	public ChannelPipelineFactory getRelayPipelineFactory(
			final HttpRequest httpRequest, final Channel browserToProxyChannel,
			final RelayListener relayListener) {

		String hostAndPort = chainProxyManager == null ? null
				: chainProxyManager.getChainProxy(httpRequest);
		if (hostAndPort == null) {
			hostAndPort = ProxyUtils.parseHostAndPort(httpRequest);
		}

		return new RelayPipelineFactory(hostAndPort, httpRequest,
				relayListener, browserToProxyChannel, channelGroup,
				responseFilters, requestFilter, chainProxyManager, this.timer,ssl);
	}

}