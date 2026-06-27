package com.jjikmeok.app.domain.activity.privateactivity.service;

import com.jjikmeok.app.domain.activity.privateactivity.enums.RobotsPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RobotsPolicyService {

    private final RestClient restClient = RestClient.create();

    public RobotsPolicy evaluate(URI sourceUri) {
        if (sourceUri == null || sourceUri.getHost() == null || sourceUri.getHost().isBlank()) {
            return RobotsPolicy.UNKNOWN;
        }

        URI robotsUri = buildRobotsUri(sourceUri);
        try {
            String robotsText = restClient.get().uri(robotsUri).retrieve().body(String.class);
            if (robotsText == null || robotsText.isBlank()) {
                return RobotsPolicy.ALLOWED;
            }
            return parseRobots(robotsText, sourceUri.getPath());
        } catch (Exception e) {
            log.debug("[Discovery] robots.txt 확인에 실패했습니다. url={}", sourceUri, e);
            return RobotsPolicy.UNKNOWN;
        }
    }

    private RobotsPolicy parseRobots(String robotsText, String path) {
        String normalizedPath = path == null || path.isBlank() ? "/" : path;
        boolean wildcard = false;
        boolean active = false;
        List<String> rules = new ArrayList<>();

        for (String line : robotsText.split("\\R")) {
            String trimmed = stripInlineComment(line);
            if (trimmed.isBlank()) {
                active = false;
                wildcard = false;
                continue;
            }

            int colon = trimmed.indexOf(':');
            if (colon < 0) {
                continue;
            }

            String key = trimmed.substring(0, colon).trim().toLowerCase();
            String value = trimmed.substring(colon + 1).trim();
            if ("user-agent".equals(key)) {
                wildcard = "*".equals(value);
                active = wildcard;
                continue;
            }

            if (active && "disallow".equals(key)) {
                if (value.isBlank()) {
                    continue;
                }
                rules.add(value);
            }
        }

        for (String rule : rules) {
            if ("/".equals(rule) || normalizedPath.startsWith(rule)) {
                return RobotsPolicy.DISALLOWED;
            }
        }
        return RobotsPolicy.ALLOWED;
    }

    private String stripInlineComment(String line) {
        if (line == null) {
            return "";
        }
        return line.split("#", 2)[0].trim();
    }

    private URI buildRobotsUri(URI sourceUri) {
        return URI.create("%s://%s%s/robots.txt".formatted(
                sourceUri.getScheme(),
                sourceUri.getHost(),
                sourceUri.getPort() >= 0 ? ":" + sourceUri.getPort() : ""
        ));
    }
}
