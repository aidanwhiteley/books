package com.aidanwhiteley.books.controller.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import javax.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

	@Test
	void testSaveAndLoadCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		String TEST_CLIENT_ID = "Hello world";
		String DUMMY_TEXT_NOT_TESTED = "http://example.com/example";

		OAuth2AuthorizationRequest authorizationRequest =
				OAuth2AuthorizationRequest.authorizationCode().clientId(TEST_CLIENT_ID).
				authorizationUri(DUMMY_TEXT_NOT_TESTED).build();
		
		
		HttpCookieOAuth2AuthorizationRequestRepository repo = 
				new HttpCookieOAuth2AuthorizationRequestRepository();
		
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
                new HttpCookieOAuth2AuthorizationRequestRepository();

        Cookie cookie = new Cookie(HttpCookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, "dummy");
        request.setCookies(cookie);
        repo.saveAuthorizationRequest(null, request, response);

        Cookie[] cookies = response.getCookies();
        assertEquals(1, cookies.length);
        assertEquals(0, cookies[0].getMaxAge());
        assertTrue(cookies[0].getValue().isEmpty());

    }

}
