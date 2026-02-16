package com.langleydata.homepoker.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpClientUtils {
	protected final Logger logger = LoggerFactory.getLogger(HttpClientUtils.class);
	private CloseableHttpClient mkClient = null;
	private HttpClientBuilder clientBuilder = null;
	
	
	// Only used for local testing
	@Value("${server.ssl.key-store-type:null}")
	private String keyStoreType;
	
	@Value("${server.ssl.key-store:null}")
	private String keyStoreLocation;

	@Value("${server.ssl.key-store-password:null}")
	private String keyStorePwd;
	
	/** Set a client to use - Primarily for testing
	 * 
	 * @param client
	 */
	void setClient(CloseableHttpClient client) {
		this.mkClient = client;
	}
	
	/** Get the HttpClientBuilder and cache locally
	 * 
	 * @return
	 */
	HttpClientBuilder getBuilder() {
		if (clientBuilder == null) {
			clientBuilder = "null".equals(keyStoreLocation) ? HttpClients.custom() : buildClientWithSSL();
		}
		return clientBuilder;
	}

	/** Get a HTTP Client with loaded SSL context
	 * 
	 * @return
	 */
	public CloseableHttpClient getClient() {
		if (mkClient == null) {
			getBuilder();
		}
		if (clientBuilder !=null) {
			mkClient = clientBuilder.build();
		}
		return mkClient;
	}
	
	/** Do an HTTP POST for a String response
	 * 
	 * @param uri
	 * @return
	 * @throws IOException
	 */
	public String postForString(final String uri, HttpPost postRequest) throws IOException {
		
		try (CloseableHttpClient client = getClient()) {
			logger.debug("POST request to {}", uri);
			postRequest.setURI(URI.create(uri));
			return client.execute(postRequest, resp -> IOUtils.toString(resp.getEntity().getContent(), Charset.defaultCharset()));
	
		} catch (Exception e) {
			throw new IOException("Doing POST request to: " + e.getMessage());
		}
	}
	
	/** Do an HTTP GET for a String response
	 * 
	 * @param uri The GET request URI
	 * @return The String response
	 * @throws IOException
	 */
 	public String getForString(final URI uri) throws IOException {
		
		try (CloseableHttpClient client = getClient()) {
			logger.trace("GET request to {}", uri);
			final HttpGet request = new HttpGet(uri);
			return client.execute(request, resp -> IOUtils.toString(resp.getEntity().getContent(), Charset.defaultCharset()));
		} catch (Exception e) {
			throw new IOException("Doing GET request", e);
		}
	}
	/**
	 * 
	 */
	private HttpClientBuilder buildClientWithSSL() {

		logger.debug("Loading keystore...");
		final char[] password = keyStorePwd.toCharArray();
		SSLContext sslContext;

		// Load the keystore
		KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance(keyStoreType);
			
			//File key = ResourceUtils.getFile(keyStoreLocation);
			try (InputStream in = this.getClass().getResourceAsStream(keyStoreLocation)) {
				keyStore.load(in, password);
			}
		} catch (Exception e) {
			logger.error("Getting keystore", e);
			return null;
		}

		try {
			sslContext = SSLContextBuilder.create()
					.loadKeyMaterial(keyStore, password)
					.loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
			final SSLConnectionSocketFactory sslConSocFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
			final HttpClientBuilder clientbuilder = HttpClients.custom();
			clientbuilder.setSSLSocketFactory(sslConSocFactory);
			logger.debug("Successfully loaded SSL context");
			return clientbuilder;
			
		} catch (Exception e) {
			logger.error("Build SSL Context", e);
		}
		return null;
	}

}
