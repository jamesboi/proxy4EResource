package proxy4eresource.proxy.util;

import java.io.IOException;
import java.util.Properties;
/**
 *
 * @author l-O-O-l
 * @email nlwe.bsit.ecom gmail.com
 *
 */
public class Configure {
	protected static Configure stv;
	protected String [] whiteListHosts;
	protected int timeoutSeconds;
	protected boolean proxyRedirect;
	protected boolean proxyTransparent;
	protected boolean rewriteUrl;
	protected boolean varish;

	public static Configure getInstance() {
		if (stv == null)
			stv = new Configure();
		return stv;
	}

	private final Properties prop = new Properties();

	protected Configure() {
		try {
			// load a properties file from class path, inside static method
			prop.load(this.getClass().getResourceAsStream("/config.properties"));
			String hosts = prop.getProperty("white-list-hosts","*");
			timeoutSeconds = Integer.parseInt(prop.getProperty("pipeline-timeout-seconds", "100"));
			String redirect = prop.getProperty("proxy-directly-redirect",Strings.strue);
			proxyRedirect = Strings.strue.equals(redirect);
			String transparent = prop.getProperty("proxy-transparent",Strings.strue);
			proxyTransparent = Strings.strue.equals(transparent);
			String srewriteurl = prop.getProperty("proxy-rewrite-url", null);
			rewriteUrl = Strings.strue.equals(srewriteurl);
			String sVarish = prop.getProperty("varish-integration",Strings.strue);
			varish = Strings.strue.equals(sVarish);
			if(! "*".equals(hosts)){
				whiteListHosts = hosts.split(",");
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}

	public String getServerBaseName() {
		return prop.getProperty("server-base-name",Strings.defaultServerBaseName);
	}

	public String getServerDefaultPort() {
		return prop.getProperty("server-default-port","80");
	}

	public String getDefaultRemoteDomain(){
		return prop.getProperty("default-remote-domain");
	}

	public String getAthorizeTokenPrefix(){
		return prop.getProperty("authorize-token-prefix",null);
	}

	public String [] getWhiteListHosts(){
		return whiteListHosts;
	}

	public int getTimeoutSeconds(){
		return timeoutSeconds;
	}

	public boolean proxyRedirect(){
		return proxyRedirect;
	}

	public boolean proxyTransparent(){
		return proxyTransparent;
	}

	public boolean needRewriteUrl(){
		return rewriteUrl;
	}

	public boolean hasVarish(){
		return varish;
	}

	public String getLoginUrl(){
		return prop.getProperty("proxy-login-url");
	}
	public String getCookieSalt(){
		return prop.getProperty("authorize-cookie-salt",Strings.empty);
	}

	public String getLoginUrlPostfix(){
		return prop.getProperty("proxy-loginUrl-postfix",Strings.loginUrlPostfix);
	}
}
