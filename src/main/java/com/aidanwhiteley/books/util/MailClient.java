package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MailClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailClient.class);

    private static final String SPACES = "    ";
    private static final String LINE_SEPARATOR = "\r\n";

    private final JavaMailSender mailSender;

    @Value("${books.users.registrationAdminEmail.emailTitle}")
    private String registrationAdminEmailTitle;

    @Value("${books.users.registrationAdminEmail.emailContent}")
    private String registrationAdminEmailContent;

    @Value("${books.users.registrationAdminEmail.emailFrom}")
    private String registrationAdminEmailFrom;

    @Value("${books.users.registrationAdminEmail.emailTo}")
    private String registrationAdminEmailTo;

    @Autowired
    public MailClient(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendEmailsToAdminsForNewUsers(List<User> newUsers) {
        boolean emailSent = true;

        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);

            messageHelper.setFrom(registrationAdminEmailFrom);
            messageHelper.setTo(registrationAdminEmailTo);
            messageHelper.setSubject(registrationAdminEmailTitle);
            messageHelper.setText(prepareAdminNewUsersNotificationEmailContent(newUsers));
        };

        try {
            mailSender.send(messagePreparator);
        } catch (MailException me) {
            emailSent = false;
            LOGGER.error("Failed to send user registration emails for {}", newUsers, me);
        }

        return emailSent;
    }

    private String prepareAdminNewUsersNotificationEmailContent(List<User> newUsers) {
        StringBuilder message = new StringBuilder();
        message.append(registrationAdminEmailContent).append(LINE_SEPARATOR).append(LINE_SEPARATOR);
        newUsers.forEach(s -> message.append(s.getFullName()).append(SPACES).append(s.getEmail()).
                append(SPACES).append(s.getLink()).append(LINE_SEPARATOR));
        return message.toString();
    }

}
