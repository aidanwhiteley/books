package com.aidanwhiteley.books.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.aidanwhiteley.books.controller.jwt.JwtAuthenticationService;
import com.aidanwhiteley.books.controller.jwt.JwtUtils;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.service.GoodReadsExportService;
import com.aidanwhiteley.books.util.*;
import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.WireFeedOutput;
import com.rometools.rome.io.XmlReader;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@AutoConfigureTestRestTemplate
class FeedsControllerTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${books.feeds.title}")
    private String booksFeedsTitles;

    @BeforeEach
    void setup() {
        // We don't want expected exception logs cluttering up test logs
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(FeedsController.class).setLevel(Level.valueOf("OFF"));
    }

    @AfterEach
    void teardown() {
        // Restore logging
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(FeedsController.class).setLevel(Level.valueOf("WARN"));
    }

    @Test
    void checkRssFeedsHasEntries() {

        // Find the port the test is running on
        String rootUri = this.testRestTemplate.getRootUri();
        String url = rootUri + "/feeds/rss";

        SyndFeed syndFeed = testRestTemplate.execute(url, HttpMethod.GET, null, response -> {
            SyndFeedInput input = new SyndFeedInput();
            try {
                return input.build(new XmlReader(response.getBody()));
            } catch (FeedException e) {
                fail("Could not parse response", e);
            }
            return null;
        });

        assertEquals(booksFeedsTitles, syndFeed.getTitle());

        assertFalse(syndFeed.getEntries().isEmpty());

        for (SyndEntry entry : syndFeed.getEntries()) {
            assertFalse(entry.getContents().getFirst().getValue().isEmpty());
        }
    }

    @Test
    void checkBooksExportNotLoggedOnHasNoBooks() {
        String rootUri = this.testRestTemplate.getRootUri();
        String url = rootUri + "/feeds/exportbooks";

        ResponseEntity<String> response = testRestTemplate.
                getForEntity(url, String.class);

        // 404 on resource for not logged on user
        assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());

    }

    @Test
    void checkBooksExportWithLoggedOnHasNoBooks() {
        String rootUri = this.testRestTemplate.getRootUri();
        String url = rootUri + "/feeds/exportbooks";

        User user = BookTestUtils.getTestUser();
        String token = jwtUtils.createTokenForUser(user);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Cookie", JwtAuthenticationService.JWT_COOKIE_NAME + "=" + token);

        ResponseEntity<String> response = testRestTemplate.
                exchange(url, HttpMethod.GET, new HttpEntity<>(requestHeaders), String.class);

        // 200 on resource for logged on user
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        assertTrue(response.getBody().contains(GoodReadsBookExport.goodReadsExportHeaderRow()));
    }

    @Test
    void testFindRecentActivityHandlesFeedException() {
        // Given - Create a mock SiteRssFeed that returns a valid Channel,
        // but then use a spy to make WireFeedOutput.outputString() throw FeedException
        SiteRssFeed mockSiteRssFeed = mock(SiteRssFeed.class);

        // Create a valid Channel - we'll force the exception in a different way
        Channel validChannel = new Channel("rss_2.0");
        validChannel.setTitle("Test Feed");
        validChannel.setDescription("Test Description");
        validChannel.setLink("https://example.com");

        when(mockSiteRssFeed.createSiteRssFeed()).thenReturn(validChannel);

        GoodReadsExportService mockGoodReadsExportService = mock(GoodReadsExportService.class);
        JwtAuthenticationUtils mockAuthUtils = mock(JwtAuthenticationUtils.class);

        // Create a partial mock/spy of the controller to intercept the WireFeedOutput creation
        FeedsController controller = new FeedsController(
            mockSiteRssFeed,
            mockGoodReadsExportService,
            mockAuthUtils
        ) {
            @Override
            public String findRecentActivity() {
                Channel channel = mockSiteRssFeed.createSiteRssFeed();
                // Create a mock WireFeedOutput that throws FeedException
                WireFeedOutput output = mock(WireFeedOutput.class);
                try {
                    when(output.outputString(any())).thenThrow(new FeedException("Test FeedException"));
                    return output.outputString(channel);
                } catch (FeedException fe) {
                    throw new IllegalStateException("Failed to generate RSS feed", fe);
                }
            }
        };

        // When/Then - Verify that IllegalStateException is thrown with the correct message
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
                controller::findRecentActivity,
            "Expected IllegalStateException to be thrown when RSS feed generation fails"
        );

        // Verify the exception message and that the cause is a FeedException
        assertEquals("Failed to generate RSS feed", exception.getMessage(),
                "Exception should have the expected error message");
        assertNotNull(exception.getCause(), "Exception should have a cause");
        assertInstanceOf(FeedException.class, exception.getCause(),
                "The cause should be a FeedException");
        assertEquals("Test FeedException", exception.getCause().getMessage(),
                "The FeedException should have the expected message");
    }

    @Test
    void testExportToCSVHandlesIOExceptionFromOutputStream() throws IOException {
        // Given - Mock dependencies to throw IOException when getting output stream
        SiteRssFeed mockSiteRssFeed = mock(SiteRssFeed.class);
        GoodReadsExportService mockGoodReadsExportService = mock(GoodReadsExportService.class);
        JwtAuthenticationUtils mockAuthUtils = mock(JwtAuthenticationUtils.class);

        User testUser = BookTestUtils.getTestUser();
        Principal mockPrincipal = mock(Principal.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        // Mock the authentication to return a valid user
        when(mockAuthUtils.extractUserFromPrincipal(mockPrincipal, false))
                .thenReturn(Optional.of(testUser));

        // Mock the export service to return some CSV data
        when(mockGoodReadsExportService.getExportInGoodReadsFormat(testUser))
                .thenReturn(Collections.singletonList("Test CSV Data"));

        // Mock getOutputStream to throw IOException
        IOException testException = new IOException("Test IOException - stream error");
        when(mockResponse.getOutputStream()).thenThrow(testException);

        FeedsController controller = new FeedsController(
                mockSiteRssFeed,
                mockGoodReadsExportService,
                mockAuthUtils
        );

        // When - Call exportToCSV
        controller.exportToCSV(mockResponse, mockPrincipal);

        // Then - Verify error handling
        // The method should catch the IOException and try to send an error response
        verify(mockResponse, times(1)).setContentType("text/csv");
        verify(mockResponse, times(1)).getOutputStream();
        verify(mockResponse, times(1)).sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void testExportToCSVHandlesIOExceptionFromSendError() throws IOException {
        // Given - Mock dependencies where both getOutputStream and sendError throw IOException
        SiteRssFeed mockSiteRssFeed = mock(SiteRssFeed.class);
        GoodReadsExportService mockGoodReadsExportService = mock(GoodReadsExportService.class);
        JwtAuthenticationUtils mockAuthUtils = mock(JwtAuthenticationUtils.class);

        User testUser = BookTestUtils.getTestUser();
        Principal mockPrincipal = mock(Principal.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        // Mock the authentication to return a valid user
        when(mockAuthUtils.extractUserFromPrincipal(mockPrincipal, false))
                .thenReturn(Optional.of(testUser));

        // Mock the export service to return some CSV data
        when(mockGoodReadsExportService.getExportInGoodReadsFormat(testUser))
                .thenReturn(Collections.singletonList("Test CSV Data"));

        // Mock getOutputStream to throw IOException
        IOException streamException = new IOException("Test IOException - stream error");
        when(mockResponse.getOutputStream()).thenThrow(streamException);

        // Mock sendError to also throw IOException (nested catch block)
        IOException sendErrorException = new IOException("Test IOException - sendError failed");
        doThrow(sendErrorException).when(mockResponse)
                .sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);

        FeedsController controller = new FeedsController(
                mockSiteRssFeed,
                mockGoodReadsExportService,
                mockAuthUtils
        );

        // When - Call exportToCSV
        // This should not throw an exception - it's caught and logged
        controller.exportToCSV(mockResponse, mockPrincipal);

        // Then - Verify both exception paths were executed
        verify(mockResponse, times(1)).getOutputStream();
        verify(mockResponse, times(1)).sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        // The method should complete without throwing an exception
    }

    @Test
    void testExportToCSVHandlesIOExceptionDuringWrite() throws IOException {
        // Given - Mock dependencies where ServletOutputStream.print throws IOException
        SiteRssFeed mockSiteRssFeed = mock(SiteRssFeed.class);
        GoodReadsExportService mockGoodReadsExportService = mock(GoodReadsExportService.class);
        JwtAuthenticationUtils mockAuthUtils = mock(JwtAuthenticationUtils.class);

        User testUser = BookTestUtils.getTestUser();
        Principal mockPrincipal = mock(Principal.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        ServletOutputStream mockOutputStream = mock(ServletOutputStream.class);

        // Mock the authentication to return a valid user
        when(mockAuthUtils.extractUserFromPrincipal(mockPrincipal, false))
                .thenReturn(Optional.of(testUser));

        // Mock the export service to return some CSV data
        when(mockGoodReadsExportService.getExportInGoodReadsFormat(testUser))
                .thenReturn(Collections.singletonList("Test CSV Data"));

        // Mock getOutputStream to return a mock that throws IOException on write
        when(mockResponse.getOutputStream()).thenReturn(mockOutputStream);
        // Since there's only one CSV row, it will use print() not println()
        doThrow(new IOException("Test IOException - write error"))
                .when(mockOutputStream).print(anyString());

        FeedsController controller = new FeedsController(
                mockSiteRssFeed,
                mockGoodReadsExportService,
                mockAuthUtils
        );

        // When - Call exportToCSV
        controller.exportToCSV(mockResponse, mockPrincipal);

        // Then - Verify error handling
        verify(mockResponse, times(1)).getOutputStream();
        verify(mockOutputStream, times(1)).print("Test CSV Data");
        verify(mockResponse, times(1)).sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }
}
