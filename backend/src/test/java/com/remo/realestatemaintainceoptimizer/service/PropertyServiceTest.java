package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.remo.realestatemaintainceoptimizer.config.PropertyStorageProperties;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.PropertyFileRepository;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies property listing (sorted by name) and lookup by id, including the not-found case.
 */
class PropertyServiceTest {

    @TempDir
    private Path storageDirectory;

    private PropertyFileRepository repository;
    private PropertyService service;

    @BeforeEach
    void setUp() {
        repository = new PropertyFileRepository(new PropertyStorageProperties(storageDirectory.toString()), new ObjectMapper());
        service = new PropertyService(repository);
    }

    @Test
    void listsEveryPropertySortedByName() {
        repository.save(new Property("1", "Wohnanlage Rheinblick", "Rheinuferstr. 8", "pi-building"));
        repository.save(new Property("2", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));

        assertThat(service.listAll()).extracting("name")
                .containsExactly("Wohnanlage Rheinblick", "Wohnanlage Sonnenhof");
    }

    @Test
    void returnsThePropertyWithTheGivenId() {
        repository.save(new Property("1", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));

        assertThat(service.getById("1").name()).isEqualTo("Wohnanlage Sonnenhof");
    }

    @Test
    void throwsWhenNoPropertyExistsForTheGivenId() {
        assertThatThrownBy(() -> service.getById("unknown")).isInstanceOf(PropertyNotFoundException.class);
    }
}
