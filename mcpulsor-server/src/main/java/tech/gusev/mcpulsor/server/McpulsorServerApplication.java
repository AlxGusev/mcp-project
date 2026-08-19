package tech.gusev.mcpulsor.server;

import lombok.SneakyThrows;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class McpulsorServerApplication {

    @Bean
    public List<ToolCallback> toolCallback() {
        return List.of(ToolCallbacks.from(new AllergyChecker()));
    }

    @SneakyThrows
    public static void main(String[] args) {
        System.out.println("Server Application Started");
        SpringApplication.run(McpulsorServerApplication.class, args);
    }
}
