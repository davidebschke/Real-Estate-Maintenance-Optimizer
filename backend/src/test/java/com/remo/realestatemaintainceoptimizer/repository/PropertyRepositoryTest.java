package com.remo.realestatemaintainceoptimizer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * Verifies that properties, including optional coordinates, round-trip through the Flyway-managed PostgreSQL schema.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PropertyRepositoryTest {

    @Autowired
    private PropertyRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndReloadsAPropertyWithCoordinates() {
        repository.save(new Property("1", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building", 50.94, 6.88));
        flushAndClear();

        Property reloaded = repository.findById("1").orElseThrow();

        assertThat(reloaded.name()).isEqualTo("Wohnanlage Sonnenhof");
        assertThat(reloaded.address()).isEqualTo("Aachener Str. 512");
        assertThat(reloaded.icon()).isEqualTo("pi-building");
        assertThat(reloaded.latitude()).isEqualTo(50.94);
        assertThat(reloaded.longitude()).isEqualTo(6.88);
    }

    @Test
    void persistsAPropertyWithoutCoordinates() {
        repository.save(new Property("1", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
        flushAndClear();

        Property reloaded = repository.findById("1").orElseThrow();

        assertThat(reloaded.latitude()).isNull();
        assertThat(reloaded.longitude()).isNull();
    }

    @Test
    void persistsUpdatedDetails() {
        Property property = repository.save(new Property("1", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
        flushAndClear();

        repository.findById(property.id()).orElseThrow().updateDetails("Wohnanlage Nordpark", "Nordparkstr. 3", 50.97, 6.95);
        flushAndClear();

        Property reloaded = repository.findById("1").orElseThrow();
        assertThat(reloaded.name()).isEqualTo("Wohnanlage Nordpark");
        assertThat(reloaded.address()).isEqualTo("Nordparkstr. 3");
        assertThat(reloaded.latitude()).isEqualTo(50.97);
        assertThat(reloaded.icon()).isEqualTo("pi-building");
    }

    @Test
    void listsEveryPropertySortedByName() {
        repository.save(new Property("1", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
        repository.save(new Property("2", "Wohnanlage Rheinblick", "Rheinuferstr. 8", "pi-building"));
        flushAndClear();

        assertThat(repository.findAllByOrderByNameAsc()).extracting(Property::name)
                .containsExactly("Wohnanlage Rheinblick", "Wohnanlage Sonnenhof");
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
