package com.huylq.goezyticket;

import com.huylq.goezyticket.support.PostgresContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Smoke test: the whole context comes up against a real Postgres.
 *
 * <p>Pinned to the {@code test} profile and a container rather than the default {@code dev} profile,
 * which would start Docker Compose and talk to the local development database — making the build
 * depend on a running dev stack and, worse, letting tests write to it.
 */
@SpringBootTest
@ActiveProfiles("test")
class GoEzyTicketApplicationTests {

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", PostgresContainer::jdbcUrl);
    registry.add("spring.datasource.username", PostgresContainer::username);
    registry.add("spring.datasource.password", PostgresContainer::password);
  }

  @Test
  void contextLoads() {
  }

}
