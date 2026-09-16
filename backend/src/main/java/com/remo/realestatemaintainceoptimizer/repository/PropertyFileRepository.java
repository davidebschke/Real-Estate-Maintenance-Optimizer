package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.config.PropertyStorageProperties;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Persists properties as one JSON file per property under the configured storage directory.
 */
@Repository
@EnableConfigurationProperties(PropertyStorageProperties.class)
public class PropertyFileRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyFileRepository.class);
    private static final String FILE_SUFFIX = ".json";

    private final Path directory;
    private final ObjectMapper objectMapper;
    private final ReentrantLock writeLock = new ReentrantLock();

    public PropertyFileRepository(PropertyStorageProperties propertyStorageProperties, ObjectMapper objectMapper) {
        this.directory = Path.of(propertyStorageProperties.directory());
        this.objectMapper = objectMapper;
        createDirectoryIfMissing();
    }

    /**
     * Writes the given property to its own file, replacing any existing file with the same id.
     */
    public Property save(Property property) {
        writeLock.lock();
        try {
            Path target = fileFor(property.id());
            Path tempFile = Files.createTempFile(directory, property.id(), FILE_SUFFIX + ".tmp");
            objectMapper.writeValue(tempFile.toFile(), property);
            Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return property;
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to persist property " + property.id(), exception);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns the property with the given id, if a corresponding file exists and is readable.
     */
    public Optional<Property> findById(String id) {
        Path file = fileFor(id);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return readFile(file);
    }

    /**
     * Returns every property whose file could be read, skipping any file that fails to parse.
     */
    public List<Property> findAll() {
        try (var files = Files.list(directory)) {
            return files
                    .filter(path -> path.toString().endsWith(FILE_SUFFIX))
                    .map(this::readFile)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to list property files in " + directory, exception);
        }
    }

    private Optional<Property> readFile(Path file) {
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), Property.class));
        } catch (JacksonException exception) {
            LOGGER.warn("Skipping unreadable property file {}", file, exception);
            return Optional.empty();
        }
    }

    private Path fileFor(String id) {
        return directory.resolve(id + FILE_SUFFIX);
    }

    private void createDirectoryIfMissing() {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to create property storage directory " + directory, exception);
        }
    }
}
