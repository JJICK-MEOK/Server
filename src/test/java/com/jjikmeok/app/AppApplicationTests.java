package com.jjikmeok.app;

import com.jjikmeok.app.domain.ai.service.AiActivityParser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:jjikmeok;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",

		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",

		"spring.flyway.enabled=false",

		"spring.data.redis.repositories.enabled=false",

		"spring.mail.username=test@example.com",
		"spring.mail.password=test-mail-password",

		"spring.ai.openai.api-key=test-openai-key",

		"storage.type=local",
		"storage.local.directory=./build/test-uploads",
		"storage.local.public-base-url=http://localhost:8080/uploads",

		"oauth2.kakao.client-id=test-kakao-client-id",
		"oauth2.kakao.redirect-uri=http://localhost:8080/oauth/kakao/callback",

		"oauth2.google.client-id=test-google-client-id",
		"oauth2.google.client-secret=test-google-client-secret",
		"oauth2.google.redirect-uri=http://localhost:8080/oauth/google/callback",

		"oauth2.naver.client-id=test-naver-client-id",
		"oauth2.naver.client-secret=test-naver-client-secret",
		"oauth2.naver.redirect-uri=http://localhost:8080/oauth/naver/callback",

		"jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class AppApplicationTests {

	@MockitoBean
	private AiActivityParser aiActivityParser;

	@Test
	void contextLoads() {
	}
}