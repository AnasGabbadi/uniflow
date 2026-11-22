package com.isga.uniflow;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base PostgreSQL de test, partagée par tous les tests d'intégration.
 *
 * - Même image que compose.yaml (postgres:16-alpine) : les migrations Flyway V1..V3 et les
 *   contraintes sont testées sur le vrai moteur, pas sur H2.
 * - {@code @ServiceConnection} : Spring Boot lit l'URL, l'utilisateur et le mot de passe du conteneur
 *   et remplace spring.datasource.* (DB_URL, DB_USERNAME, DB_PASSWORD ne sont plus nécessaires).
 * - Le conteneur est un champ static : un SEUL conteneur est démarré pour toute l'exécution Maven,
 *   même si plusieurs contextes Spring (@DataJpaTest, @SpringBootTest) sont créés.
 *   Il est détruit automatiquement à la fin de la JVM (Ryuk, conteneur compagnon de Testcontainers).
 *
 * Utilisation : {@code @Import(TestcontainersConfiguration.class)} sur la classe de test.
 * Prérequis : Docker démarré.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
