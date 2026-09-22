package com.remo.realestatemaintainceoptimizer.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remo.realestatemaintainceoptimizer.config.PropertyStorageProperties;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.repository.PropertyFileRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies the property create and read REST API against an isolated, temporary storage directory.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PropertyControllerTest {

    @TempDir
    static Path storageDirectory;

    @DynamicPropertySource
    static void overrideStorageDirectory(DynamicPropertyRegistry registry) {
        registry.add("remo.storage.properties.directory", storageDirectory::toString);
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void seedProperty() throws IOException {
        try (var files = Files.list(storageDirectory)) {
            for (Path file : files.toList()) {
                Files.delete(file);
            }
        }

        PropertyFileRepository repository =
                new PropertyFileRepository(new PropertyStorageProperties(storageDirectory.toString()), new ObjectMapper());
        repository.save(new Property(
                "1", "Wohnanlage Sonnenhof", "Aachener Str. 512, 50933 Köln-Braunsenfeld", "pi-building", 50.94, 6.88));
    }

    @Test
    void listsEverySeededProperty() throws Exception {
        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", equalTo("Wohnanlage Sonnenhof")));
    }

    @Test
    void getsAPropertyById() throws Exception {
        mockMvc.perform(get("/api/properties/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address", equalTo("Aachener Str. 512, 50933 Köln-Braunsenfeld")))
                .andExpect(jsonPath("$.latitude", equalTo(50.94)))
                .andExpect(jsonPath("$.longitude", equalTo(6.88)));
    }

    @Test
    void gettingAnUnknownPropertyReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/properties/{id}", "unknown-id")).andExpect(status().isNotFound());
    }

    @Test
    void createsAProperty() throws Exception {
        String requestBody = """
                {
                  "name": "Wohnanlage Nordpark",
                  "address": "Nordparkstr. 3, 50733 Köln",
                  "latitude": 50.97,
                  "longitude": 6.95
                }
                """;

        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", equalTo("Wohnanlage Nordpark")))
                .andExpect(jsonPath("$.icon", equalTo("pi-building")));
    }

    @Test
    void creatingAPropertyWithoutANameReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                  "name": "",
                  "address": "Nordparkstr. 3, 50733 Köln"
                }
                """;

        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
