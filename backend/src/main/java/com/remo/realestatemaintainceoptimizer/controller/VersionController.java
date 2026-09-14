package com.remo.realestatemaintainceoptimizer.controller;

import com.remo.realestatemaintainceoptimizer.dto.VersionResponse;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the currently deployed application version to API consumers such as the frontend footer.
 */
@RestController
public class VersionController {

    private final BuildProperties buildProperties;

    public VersionController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    /**
     * Returns the application version currently packaged from {@code pom.xml}.
     */
    @GetMapping("/api/version")
    public VersionResponse getVersion() {
        return new VersionResponse(buildProperties.getVersion());
    }
}
