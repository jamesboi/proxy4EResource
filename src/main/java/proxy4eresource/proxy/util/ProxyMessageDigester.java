package proxy4eresource.proxy.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
/**
 *
 * @author l-O-O-l
 * @email nlwe.bsit.ecom gmail.com
 *
 */
public class ProxyMessageDigester {
	protected static ProxyMessageDigester _digester;
	protected MessageDigest md5;
	protected Cache<String, String> cacher;

	protected ProxyMessageDigester() {
		try {
			md5 = MessageDigest.getInstance("MD5"); // or "SHA-1"
		} catch (NoSuchAlgorithmException e) {
		}
		cacher = CacheBuilder.newBuilder().maximumSize(1000)
				.expireAfterAccess(10, TimeUnit.MINUTES)
				.build(new CacheLoader<String, String>() {
					public String load(String key) { // no checked exception
						return Strings.empty;
					}
				});

	}

	public static ProxyMessageDigester getInstance() {
		if (_digester == null)
			_digester = new ProxyMessageDigester();
		return _digester;
	}

	public String getMd5(String msg) {
		String m = cacher.getIfPresent(msg);
		if(m == null){
			m = buildMD5(msg);
			cacher.put(msg, m);
		}
		return m;
	}

	protected String buildMD5(String input) {
		StringBuilder result = null;
		if (input != null) {
			BigInteger hash;
			synchronized (md5) {
				md5.update(input.getBytes());
				hash = new BigInteger(1, md5.digest());
			}
			String mdResult = hash.toString(16);
			int missinglenth = 32 - mdResult.length();
			result = new StringBuilder();
			for (int i = 0; i < missinglenth; i++) {
				result.append("0");
			}
			result.append(mdResult);
		}
		return result.toString();
	}
}
