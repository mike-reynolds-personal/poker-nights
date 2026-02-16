package com.langleydata.homepoker.gateway.controllers.session;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.langleydata.homepoker.api.session.TokenInfo;
import com.langleydata.homepoker.api.session.TokenResponse;
import com.langleydata.homepoker.services.HttpClientUtils;
import com.nimbusds.oauth2.sdk.util.StringUtils;

@Service
@ConfigurationProperties("spring.security.oauth2.client")
public final class GatewayTokenService {
	private final Logger logger = LoggerFactory.getLogger(GatewayTokenService.class);
	
	private static final String TOKEN_PATH = "/v1/token";
	public static final long TOKEN_EXPIRY_MARGIN = 30 * 1000;

	private String baseUrl = null;
	
	@Value("${server.port}")
	private String serverPort;

	@Autowired
	HttpClientUtils httpClientUtils;
	
	@Value("${spring.security.oauth2.client.registration.google.client-id}")
	private String googleClientId;
	@Value("${spring.security.oauth2.client.registration.google.client-secret}")
	private String googleClientSecret;
			
	private Gson gson = new Gson();
	
	/** Request a refresh of an access token
	 * 
	 * @param refreshToken
	 * @return Updated TokenInfo
	 * @throws IOException 
	 */
	public TokenInfo refreshToken(final String refreshToken) throws IOException {
		
		final String url = "temp_only" + TOKEN_PATH;
		final HttpPost request = buildEntity(refreshToken, "refresh_token");
		final String response = httpClientUtils.postForString(url, request); 
		final TokenInfo tokenInfo = new TokenInfo();
		
		if (StringUtils.isNotBlank(response)) {
			final TokenResponse tResponse = gson.fromJson(response, TokenResponse.class);
			tokenInfo.setTokens(tResponse);
		}
		
		return tokenInfo;
	}
	/** Every method attempted does not result in a refresh of the access 
	 * token (see tokenService.authorise() ) due to redirects, cookies or other things.
	 * For now, leaving to the browser to do the redirection and refresh, which seems to be 
	 * the only way without a lot more experimentation. There is possibly a way with a Webclient, 
	 * as that allows cookies, or potentially by using Spring-boot's Oauth2/ OAuth2RestTemplate 
	 * classes, but its more experimentation.
	 * 
	 * Links
	 * https://developer.okta.com/blog/2018/04/02/client-creds-with-spring-boot
	 * https://github.com/spring-projects/spring-security/blob/5.4.0/samples/boot/oauth2webclient/src/main/java/sample/config/WebClientConfig.java
	 * https://github.com/spring-projects/spring-security/wiki/OAuth-2.0-Migration-Guide
	 */
//	public void authorise() throws IOException {
//
//		//final String url = getBaseUri() + "/oauth2/authorization/okta";
//		final String url = auth2Props.getIssuer() + "/v1/authorize";
//
//		final String scopes = auth2Props.getScopes().stream()
//			.collect(Collectors.joining(" "));
//		URI uri = null;
//		try {
//			uri = new URIBuilder(url)
//					.addParameter("client_id", auth2Props.getClientId())
//					.addParameter("response_type", "code")
//					.addParameter("scope", scopes)
//					.addParameter("state", "abcd").build();
//		} catch (URISyntaxException e) {
//			logger.warn("Unable to build URI: " + e.getMessage());
//		}
//
//		
//		final String resp = httpClientUtils.getForString(uri);
//
//	}
	/** Build an APPLICATION_FORM_URLENCODED entity for a token request
	 * 
	 * @param token
	 * @param grantType
	 * @return
	 */
	private HttpPost buildEntity(final String token, final String grantType) {
		final HttpPost post = new HttpPost();
		
//		final HttpHeaders headers = new HttpHeaders();
		post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
		post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		
//		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
//		headers.setCacheControl(CacheControl.noCache());
		final String auth = Base64.encodeBase64URLSafeString((googleClientId + ":" + googleClientSecret).getBytes());
		post.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth);
		
		JsonObject body = new JsonObject();
		body.addProperty("refresh_token", token);
		body.addProperty("grant_type", grantType);
//		body.addProperty("redirect_uri", getBaseUri() + auth2Props.getRedirectUri());
		byte[] content = body.toString().getBytes();
		
		BasicHttpEntity requestBody = new BasicHttpEntity();
		 requestBody.setContent(new ByteArrayInputStream(content));
		    requestBody.setContentLength(content.length);

		    post.setEntity(requestBody);
	    
	    return post;
	}

	/** Get the server url
	 * 
	 * @return
	 */
	private String getBaseUri() {
		if (baseUrl == null) {
			baseUrl = InetAddress.getLoopbackAddress().getHostName() + ":" + serverPort;
			//baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		}
		return baseUrl;
	}
}
