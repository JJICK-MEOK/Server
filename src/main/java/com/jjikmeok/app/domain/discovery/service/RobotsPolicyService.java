package com.jjikmeok.app.domain.discovery.service;

import com.jjikmeok.app.domain.discovery.enums.RobotsPolicy;
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

        URI robotsUri = URI.create(sourceUri.getScheme() + "://" + sourceUri.getHost() + "/robots.txt");
        try {
            String robotsText = restClient.get().uri(robotsUri).retrieve().body(String.class);
            if (robotsText == null || robotsText.isBlank()) {
                return RobotsPolicy.ALLOWED;
            }
            return parseRobots(robotsText, sourceUri.getPath());
        } catch (Exception e) {
            log.debug("[Discovery] robots.txt 확인 실패. url={}", sourceUri, e);
            return RobotsPolicy.UNKNOWN;
        }
    }

    private RobotsPolicy parseRobots(String robotsText, String path) {
        String normalizedPath = path == null || path.isBlank() ? "/" : path;
        boolean wildcard = false;
        boolean active = false;
        List<String> rules = new ArrayList<>();

        for (String line : robotsText.split("\\R")) {
            String trimmed = line.trim();
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
}
