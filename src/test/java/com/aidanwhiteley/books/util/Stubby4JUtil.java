package com.aidanwhiteley.books.util;

import io.github.azagniotov.stubby4j.client.StubbyClient;

import java.io.File;

public class Stubby4JUtil {

    private static final int STUBBY_TEST_PORT = 8883;
    private static StubbyClient stubby = null;
    private static final String STUBBY_CONFIG_YAML = "stubs.yaml";

    public static void configureStubServer() throws Exception {
        stubby = new StubbyClient();
        File resourcesDirectory = new File("src/test/resources");
        String filePath = resourcesDirectory.getAbsolutePath();

        // We set explicit ports here to try to avoid
        // "MultiException: Multiple exceptions" if a manually run version of
        // Stubby is still active
        stubby.startJetty(STUBBY_TEST_PORT, STUBBY_TEST_PORT + 1,
                STUBBY_TEST_PORT + 2, filePath + "/stubs/stubserver/" + STUBBY_CONFIG_YAML);
    }

    public static void stopStubServer() throws Exception {
        if (stubby != null) {
            stubby.stopJetty();
        }
    }
}
