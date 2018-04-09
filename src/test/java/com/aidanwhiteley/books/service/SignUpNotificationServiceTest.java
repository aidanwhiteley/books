package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.util.IntegrationTest;
import com.aidanwhiteley.books.util.MailClient;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.aidanwhiteley.books.domain.User.AuthenticationProvider.GOOGLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SignUpNotificationServiceTest extends IntegrationTest {

    // This value must match the value in the corresponding YAML config file.
    // Default port 25 shifted to allow easy use on Unix environments where
    // binding to ports less than 1024 requires root permissions.
    @SuppressWarnings("WeakerAccess")
    public static final int PORT = 3025;
    private static final String TEST_USER = "Test User";

    private GreenMail smtpServer;

    @Value("${books.users.registrationAdminEmail.emailContent}")
    private String registrationAdminEmailContent;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MailClient mailClient;

    @Before
    public void setUp() {
        smtpServer = new GreenMail(new ServerSetup(PORT, null, "smtp"));
        smtpServer.start();
    }

    @After
    public void tearDown() {
        smtpServer.stop();
    }

    @Test
    public void testNewRegistrationIsVisible() {
        SignUpNotificationService service = new SignUpNotificationService(userRepository, mailClient);
        User newUser = insertTestUser();

        List<User> newUsers = service.findNewUsers();
        assertEquals(1, newUsers.size());

        // Tidy up
        userRepository.delete(newUser);
    }

    @Test
    public void testNewRegistrationEmailedToAdmin() throws IOException, MessagingException {
        SignUpNotificationService service = new SignUpNotificationService(userRepository, mailClient);
        service.setRegistrationAdminEmailEnabled(true);
        User newUser = insertTestUser();

        List<User> newUsers = service.findNewUsers();
        assertEquals(1, newUsers.size());

        service.checkForNewUsersAndEmailAdmin();
        assertEmailNotificationReceived();

        newUsers = service.findNewUsers();
        assertEquals(0, newUsers.size());

        // Tidy up
        userRepository.delete(newUser);
    }

    private User insertTestUser() {
        User newUser = User.builder().authenticationServiceId(UUID.randomUUID().toString()).
                authProvider(GOOGLE).fullName(TEST_USER).build();
        userRepository.insert(newUser);
        return newUser;
    }

    private void assertEmailNotificationReceived() throws IOException, MessagingException {
        MimeMessage[] receivedMessages = smtpServer.getReceivedMessages();
        assertEquals(1, receivedMessages.length);
        String content = (String) receivedMessages[0].getContent();

        assertTrue(content.contains(TEST_USER));
        assertTrue(content.contains(registrationAdminEmailContent));
    }
}
