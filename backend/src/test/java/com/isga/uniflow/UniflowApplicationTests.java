package com.isga.uniflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Démarre le contexte complet sur la base PostgreSQL Testcontainers
 * (plus besoin d'une base sur localhost:5432).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UniflowApplicationTests {

	@Test
	void contextLoads() {
	}

}
