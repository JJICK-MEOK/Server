package com.jjikmeok.app.domain.activity.publicactivity.service;

import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalActivityGatewayTest {

    private final ExternalActivityGateway gateway = new ExternalActivityGateway();

    @Test
    void buildUrl_addsDynamicDatesAndPages() {
        String kopis = gateway.buildUrl(SourceType.KOPIS, "https://kopis.test/list", "key",
                LocalDate.of(2026, 5, 24), LocalDate.of(2026, 8, 24), 1);
        String exhibition = gateway.buildUrl(SourceType.EXHIBITION, "https://api.kcisa.kr/openapi/API_CCA_145/request",
                "key", LocalDate.of(2026, 5, 24), LocalDate.of(2026, 8, 24), 2);
        String seoulCulture = gateway.buildUrl(SourceType.SEOUL_CULTURE, "http://openapi.seoul.go.kr/key/json/List",
                "", LocalDate.of(2026, 5, 24), LocalDate.of(2026, 8, 24), 2);
        String seoulReservation = gateway.buildUrl(SourceType.SEOUL_RESERVATION, "http://openapi.seoul.go.kr/key/json/List",
                "", LocalDate.of(2026, 5, 24), LocalDate.of(2026, 8, 24), 2);

        assertThat(kopis).contains("stdate=20260524", "eddate=20260624", "cpage=1", "prfstate=02");
        assertThat(kopis).contains("service=key").doesNotContain("serviceKey=key");
        assertThat(exhibition).contains("pageNo=2", "numOfRows=100");
        assertThat(seoulCulture).endsWith("/101/200");
        assertThat(seoulReservation).contains("/101/200", "sortStdr=1");
    }

    @Test
    void buildUrl_keepsEncodedServiceKeyAndReplacesBlankKey() {
        String url = gateway.buildUrl(SourceType.EXHIBITION,
                "https://api.example.com/list?serviceKey=",
                "abc%2Fdef%3D",
                LocalDate.of(2026, 5, 24),
                LocalDate.of(2026, 8, 24),
                1);

        assertThat(url).contains("serviceKey=abc%2Fdef%3D");
        assertThat(url).doesNotContain("serviceKey=&");
        assertThat(url).doesNotContain("abc%252Fdef%253D");
    }

    @Test
    void fetchPage_decodesUtf8XmlEvenWhenHttpCharsetIsWrong() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/request", exchange -> {
            byte[] body = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <response><body><items><item><title>?쒖슱 ?꾩떆</title></item></items></body></response>
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/xml;charset=ISO-8859-1");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        ExternalActivityGateway.FetchedPayload payload;
        try {
            payload = gateway.fetchPage(
                    SourceType.EXHIBITION,
                    "http://localhost:" + server.getAddress().getPort() + "/api/request",
                    "key",
                    LocalDate.of(2026, 5, 24),
                    LocalDate.of(2026, 8, 24),
                    1
            );
        } finally {
            server.stop(0);
        }

        assertThat(payload.payload()).contains("?쒖슱 ?꾩떆");
    }

    @Test
    void fetchPage_retriesRetryableGatewayErrors() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/list", exchange -> {
            int attempt = attempts.incrementAndGet();
            if (attempt == 1) {
                byte[] body = "Error forwarding request to backend server".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(502, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
                return;
            }
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            gateway.fetchPage(
                    SourceType.EXHIBITION,
                    "http://localhost:" + server.getAddress().getPort() + "/list",
                    "key",
                    LocalDate.of(2026, 5, 24),
                    LocalDate.of(2026, 8, 24),
                    1
            );
        } finally {
            server.stop(0);
        }

        assertThat(attempts.get()).isEqualTo(2);
    }
}
