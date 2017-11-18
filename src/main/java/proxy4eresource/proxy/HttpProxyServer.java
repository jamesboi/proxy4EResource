package proxy4eresource.proxy;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;
import org.jboss.netty.util.Timer;
import org.littleshoot.proxy.ChainProxyManager;
import org.littleshoot.proxy.ProxyAuthorizationHandler;
import org.littleshoot.proxy.ProxyAuthorizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import proxy4eresource.proxy.manager.AuthorizationManager;
import proxy4eresource.proxy.manager.EResourceChainProxyManager;
import proxy4eresource.proxy.relay.EResourceRelayPipelineFactoryFactory;
/**
 *
 * @author l-O-O-l
 * @email nlwe.bsit.ecom gmail.com
 *
 */
public class HttpProxyServer {
	private final Logger log = LoggerFactory.getLogger(getClass());

	protected final ClientSocketChannelFactory clientChannelFactory;
	protected final Timer timer;
	protected final ServerSocketChannelFactory serverChannelFactory;
	protected final ServerBootstrap serverBootstrap;
	protected final int port;
	protected final ChannelGroup allChannels = new DefaultChannelGroup(
			"HTTP-Proxy-Server");
	protected final AtomicBoolean stopped = new AtomicBoolean(false);
	protected final ProxyAuthorizationManager authenticationManager = new AuthorizationManager();

	// new DefaultProxyAuthorizationManager();

	public HttpProxyServer(final int port) {
		this.port = port;
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(final Thread t, final Throwable e) {
				log.error("Uncaught throwable", e);
			}
		});
		clientChannelFactory = new NioClientSocketChannelFactory(
				newClientThreadPool(), newClientThreadPool());
		timer = new HashedWheelTimer();

		serverChannelFactory = new NioServerSocketChannelFactory(
				newServerThreadPool(), newServerThreadPool());

		// Use our thread names so users know there are LittleProxy threads.
		ThreadRenamingRunnable
				.setThreadNameDeterminer(ThreadNameDeterminer.CURRENT);
		this.serverBootstrap = new ServerBootstrap(serverChannelFactory);
	}

	public void start() {
		start(false);

	}

	public void stop() {
		log.info("Shutting down proxy");
		if (stopped.get()) {
			log.info("Already stopped");
			return;
		}
		stopped.set(true);

		log.info("Closing all channels...");

		// See http://static.netty.io/3.5/guide/#start.12

		final ChannelGroupFuture future = allChannels.close();
		future.awaitUninterruptibly(10 * 1000);

		if (!future.isCompleteSuccess()) {
			final Iterator<ChannelFuture> iter = future.iterator();
			while (iter.hasNext()) {
				final ChannelFuture cf = iter.next();
				if (!cf.isSuccess()) {
					log.warn("Cause of failure for {} is {}", cf.getChannel(),
							cf.getCause());
				}
			}
		}
		log.info("Stopping timer");
		timer.stop();
		serverChannelFactory.releaseExternalResources();
		clientChannelFactory.releaseExternalResources();

		log.info("Done shutting down proxy");

	}

	public void start(boolean ssl) {
		System.out.println("here");
		log.info("Starting proxy on port: " + this.port);
		this.stopped.set(false);
		final ChainProxyManager chainProxyManagernew = new EResourceChainProxyManager();
		final HttpServerPipelineFactory factory;
		factory = new HttpServerPipelineFactory(authenticationManager,
				allChannels, new EResourceRelayPipelineFactoryFactory(
						chainProxyManagernew, null, null, this.allChannels,
						timer, ssl), null, timer, this.clientChannelFactory);
		serverBootstrap.setPipelineFactory(factory);
		InetSocketAddress isa = new InetSocketAddress(port);
		final Channel channel = serverBootstrap.bind(isa);
		allChannels.add(channel);

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				log.warn("Stopping ... ");
				stop();
			}
		}));
	}

	public void addProxyAuthenticationHandler(
			final ProxyAuthorizationHandler pah) {
		this.authenticationManager.addHandler(pah);
	}

	private Executor newClientThreadPool() {
		return newThreadPool("Proxy4EResouce-NioClientSocketChannelFactory-Thread-");

	}

	private Executor newServerThreadPool() {
		return newThreadPool("Proxy4EResouce-NioServerSocketChannelFactory-Thread-");
	}

	protected Executor newThreadPool(final String msg) {
		return Executors.newCachedThreadPool(new ThreadFactory() {
			private int num = Integer.MIN_VALUE;

			public Thread newThread(final Runnable r) {
				final Thread t = new Thread(r, msg + num++);
				return t;
			}
		});
	}

}
