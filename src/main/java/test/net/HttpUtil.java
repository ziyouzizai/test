package test.net;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSONObject;

import test.others.MD5Utils;

/**
 * http、https 请求工具类， 微信为https的请求
 * 
 * @author yehx
 *
 */
public class HttpUtil {

	private static final String DEFAULT_CHARSET = "UTF-8";

	private static final String _GET = "GET"; // GET
	private static final String _POST = "POST";// POST
	public static final int DEF_CONN_TIMEOUT = 30000;
	public static final int DEF_READ_TIMEOUT = 30000;

	public static JSONObject doPost(String url, JSONObject json) {
		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url);
		JSONObject response = null;
		try {
			StringEntity s = new StringEntity(json.toString());
			s.setContentEncoding("UTF-8");
			s.setContentType("application/json");// 发送json数据需要设置contentType
			post.setEntity(s);
			HttpResponse res = httpclient.execute(post);
			if (res.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				String result = EntityUtils.toString(res.getEntity());// 返回json格式：
				response = JSONObject.parseObject(result);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return response;
	}

	/**
	 * 初始化http请求参数
	 * 
	 * @param url
	 * @param method
	 * @param headers
	 * @return
	 * @throws Exception
	 */
	private static HttpURLConnection initHttp(String url, String method, Map<String, String> headers) throws Exception {
		URL _url = new URL(url);
		HttpURLConnection http = (HttpURLConnection) _url.openConnection();
		// 连接超时
		http.setConnectTimeout(DEF_CONN_TIMEOUT);
		// 读取超时 --服务器响应比较慢，增大时间
		http.setReadTimeout(DEF_READ_TIMEOUT);
		http.setUseCaches(false);
		http.setRequestMethod(method);
		http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		http.setRequestProperty("User-Agent",
				"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.146 Safari/537.36");
		if (null != headers && !headers.isEmpty()) {
			for (Entry<String, String> entry : headers.entrySet()) {
				http.setRequestProperty(entry.getKey(), entry.getValue());
			}
		}
		http.setDoOutput(true);
		http.setDoInput(true);
		http.connect();
		return http;
	}

	/**
	 * 初始化http请求参数
	 * 
	 * @param url
	 * @param method
	 * @return
	 * @throws Exception
	 */
	private static HttpsURLConnection initHttps(String url, String method, Map<String, String> headers)
			throws Exception {
		TrustManager[] tm = { new MyX509TrustManager() };
		System.setProperty("https.protocols", "TLSv1");
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, tm, new java.security.SecureRandom());
		// 从上述SSLContext对象中得到SSLSocketFactory对象
		SSLSocketFactory ssf = sslContext.getSocketFactory();
		URL _url = new URL(url);
		HttpsURLConnection http = (HttpsURLConnection) _url.openConnection();
		// 设置域名校验
		http.setHostnameVerifier(new HttpUtil().new TrustAnyHostnameVerifier());
		// 连接超时
		http.setConnectTimeout(DEF_CONN_TIMEOUT);
		// 读取超时 --服务器响应比较慢，增大时间
		http.setReadTimeout(DEF_READ_TIMEOUT);
		http.setUseCaches(false);
		http.setRequestMethod(method);
		http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		http.setRequestProperty("User-Agent",
				"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.146 Safari/537.36");
		if (null != headers && !headers.isEmpty()) {
			for (Entry<String, String> entry : headers.entrySet()) {
				http.setRequestProperty(entry.getKey(), entry.getValue());
			}
		}
		http.setSSLSocketFactory(ssf);
		http.setDoOutput(true);
		http.setDoInput(true);
		http.connect();
		return http;
	}

	/**
	 * 
	 * @description 功能描述: get 请求
	 * @return 返回类型:
	 * @throws Exception
	 */
	public static String get(String url, Map<String, String> params, Map<String, String> headers) throws Exception {
		HttpURLConnection http = null;
		if (isHttps(url)) {
			http = initHttps(initParams(url, params), _GET, headers);
		} else {
			http = initHttp(initParams(url, params), _GET, headers);
		}
		InputStream in = http.getInputStream();
		BufferedReader read = new BufferedReader(new InputStreamReader(in, DEFAULT_CHARSET));
		String valueString = null;
		StringBuffer bufferRes = new StringBuffer();
		while ((valueString = read.readLine()) != null) {
			bufferRes.append(valueString);
		}
		in.close();
		if (http != null) {
			http.disconnect();// 关闭连接
		}
		return bufferRes.toString();
	}
	
	public static boolean isHtml(String url) throws Exception{
		HttpURLConnection http = null;
		if (isHttps(url)) {
			http = initHttps(initParams(url,null), _GET,null);
		} else {
			http = initHttp(initParams(url, null), _GET, null);
		}
		String contentType = http.getHeaderField("Content-Type");
		if(contentType.indexOf("text/html") > -1) {
			return true;
		}
		return false;
	}
	
