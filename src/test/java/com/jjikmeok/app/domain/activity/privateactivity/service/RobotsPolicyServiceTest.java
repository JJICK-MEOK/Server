package com.jjikmeok.app.domain.activity.privateactivity.service;

import com.jjikmeok.app.domain.activity.privateactivity.enums.RobotsPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class RobotsPolicyServiceTest {

    private final RobotsPolicyService robotsPolicyService = new RobotsPolicyService();

    @Test
    void parseRobots_ignoresInlineCommentInDisallowRule() {
        RobotsPolicy policy = ReflectionTestUtils.invokeMethod(
                robotsPolicyService,
                "parseRobots",
                "User-agent: *\nDisallow: /private # inline comment\n",
                "/private/page"
        );

        assertThat(policy).isEqualTo(RobotsPolicy.DISALLOWED);
    }

    @Test
    void buildRobotsUri_preservesNonDefaultPort() {
        URI robotsUri = ReflectionTestUtils.invokeMethod(
                robotsPolicyService,
                "buildRobotsUri",
                URI.create("https://example.com:8443/path?q=1")
        );

        assertThat(robotsUri).hasToString("https://example.com:8443/robots.txt");
    }
}
