package software.pxel.learneasy.feign.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.pxel.learneasy.feign.common.CustomFeignErrorDecoder;

@Configuration
public class AIFeignConfig {

    @Bean
    public ErrorDecoder aiErrorDecoder(ObjectMapper objectMapper) {
        return new CustomFeignErrorDecoder(objectMapper);
    }
}
