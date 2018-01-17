package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.stereotype.Component;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.GOOGLE;

@Component
public class OauthAuthenticationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(OauthAuthenticationUtils.class);

    // TODO - fix this when supporting more than just Google logons
    @Value("${google.client.clientId}")
    private String securityOauth2ClientClientId;

    public User.AuthenticationProvider getAuthProviderFromAuth(OAuth2Authentication auth) {
        OAuth2Request storedRquest = auth.getOAuth2Request();
        String clientId = storedRquest.getClientId();

        if (clientId.equals(securityOauth2ClientClientId)) {
            return GOOGLE;
        } else {
            LOGGER.error("Unknown clientId specified of {} so cant determine authentication provider. Config value is {}", clientId, securityOauth2ClientClientId);
            throw new IllegalArgumentException("Uknown client id specified");
        }
    }

    public String getAuthProviderFromAuthAsString(OAuth2Authentication auth) {
        return this.getAuthProviderFromAuth(auth).toString();
    }
}
