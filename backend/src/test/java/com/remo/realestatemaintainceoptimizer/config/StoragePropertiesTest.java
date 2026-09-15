package com.remo.realestatemaintainceoptimizer.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that {@code remo.storage.*} properties bind correctly from application.yml.
 */
@SpringBootTest
class StoragePropertiesTest {

    @Autowired
    private StorageProperties storageProperties;

    @Test
    void bindsTheConfiguredStorageDirectory() {
        assertThat(storageProperties.directory()).isEqualTo("ExampleTerms");
    }
}
