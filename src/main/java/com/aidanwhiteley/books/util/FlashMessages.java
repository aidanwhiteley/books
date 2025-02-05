package com.aidanwhiteley.books.util;

import com.innoq.spring.cookie.flash.CookieFlashMapManager;
import com.innoq.spring.cookie.flash.codec.jackson.JacksonFlashMapListCodec;
import com.innoq.spring.cookie.security.CookieValueSigner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;

@Component
public class FlashMessages {

    private final CookieFlashMapManager cookieFlashMapManager;

    public FlashMessages(@Value("${books.flashmessages.hmacKey}") String flashMessagesHmacKey,
                         @Value("${books.flashmessages.cookieName}") String flashMessagesCookieName) {
        this.cookieFlashMapManager = new CookieFlashMapManager(
            JacksonFlashMapListCodec.create(),
                CookieValueSigner.hmacSha1(flashMessagesHmacKey),
                flashMessagesCookieName);
    }

    public void storeFlashMessage(String key, String value, HttpServletRequest request, HttpServletResponse response) {
        var flashMap = new FlashMap();
        flashMap.put(key, value);
        cookieFlashMapManager.saveOutputFlashMap(flashMap, request, response);
    }

    public String retrieveFlashMessage(String key, HttpServletRequest request, HttpServletResponse response) {
        FlashMap flashMap = cookieFlashMapManager.retrieveAndUpdate(request, response);
        if (flashMap != null && flashMap.containsKey(key)) {
            return flashMap.get(key).toString();
        } else {
            return "";
        }
    }


}
