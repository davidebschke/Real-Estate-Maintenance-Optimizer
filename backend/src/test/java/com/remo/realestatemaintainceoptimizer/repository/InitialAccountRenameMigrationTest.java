package com.remo.realestatemaintainceoptimizer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies that migration V4 renames the productive account to its neutral default name, keeps its password hash and leaves an account renamed by its owner untouched.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class InitialAccountRenameMigrationTest {

    private static final String INITIAL_ACCOUNT_ID = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void useProjectSchemaAndClearUsers() {
        jdbcTemplate.execute("SET LOCAL search_path TO remo");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void renamesTheInitialAccountAndKeepsItsPasswordHash() throws IOException {
        insertInitialAccount("debschke", "David Ebschke");

        runMigration();

        Map<String, Object> account = loadInitialAccount();
        assertThat(account).containsEntry("username", "account_default").containsEntry("display_name", "Account_Default");
        assertThat(account).containsEntry("password_hash", "stored-hash");
    }

    @Test
    void leavesAnInitialAccountRenamedByItsOwnerUntouched() throws IOException {
        insertInitialAccount("my-own-name", "David Ebschke");

        runMigration();

        Map<String, Object> account = loadInitialAccount();
        assertThat(account).containsEntry("username", "my-own-name").containsEntry("display_name", "David Ebschke");
    }

    private void insertInitialAccount(String username, String displayName) {
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, display_name, demo_account, created_at, version)"
                        + " VALUES (?, ?, 'stored-hash', ?, FALSE, NOW(), 0)",
                INITIAL_ACCOUNT_ID, username, displayName);
    }

    private void runMigration() throws IOException {
        String migration = new ClassPathResource("db/migration/V4__rename_initial_account.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        jdbcTemplate.execute(migration);
    }

    private Map<String, Object> loadInitialAccount() {
        return jdbcTemplate.queryForMap("SELECT username, display_name, password_hash FROM users WHERE id = ?", INITIAL_ACCOUNT_ID);
    }
}
