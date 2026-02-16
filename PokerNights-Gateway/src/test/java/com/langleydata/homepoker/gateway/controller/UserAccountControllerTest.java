package com.langleydata.homepoker.gateway.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.langleydata.homepoker.api.AccountLevel;
import com.langleydata.homepoker.api.AccountService;
import com.langleydata.homepoker.api.UserAccount;
import com.langleydata.homepoker.api.session.UserSession;
import com.langleydata.homepoker.exception.GameSchedulingException;
import com.langleydata.homepoker.services.Account;

import reactor.core.publisher.Mono;

@RunWith(MockitoJUnitRunner.class)
public class UserAccountControllerTest {

	@Mock
	private OAuth2User user;
	@Mock
	private OAuth2AuthenticationToken authToken;
	@Mock
	private OAuth2AuthorizedClient client;
	@Mock
	private ReactiveOAuth2AuthorizedClientService authService;
	
	@Mock
	private AccountService accountService;
	
	@InjectMocks
	private UserAccountController gc;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		gc.defaultAccLevel = "TIER_1";
		when(user.getName()).thenReturn("1234");
		when(authToken.getAuthorizedClientRegistrationId()).thenReturn("google");
		when(authToken.getName()).thenReturn("name");
		
		OAuth2AccessToken at = mock(OAuth2AccessToken.class);
		when(at.getExpiresAt()).thenReturn(Instant.now());
		when(client.getAccessToken()).thenReturn(at);
		when(client.getRefreshToken()).thenReturn(mock(OAuth2RefreshToken.class));
		doReturn(Mono.just(client)).when(authService).loadAuthorizedClient( eq("google"), eq("name"));
	}
	
	@Test
	public void testGetSessionWithExistingFullAccount() throws GameSchedulingException {
		UserAccount existing = new Account.Builder("1234", "bob123")
				.email("bob@bob")
				.givenName("Bob")
				.familyName("Smith")
				.fullName("Bob Smith")
				.locale("en-gb")
				.picture("here")
				.accLevel(AccountLevel.TIER_1)
				.build();
		
		when(accountService.getAccount("1234")).thenReturn(existing);
		
		
		UserSession ret = gc.getSession(user, authToken);
		assertNotNull(ret);
		
		assertEquals("1234", ret.getPlayerId());
		assertEquals("bob123", ret.getPlayerHandle());
		assertEquals("bob@bob", ret.getEmail());
		assertEquals("Bob", ret.getGivenName());
		assertEquals("Smith", ret.getFamilyName());
		assertEquals("Bob Smith", ret.getFullName());
		assertEquals("en-gb", ret.getLocale());
		assertEquals("here", ret.getPicture());
		
		assertNotNull(ret.getTokens().getAccessToken());
		assertNotNull(ret.getTokens().getRefreshToken());
		assertEquals(AccountLevel.TIER_1, ret.getAccLevel());
		assertNotNull(ret.getStats());
		verify(accountService, Mockito.never()).saveAccount(ret);
	}
	
	@Test
	@Ignore //TODO Needs to change as accounts are now created on authentication success
	public void testGetSessionWithNoExistingAccount() throws GameSchedulingException {
		
		when(user.getAttribute("email")).thenReturn("bob@bob");
		when(user.getAttribute("nickname")).thenReturn("bob123");
		when(user.getAttribute("given_name")).thenReturn("Bob");
		when(user.getAttribute("family_name")).thenReturn("Smith");
		when(user.getAttribute("name")).thenReturn("Bob Smith");
		when(user.getAttribute("locale")).thenReturn("en-gb");
		when(user.getAttribute("picture")).thenReturn("here");
		
		when(accountService.getAccount("1234")).thenReturn(null);
		
		UserSession ret = gc.getSession(user, authToken);
		assertNotNull(ret);
		
		assertEquals("1234", ret.getPlayerId());
		assertEquals("bob123", ret.getPlayerHandle());
		assertEquals("bob@bob", ret.getEmail());
		assertEquals("Bob", ret.getGivenName());
		assertEquals("Smith", ret.getFamilyName());
		assertEquals("Bob Smith", ret.getFullName());
		assertEquals("en-gb", ret.getLocale());
		assertEquals("here", ret.getPicture());
		
		assertNotNull(ret.getTokens().getAccessToken());
		assertNotNull(ret.getTokens().getRefreshToken());
		assertEquals(AccountLevel.TIER_1, ret.getAccLevel());
		assertNotNull(ret.getStats());
		
		verify(accountService).saveAccount(ret);
	}
	
	@Test
	public void testGetSessionWithNoUser() {
		UserSession ret = gc.getSession(null, null);
		assertEquals("", ret.getPlayerId());
		assertEquals("", ret.getPlayerHandle());
		assertNull(ret.getTokens());
		
	}
	
	
}
