package software.pxel.learneasy.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "software.pxel.learneasy.feign")
public class ClientConfiguration {
}
