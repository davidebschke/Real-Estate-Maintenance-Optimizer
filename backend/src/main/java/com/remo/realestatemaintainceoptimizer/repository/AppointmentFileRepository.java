package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.config.StorageProperties;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
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
 * Persists appointments as one JSON file per appointment under the configured storage directory.
 */
@Repository
@EnableConfigurationProperties(StorageProperties.class)
public class AppointmentFileRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentFileRepository.class);
    private static final String FILE_SUFFIX = ".json";

    private final Path directory;
    private final ObjectMapper objectMapper;
    private final ReentrantLock writeLock = new ReentrantLock();

    public AppointmentFileRepository(StorageProperties storageProperties, ObjectMapper objectMapper) {
        this.directory = Path.of(storageProperties.directory());
        this.objectMapper = objectMapper;
        createDirectoryIfMissing();
    }

    /**
     * Writes the given appointment to its own file, replacing any existing file with the same id.
     */
    public Appointment save(Appointment appointment) {
        writeLock.lock();
        try {
            Path target = fileFor(appointment.id());
            Path tempFile = Files.createTempFile(directory, appointment.id(), FILE_SUFFIX + ".tmp");
            objectMapper.writeValue(tempFile.toFile(), appointment);
            Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return appointment;
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to persist appointment " + appointment.id(), exception);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns the appointment with the given id, if a corresponding file exists and is readable.
     */
    public Optional<Appointment> findById(String id) {
        Path file = fileFor(id);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return readFile(file);
    }

    /**
     * Returns every appointment whose file could be read, skipping any file that fails to parse.
     */
    public List<Appointment> findAll() {
        try (var files = Files.list(directory)) {
            return files
                    .filter(path -> path.toString().endsWith(FILE_SUFFIX))
                    .map(this::readFile)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to list appointment files in " + directory, exception);
        }
    }

    /**
     * Returns every appointment sharing the given series id.
     */
    public List<Appointment> findBySeriesId(String seriesId) {
        return findAll().stream()
                .filter(appointment -> seriesId.equals(appointment.seriesId()))
                .toList();
    }

    /**
     * Deletes the appointment file with the given id, if it exists.
     */
    public void deleteById(String id) {
        writeLock.lock();
        try {
            Files.deleteIfExists(fileFor(id));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to delete appointment " + id, exception);
        } finally {
            writeLock.unlock();
        }
    }

    private Optional<Appointment> readFile(Path file) {
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), Appointment.class));
        } catch (JacksonException exception) {
            LOGGER.warn("Skipping unreadable appointment file {}", file, exception);
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
            throw new UncheckedIOException("Failed to create appointment storage directory " + directory, exception);
        }
    }
}
