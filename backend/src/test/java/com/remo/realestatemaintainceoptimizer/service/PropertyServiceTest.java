package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.remo.realestatemaintainceoptimizer.config.PropertyStorageProperties;
import com.remo.realestatemaintainceoptimizer.dto.CreatePropertyRequest;
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
    void includesStoredCoordinatesInTheResponse() {
        repository.save(new Property("1", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building", 50.94, 6.88));

        var response = service.getById("1");

        assertThat(response.latitude()).isEqualTo(50.94);
        assertThat(response.longitude()).isEqualTo(6.88);
    }

    @Test
    void throwsWhenNoPropertyExistsForTheGivenId() {
        assertThatThrownBy(() -> service.getById("unknown")).isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void createsAPropertyWithAGeneratedIdAndTheDefaultIcon() {
        var response = service.create(new CreatePropertyRequest(
                "Wohnanlage Nordpark", "Nordparkstr. 3, 50733 Köln", 50.97, 6.95));

        assertThat(response.id()).isNotBlank();
        assertThat(response.name()).isEqualTo("Wohnanlage Nordpark");
        assertThat(response.address()).isEqualTo("Nordparkstr. 3, 50733 Köln");
        assertThat(response.icon()).isEqualTo("pi-building");
        assertThat(response.latitude()).isEqualTo(50.97);
        assertThat(response.longitude()).isEqualTo(6.95);
        assertThat(service.getById(response.id()).name()).isEqualTo("Wohnanlage Nordpark");
    }

    @Test
    void createsAPropertyWithoutCoordinatesWhenNoneAreGiven() {
        var response = service.create(new CreatePropertyRequest("Wohnanlage Nordpark", "Nordparkstr. 3", null, null));

        assertThat(response.latitude()).isNull();
        assertThat(response.longitude()).isNull();
    }
}
