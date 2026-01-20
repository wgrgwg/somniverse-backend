package dev.wgrgwg.somniverse.config;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.HarmBlockThreshold;
import com.google.genai.types.HarmCategory;
import com.google.genai.types.SafetySetting;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GeminiConfig {

    private final GeminiProperties geminiProperties;

    @Bean
    public Client geminiClient() {
        return Client.builder().apiKey(geminiProperties.getApiKey()).build();
    }

    @Bean
    public GenerateContentConfig geminiContentConfig() {
        ImmutableList<SafetySetting> safetySettings = ImmutableList.of(
            SafetySetting.builder()
                .category(HarmCategory.Known.HARM_CATEGORY_HATE_SPEECH)
                .threshold(HarmBlockThreshold.Known.BLOCK_ONLY_HIGH).build(),
            SafetySetting.builder()
                .category(HarmCategory.Known.HARM_CATEGORY_DANGEROUS_CONTENT)
                .threshold(HarmBlockThreshold.Known.BLOCK_LOW_AND_ABOVE).build());

        return GenerateContentConfig.builder().safetySettings(safetySettings).build();
    }
}
