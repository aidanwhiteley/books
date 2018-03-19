package com.aidanwhiteley.books.service;

import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.util.MailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SignUpNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignUpNotificationService.class);

    @Value("${books.users.registrationAdminEmail.enabled}")
    private boolean registrationAdminEmailEnabled;

    private final UserRepository userRepository;
    private final MailClient mailClient;

    @Autowired
    public SignUpNotificationService(UserRepository userRepository, MailClient mailClient) {
        this.userRepository = userRepository;
        this.mailClient = mailClient;
    }

    public List<User> findNewUsers() {
        return userRepository.findAllByAdminEmailedAboutSignupIsFalse();
    }


    @Scheduled(cron = "${books.users.registrationAdminEmail.cron}")
    public void checkForNewUsersAndEmailAdmin() {

        if (registrationAdminEmailEnabled) {

            List<User> newUsers = findNewUsers();

            if (newUsers.isEmpty()) {
                LOGGER.debug("No new user registration found so no emails to the admin at {}", LocalDateTime.now());
            } else {
                boolean emailsSent = mailClient.sendEmailsToAdminsForNewUsers(newUsers);

                if (emailsSent) {
                    newUsers.forEach(userRepository::updateUserAdminNotified);
                }
                LOGGER.debug("Command issued to send new user registration emails to the admin for users: {} at {}",
                        newUsers, LocalDateTime.now());
            }
        } else {
            LOGGER.debug("Did not send any new user registration emails to the admin at {}", LocalDateTime.now());
        }
    }


    public void setRegistrationAdminEmailEnabled(boolean registrationAdminEmailEnabled) {
        this.registrationAdminEmailEnabled = registrationAdminEmailEnabled;
    }
}
