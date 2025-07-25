package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.BookRepository;
import com.aidanwhiteley.books.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoodReadsExportServiceTest extends IntegrationTest {

    @Autowired
    private GoodReadsExportService goodReadsExportService;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void testGoodReadsExportHasExpectedRows()  {
        User user = new User();
        user.setAuthenticationServiceId("107641352409228521888");
        user.setAuthProvider(User.AuthenticationProvider.GOOGLE);
       var output = goodReadsExportService.getExportInGoodReadsFormat(user);

       final int headerRows = 1;
       final long rowsInDataBase = bookRepository.findAll().stream().filter(s -> s.isOwner(user)).count();
       assertEquals(headerRows + rowsInDataBase, output.size());
    }
}
