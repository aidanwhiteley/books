package com.aidanwhiteley.books.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
public class FlashMessagesTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

}
