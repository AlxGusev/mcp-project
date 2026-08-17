package tech.gusev.mcpulsor.server;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;

import java.util.Map;

import static io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

public class ClientMain {

    public static void main(String[] args) {
        var clientTransport = HttpClientStreamableHttpTransport
                .builder("http://localhost:8091")
                .endpoint("/mcpulsor")
                .build();


        try (McpSyncClient client = McpClient.sync(clientTransport).build()) {
            client.initialize();
            client.listTools().tools().forEach(System.out::println);
            client.callTool(CallToolRequest
                            .builder("Diagnoser")
                            .arguments(Map.of("name", "Tom"))
                            .build())
                    .content()
                    .forEach(System.out::println);
        }
    }
}
