package proxy4eresource.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import proxy4eresource.proxy.util.Configure;
import proxy4eresource.proxy.util.ProxyHeaderUtil;
/**
 *
 * @author l-O-O-l
 * @email nlwe.bsit.ecom gmail.com
 *
 */
public class Launcher {
	public static void main(final String... args) {
		Configure stv = Configure.getInstance();
		int port = Integer.parseInt(stv.getServerDefaultPort());
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}

		System.out.println("About to start server on port: " + port);
		final HttpProxyServer server = new HttpProxyServer(port);
		server.start();
//		final HttpProxyServer server2 = new HttpProxyServer(8002);
//		server2.start(true);

		System.out.println("Server started.");
		if (isExit())
			server.stop();
//			server2.stop();
	}

	public static boolean isExit() {
		// open up standard input
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String cmd = null;
		while (null == cmd || !"exit".equals(cmd)) {
			System.out.print("Enter 'exit' to exit the server: ");
			try {
				cmd = br.readLine();
				if ("stats".equals(cmd)) {
					stats();
				}
			} catch (IOException ioe) {
				System.out.println("IO error trying to read your name!");
				System.exit(1);
			}
		}

		System.out.println("Exiting ...");
		return true;
	}

	private static void stats() {
		long maxMemory = Runtime.getRuntime().maxMemory();
		System.out
				.println("Maximum memory (KB): "
						+ (maxMemory == Long.MAX_VALUE ? "no limit"
								: maxMemory / 1000));
		System.out.println("Total memory (KB): "
				+ Runtime.getRuntime().totalMemory() / 1000);
		ProxyHeaderUtil u = ProxyHeaderUtil.getInstance();
		System.out.println("Cache Size: TopDomains "
				+ u.getSizeTopDomainCache() + " RemoteDomains "
				+ u.getSizeRemoteDomainfBCache());

	}
}
