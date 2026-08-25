package software.pxel.learneasy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@EnableAsync
@Configuration
public class AsyncConfiguration {

    @Bean
    public Executor ioExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
