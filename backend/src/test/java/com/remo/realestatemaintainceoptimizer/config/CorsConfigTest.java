package com.remo.realestatemaintainceoptimizer.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that the configured frontend origin is allowed to preflight every HTTP method the REST API actually uses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CorsConfigTest {

    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE"})
    void allowsThePreflightForEveryMethodTheApiUses(String method) throws Exception {
        var result = mockMvc.perform(options("/api/properties/1")
                        .header("Origin", FRONTEND_ORIGIN)
                        .header("Access-Control-Request-Method", method))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader("Access-Control-Allow-Methods")).contains(method);
    }
}