	public static String getWithJudge(String url) throws Exception {
		HttpURLConnection http = null;
		if (isHttps(url)) {
			http = initHttps(initParams(url,null), _GET,null);
		} else {
			http = initHttp(initParams(url, null), _GET, null);
		}
		String response = http.getResponseMessage();
		System.out.println(http.getHeaderFields());
		System.out.println(http.getHeaderField("Content-Type"));
		InputStream in = http.getInputStream();
		BufferedReader read = new BufferedReader(new InputStreamReader(in, DEFAULT_CHARSET));
		String valueString = null;
		StringBuffer bufferRes = new StringBuffer();
		while ((valueString = read.readLine()) != null) {
			bufferRes.append(valueString);
		}
		in.close();
		if (http != null) {
			http.disconnect();// 关闭连接
		}
		return bufferRes.toString();
	}

	public static String get(String url) throws Exception {
		return get(url, null);
	}

	public static String get(String url, Map<String, String> params) throws Exception {
		return get(url, params, null);
	}

	public static String post(String url, String params) throws Exception {
		HttpURLConnection http = null;
		if (isHttps(url)) {
			http = initHttps(url, _POST, null);
		} else {
			http = initHttp(url, _POST, null);
		}
		OutputStream out = http.getOutputStream();
		out.write(params.getBytes(DEFAULT_CHARSET));
		out.flush();
		out.close();

		InputStream in = http.getInputStream();
		BufferedReader read = new BufferedReader(new InputStreamReader(in, DEFAULT_CHARSET));
		String valueString = null;
		StringBuffer bufferRes = new StringBuffer();
		while ((valueString = read.readLine()) != null) {
			bufferRes.append(valueString);
		}
		in.close();
		if (http != null) {
			http.disconnect();// 关闭连接
		}
		return bufferRes.toString();
	}

	/**
	 * 功能描述: 构造请求参数
	 * 
	 * @return 返回类型:
	 * @throws Exception
	 */
	public static String initParams(String url, Map<String, String> params) throws Exception {
		if (null == params || params.isEmpty()) {
			return url;
		}
		StringBuilder sb = new StringBuilder(url);
		if (url.indexOf("?") == -1) {
			sb.append("?");
		}
		sb.append(map2Url(params));
		return sb.toString();
	}

	/**
	 * map构造url
	 * 
	 * @return 返回类型:
	 * @throws Exception
	 */
	public static String map2Url(Map<String, String> paramToMap) throws Exception {
		if (null == paramToMap || paramToMap.isEmpty()) {
			return null;
		}
		StringBuffer url = new StringBuffer();
		boolean isfist = true;
		for (Entry<String, String> entry : paramToMap.entrySet()) {
			if (isfist) {
				isfist = false;
			} else {
				url.append("&");
			}
			url.append(entry.getKey()).append("=");
			String value = entry.getValue();
			if (value != null && !"".equals(value.trim())) {
				url.append(URLEncoder.encode(value, DEFAULT_CHARSET));
			}
		}
		return url.toString();
	}

	/**
	 * 检测是否https
	 * 
	 * @param url
	 */
	private static boolean isHttps(String url) {
		return url.startsWith("https");
	}

	/**
	 * https 域名校验
	 * 
	 * @param url
	 * @param params
	 * @return
	 */
	public class TrustAnyHostnameVerifier implements HostnameVerifier {
		public boolean verify(String hostname, SSLSession session) {
			return true;// 直接返回true
		}
	}

	public static void main(String[] args) throws Exception {
		String url = "http://sdkv4test.qcwanwan.com/api/v1.0/cp/device/check";
		String params = "app_id=99&apple_id=222222&idfa=i1111,i2222";
		String signkey = "sign_key=27b758d2cb990f2f7ccd8c3a47b88df7";
		String sign = MD5Utils.MD5Encode(params + "&" + signkey);
		System.out.println(params + "&" + signkey);

		params = params + "&" + signkey + "&sign=" + sign;

		System.out.println(params);
		String result = HttpUtil.post(url, params);

		System.out.println(result);

		System.out.println(
				MD5Utils.MD5Encode("app_id=99&apple_id=222222&idfa=i1111&sign_key=27b758d2cb990f2f7ccd8c3a47b88df7"));
	}
}

/*
 * HTTPS忽略证书验证,防止高版本jdk因证书算法不符合约束条件,使用继承X509ExtendedTrustManager的方式
 */
class MyX509TrustManager extends X509ExtendedTrustManager {
	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
	}

	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return null;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1, Socket arg2) throws CertificateException {
	}

	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) throws CertificateException {
	}

	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1, Socket arg2) throws CertificateException {
	}

	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) throws CertificateException {
	}

}