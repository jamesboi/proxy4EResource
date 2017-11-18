package proxy;

import junit.framework.TestCase;
import proxy4eresource.proxy.util.ProxyHeaderUtil;
import proxy4eresource.proxy.util.ProxyMessageDigester;

public class ProxyHeaderUtilTest extends TestCase {
	protected ProxyHeaderUtil urlUtil = ProxyHeaderUtil.getInstance();
	protected ProxyMessageDigester digester = ProxyMessageDigester
			.getInstance();

	public void t0estDomainNameFromBrowser() {
		// System.out.println(util.validate("a.p.qit"));
		assertNotNull(urlUtil.getRemoteDomainFromBrowser("a.p.qit"));
		assertNotNull(urlUtil.getRemoteDomainFromBrowser("b.p.qit"));
		assertNotNull(urlUtil.getRemoteDomainFromBrowser("x.aadf.zz.p.qit"));
		assertNotNull(urlUtil.getRemoteDomainFromBrowser("..p.qit"));
		assertNotNull(urlUtil.getRemoteDomainFromBrowser("w.i.p.qit:8000"));
		assertNull(urlUtil.getRemoteDomainFromBrowser("a.sit"));
		assertNull(urlUtil.getRemoteDomainFromBrowser("a.git"));
		assertNull(urlUtil.getRemoteDomainFromBrowser("google.com"));
		assertNull(urlUtil.getRemoteDomainFromBrowser("eresrouce.org.sit"));
		assertNotNull(urlUtil.getRemoteDomainFromBrowser("eresource.p.qit"));
		assertEquals("eresrouce.org",
				urlUtil.getRemoteDomainFromBrowser("eresrouce.org.p.qit"));
		// assertEquals("eresrouce.org:8001",urlUtil.getRemoteDomainFromBrowser("eresrouce.org.p.qit:8001"));
	}

	public void testDomainNameFromVendor() {
		String url = "https://www.google.com/a/b/c";
		String o = urlUtil.getRemoteDomainFromVendor(url);
		assertEquals("https://www.google.com.p.qit/a/b/c", o);
		url = "https://www.zaobao.com";
		o = urlUtil.getRemoteDomainFromVendor(url);
		assertEquals("https://www.zaobao.com.p.qit", o);
		url = "https://www.zaobao.com/";
		o = urlUtil.getRemoteDomainFromVendor(url);
		assertEquals("https://www.zaobao.com.p.qit/", o);

		url = "http://www.google.com/a/b/c";
		o = urlUtil.getRemoteDomainFromVendor(url);
		assertEquals("http://www.google.com.p.qit/a/b/c", o);
		url = "http://www.zaobao.com";
		o = urlUtil.getRemoteDomainFromVendor(url);
		assertEquals("http://www.zaobao.com.p.qit", o);
		url = "http://www.zaobao.com/";
		o = urlUtil.getRemoteDomainFromVendor(url);
		assertEquals("http://www.zaobao.com.p.qit/", o);

		// assertEquals("https://www.zaobao.com.p.qit:8001/",urlUtil.getRemoteDomainFromVendor("https://www.zaobao.com:8001/"));
	}

	public void testTransparentURL4Vendor() {
		String url = "https://www.google.com.p.qit/a/b/c";
		String o = urlUtil.getTransparentURL4Vendor(url);
		assertEquals("https://www.google.com/a/b/c", o);
		url = "https://www.zaobao.com.p.qit";
		o = urlUtil.getTransparentURL4Vendor(url);
		assertEquals("https://www.zaobao.com", o);
		url = "https://www.zaobao.com.p.qit/";
		o = urlUtil.getTransparentURL4Vendor(url);
		assertEquals("https://www.zaobao.com/", o);

		url = "http://www.google.com.p.qit/a/b/c";
		o = urlUtil.getTransparentURL4Vendor(url);
		assertEquals("http://www.google.com/a/b/c", o);
		url = "http://www.zaobao.com.p.qit";
		o = urlUtil.getTransparentURL4Vendor(url);
		assertEquals("http://www.zaobao.com", o);
		url = "http://www.zaobao.com.p.qit/";
		o = urlUtil.getTransparentURL4Vendor(url);
		assertEquals("http://www.zaobao.com/", o);
		// assertEquals("https://www.zaobao.com:8001/a",urlUtil.getTransparentURL4Vendor("https://www.zaobao.com.p.qit:8001/a"));
	}

	public void testGetSecondLastDotPosition() {
		assertEquals(".hi.com", urlUtil.getTopDomainName("abc.efg.hi.com"));
		assertEquals(".hiq.com", urlUtil.getTopDomainName("www.hiq.com:3158"));
	}

	public void testGetDomainName() {
		assertEquals("abc.cfg.com",
				urlUtil.getDomainName("https://abc.cfg.com/abcde"));
		assertEquals("abc.cfg.co",
				urlUtil.getDomainName("https://abc.cfg.co:335/abcde"));
	}

	public void testMd5() {
		assertEquals("900150983cd24fb0d6963f7d28e17f72", digester.getMd5("abc"));
		assertEquals("d6b0ab7f1c8ab8f514db9a6d85de160a",
				digester.getMd5("abc12345"));
		assertEquals("9fe15fea0cb9bac51dcb8b9436801aee",
				digester.getMd5("a$3xsxKd-."));
		assertEquals("dbb4058f1e3992a0a2aa5ab63fe3051a",
				digester.getMd5("1123.311.22.890"));
		assertEquals("042999d53e8c4dd54e4d28f038a88788",
				digester.getMd5("1@#$%^&UHB"));
	}
}
