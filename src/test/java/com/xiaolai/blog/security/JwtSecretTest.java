package com.xiaolai.blog.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class JwtSecretTest {
    @Test
    void productionRejectsRepositoryDefaultSecret() {
        for (String profile : new String[]{"prod", "production"}) {
            var environment = new MockEnvironment();
            environment.setActiveProfiles(profile);
            assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(JwtTokenProvider.DEFAULT_DEV_SECRET, 60000, environment));
        }
    }

    @Test
    void productionAcceptsAnIndependentSecretAndValidatesTokens() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        var provider = new JwtTokenProvider("a-random-test-secret-used-only-in-this-unit-test-123456789", 60000, environment);
        assertEquals("test", provider.parseClaims(provider.generateToken("test", "user", 0)).getSubject());
    }
}
