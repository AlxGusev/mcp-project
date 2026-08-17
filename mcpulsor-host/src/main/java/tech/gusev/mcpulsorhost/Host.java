package tech.gusev.mcpulsorhost;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import static io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

@Service
public class Host {

    private final ChatClient chatClient;
    private String systemPrompt;
    private McpSyncClient mcpClient;

    public Host(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostConstruct
    public void init() {
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:8091").endpoint("/mcpulsor").build();
        mcpClient = McpClient
                .sync(transport)
                .loggingConsumer(loggingMessageNotification ->
                        System.out.println("\nClient says: I got the message from the server - " + loggingMessageNotification.data()))
                .build();
        mcpClient.initialize();
        ListToolsResult toolsResult = mcpClient.listTools();
        systemPrompt = SystemPromptFactory.withTools(toolsResult);
    }

    public void printAnswerToUser(String question) {
        System.out.println("The host says: The user asked this question: " + question);
        AssistantMessage assistantMessage = chatClient
                .prompt(systemPrompt)
                .user(question)
                .call()
                .chatResponse()
                .getResult()
                .getOutput();

        if (CallToolUtil.isToolRequired(assistantMessage.getText())) {
            System.out.println("\nThe host says: The model asks to do something: " + assistantMessage.getText());
            CallToolRequest callToolRequest = CallToolUtil.getRequiredTool(assistantMessage.getText());
            String toolResponse = CallToolUtil.wrapResponse(mcpClient.callTool(callToolRequest).content().getFirst().toString());
            System.out.println("\nThe host says: The server's response to the model's request : " + toolResponse);
            UserMessage toolMessage = new UserMessage(toolResponse);
            UserMessage userMessage = new UserMessage(question);
            assistantMessage = chatClient.prompt()
                    .system(systemPrompt)
                    .messages(List.of(userMessage, assistantMessage, toolMessage))
                    .call()
                    .chatResponse()
                    .getResult()
                    .getOutput();
        }

        System.out.println(assistantMessage.getText());
    }
}
