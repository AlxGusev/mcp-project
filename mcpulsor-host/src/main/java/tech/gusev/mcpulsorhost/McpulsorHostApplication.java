package tech.gusev.mcpulsorhost;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpulsorHostApplication {

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(chatModel).defaultTools(toolCallbackProvider)
                .defaultOptions(OllamaChatOptions.builder()
                        .temperature(0.1)
                        .topK(10)
                        .topP(0.95)
                        .repeatPenalty(1.0))
                .build();
    }

    public static void main(String[] args) {
        String firstQuestion = "What is my heart rate for the last 7 days?";
        String secondQuestion = "How are you?";
        String thirdQuestion = "What will my heart rate for the next 7 days be plus 100?";
        Host host = SpringApplication.run(McpulsorHostApplication.class, args).getBean(Host.class);
        host.printAnswerToUser(firstQuestion);
        host.printAnswerToUser(secondQuestion);
        host.printAnswerToUser(thirdQuestion);
    }

}