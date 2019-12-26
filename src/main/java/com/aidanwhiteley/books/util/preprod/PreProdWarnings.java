package com.aidanwhiteley.books.util.preprod;

import org.aspectj.apache.bcel.generic.LOOKUPSWITCH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class PreProdWarnings {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreProdWarnings.class);
    private static final String SEPARATOR = "**********************************************************************";
    private final Environment environment;

    @Autowired
    public PreProdWarnings(Environment environment) {
        this.environment = environment;
    }

    public void displayDataReloadWarningMessage() {
        String profiles = String.join(",", this.environment.getActiveProfiles());

        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("");
            LOGGER.warn(SEPARATOR);
            LOGGER.warn("*** WARNING!                                                       ***");
            LOGGER.warn("*** All data is deleted and dummy data reloaded when running with  ***");
            LOGGER.warn("*** the profile {} ***", padRight(profiles, 50));
            LOGGER.warn(SEPARATOR);
            LOGGER.warn("");
        }
    }

    public void displayMongoJavaServerWarningMessage() {
        String profiles = String.join(",", this.environment.getActiveProfiles());

        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("");
            LOGGER.warn(SEPARATOR);
            LOGGER.warn("*** WARNING!                                                       ***");
            LOGGER.warn("*** You are running with an in memory version of Mongo with the    ***");
            LOGGER.warn("*** profile {} ***", padRight(profiles, 54));
            LOGGER.warn(SEPARATOR);
            LOGGER.warn("");
        }
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
