package dev.wgrgwg.somniverse.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "gemini")
@Validated
@Getter
@Setter
public class GeminiProperties {

    @NotBlank
    private String apiKey;

    @NotBlank
    private String modelName;
}
