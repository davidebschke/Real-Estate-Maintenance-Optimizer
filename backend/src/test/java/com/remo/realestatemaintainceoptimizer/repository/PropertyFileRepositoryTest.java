package com.remo.realestatemaintainceoptimizer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.config.PropertyStorageProperties;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies that properties are correctly saved as and read from individual JSON files.
 */
class PropertyFileRepositoryTest {

    @TempDir
    private Path storageDirectory;

    private PropertyFileRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PropertyFileRepository(new PropertyStorageProperties(storageDirectory.toString()), new ObjectMapper());
    }

    @Test
    void savesAndReadsBackAProperty() {
        Property property = createProperty("1", "Wohnanlage Sonnenhof");

        repository.save(property);

        assertThat(repository.findById("1")).contains(property);
    }

    @Test
    void returnsEmptyForUnknownId() {
        assertThat(repository.findById("unknown")).isEmpty();
    }

    @Test
    void findAllReturnsEverySavedProperty() {
        repository.save(createProperty("1", "Wohnanlage Sonnenhof"));
        repository.save(createProperty("2", "Wohnpark Lindenthal"));

        assertThat(repository.findAll()).extracting(Property::id).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    void findAllSkipsAFileThatFailsToParse() throws IOException {
        repository.save(createProperty("1", "Wohnanlage Sonnenhof"));
        Files.writeString(storageDirectory.resolve("corrupt.json"), "{ not valid json");

        assertThat(repository.findAll()).extracting(Property::id).containsExactly("1");
    }

    private Property createProperty(String id, String name) {
        return new Property(id, name, "Aachener Str. 512, 50933 Köln-Braunsenfeld", "pi-building");
    }
}
