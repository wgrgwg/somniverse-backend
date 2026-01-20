package dev.wgrgwg.somniverse.dream.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import dev.wgrgwg.somniverse.config.GeminiProperties;
import dev.wgrgwg.somniverse.dream.domain.Dream;
import dev.wgrgwg.somniverse.dream.exception.DreamErrorCode;
import dev.wgrgwg.somniverse.dream.repository.DreamRepository;
import dev.wgrgwg.somniverse.global.exception.CustomException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class DreamAnalysisService {

    private final Client geminiClient;
    private final GeminiProperties geminiProperties;
    private final DreamRepository dreamRepository;
    private final ResourceLoader resourceLoader;
    private final GenerateContentConfig generateContentConfig;

    private String promptTemplate;

    @PostConstruct
    public void loadPromptTemplate() {
        try {
            Resource resource = resourceLoader.getResource("classpath:prompts/dream-analysis.txt");
            this.promptTemplate = StreamUtils.copyToString(resource.getInputStream(),
                StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("프롬프트 파일 로드 실패");
        }
    }

    @Async
    @Transactional
    public void analyzeAndSaveEmotion(Long dreamId, String title, String content) {
        log.info("Dream ID [{}] 비동기 감정 분석 시작", dreamId);

        try {
            String prompt = String.format(promptTemplate, title, content);

            GenerateContentResponse response = geminiClient.models.generateContent(
                geminiProperties.getModelName(), prompt, generateContentConfig);

            String emotion = response.text().trim();

            log.info("Dream ID [{}] 분석 완료: {}", dreamId, emotion);

            Dream dream = dreamRepository.findById(dreamId)
                .orElseThrow(() -> new CustomException(DreamErrorCode.DREAM_NOT_FOUND));

            dream.updateAnalyzedEmotion(emotion);
        } catch (Exception e) {
            dreamRepository.findById(dreamId).ifPresent(Dream::failAnalysis);
        }
    }
}
