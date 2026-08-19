package tech.gusev.mcpulsorhost;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.annotation.McpLogging;
import org.springframework.ai.mcp.annotation.McpSampling;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import static io.modelcontextprotocol.spec.McpSchema.*;
import static io.modelcontextprotocol.spec.McpSchema.Role.ASSISTANT;

@Service
public class Host {

    private final ChatModel chatModel;
    private final ChatClient chatClient;

    public Host(ChatModel chatModel, ChatClient chatClient) {
        this.chatModel = chatModel;
        this.chatClient = chatClient;
    }

    @McpLogging(clients = "mcpulsor-server")
    public void logClient(LoggingMessageNotification loggingMessageNotification) {
        System.out.println("Client says: I got the message from the server - " + loggingMessageNotification.data());
    }

    @McpSampling(clients = "mcpulsor-server")
    public CreateMessageResult createSampling(CreateMessageRequest createMessageRequest) {
        ChatClient samplingChatClient = ChatClient.builder(chatModel)
                .defaultOptions(OllamaChatOptions.builder()
                        .numPredict(createMessageRequest.maxTokens())
                        .temperature(createMessageRequest.temperature()))
                .build();

        String samplingAnswer = samplingChatClient
                .prompt()
                .system(createMessageRequest.systemPrompt())
                .user(createMessageRequest.messages().getFirst().content().toString())
                .call()
                .content();
        return CreateMessageResult.builder(ASSISTANT, TextContent.builder(samplingAnswer).build(), "ABC").build();
    }

    public void printAnswerToUser(String question) {
        System.out.println("The host says: The user asked this question: " + question);
        AssistantMessage assistantMessage = chatClient
                .prompt()
                .user(question)
                .call()
                .chatResponse()
                .getResult()
                .getOutput();
        System.out.println(assistantMessage.getText());
    }
}
