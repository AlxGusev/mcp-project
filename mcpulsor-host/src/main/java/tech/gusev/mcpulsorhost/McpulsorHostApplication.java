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
        String firstQuestion = "What is my heart rate today? Will I survive?";
        String secondQuestion = "How am I feeling?";
        String thirdQuestion = "Given my heart rate, should I go for a 100-km bike ride?";
        Host host = SpringApplication.run(McpulsorHostApplication.class, args).getBean(Host.class);
        host.printAnswerToUser(firstQuestion);
        host.printAnswerToUser("How are you?");
        host.printAnswerToUser(secondQuestion);
        host.printAnswerToUser(thirdQuestion);
    }

}