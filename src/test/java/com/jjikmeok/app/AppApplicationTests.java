package com.jjikmeok.app;

import com.jjikmeok.app.domain.ai.service.AiActivityParser;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:jjikmeok;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
		"spring.mail.username=test@example.com",
		"spring.mail.password=test-mail-password",
		"spring.autoconfigure.exclude=org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
		"spring.ai.openai.api-key=test-openai-key",
		"spring.cloud.aws.s3.bucket=test-bucket",
		"spring.cloud.aws.credentials.access-key=test-access-key",
		"spring.cloud.aws.credentials.secret-key=test-secret-key",
		"oauth2.kakao.client-id=test-kakao-client-id",
		"oauth2.google.client-id=test-google-client-id",
		"oauth2.google.client-secret=test-google-client-secret",
		"jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class AppApplicationTests {

	@MockitoBean
	AiActivityParser aiActivityParser;

	@MockitoBean
	VectorStore vectorStore;

	@Test
	void contextLoads() {
	}

}
