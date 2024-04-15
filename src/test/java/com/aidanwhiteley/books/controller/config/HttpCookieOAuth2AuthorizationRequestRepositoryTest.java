package com.aidanwhiteley.books.controller.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.aidanwhiteley.books.controller.exceptions.JwtAuthAuzException;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

	public static final String TEST_CLIENT_ID = "Hello world";
	public static final String DUMMY_TEXT_NOT_TESTED = "http://example.com/example";

	@Test
	void testSaveAndLoadCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		OAuth2AuthorizationRequest authorizationRequest =
				OAuth2AuthorizationRequest.authorizationCode().clientId(TEST_CLIENT_ID).
						authorizationUri(DUMMY_TEXT_NOT_TESTED).build();

		HttpCookieOAuth2AuthorizationRequestRepository repo =
				new HttpCookieOAuth2AuthorizationRequestRepository(WebSecurityConfiguration.getAuthRequestJsonMapper());

		repo.saveAuthorizationRequest(authorizationRequest, request, response);

		// Now get the cookie that should have been added to the response
		Cookie cookie = response.getCookie(HttpCookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);

		request.setCookies(cookie);

		OAuth2AuthorizationRequest retrievedOauth = repo.loadAuthorizationRequest(request);
		String clientId = retrievedOauth.getClientId();

		assertEquals(TEST_CLIENT_ID, clientId);
	}

	@Test
	void testNoAuthClearsCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        HttpCookieOAuth2AuthorizationRequestRepository repo =
                new HttpCookieOAuth2AuthorizationRequestRepository(WebSecurityConfiguration.getAuthRequestJsonMapper());

        Cookie cookie = new Cookie(HttpCookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, "dummy");
        request.setCookies(cookie);
        repo.saveAuthorizationRequest(null, request, response);

        Cookie[] cookies = response.getCookies();
        assertEquals(1, cookies.length);
        assertEquals(0, cookies[0].getMaxAge());
        assertTrue(cookies[0].getValue().isEmpty());

    }

	@Test
	void testAuthToJsonProcessingException() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		OAuth2AuthorizationRequest authorizationRequest =
				OAuth2AuthorizationRequest.authorizationCode().clientId(TEST_CLIENT_ID).
						authorizationUri(DUMMY_TEXT_NOT_TESTED).build();

		ObjectMapper om = mock(ObjectMapper.class);
		when(om.writeValueAsString(any())).thenThrow(new JsonProcessingException("This is an expected exception for this test") {});
		when(om.readValue(anyString(), eq(OAuth2AuthorizationRequest.class))).
				thenThrow(new JsonProcessingException("This is another expected exception for this test") {});

		HttpCookieOAuth2AuthorizationRequestRepository repo = new HttpCookieOAuth2AuthorizationRequestRepository(om);

		// We dont want expected exception logs cluttering up test logs
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(HttpCookieOAuth2AuthorizationRequestRepository.class).setLevel(Level.valueOf("OFF"));

		assertThrows(JwtAuthAuzException.class, () ->
				repo.saveAuthorizationRequest(authorizationRequest, request, response));

		Cookie aCookie = new Cookie(HttpCookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, "dummyVal");
		request.setCookies(new Cookie[]{aCookie});
		assertThrows(JwtAuthAuzException.class, () ->
				repo.loadAuthorizationRequest(request));

		context.getLogger(HttpCookieOAuth2AuthorizationRequestRepository.class).setLevel(Level.valueOf("WARN"));

		verify(om, times(1)).writeValueAsString(authorizationRequest);
	}

}
