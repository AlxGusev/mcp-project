package tech.gusev.mcpulsor.server;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.SneakyThrows;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

import static io.modelcontextprotocol.spec.McpSchema.*;

public class McpulsorServerApplication {

    @SneakyThrows
    public static void main(String[] args) {
        var transportProvider = HttpServletStreamableServerTransportProvider.builder().mcpEndpoint("/mcpulsor").build();

        Tool heartRateMonitorTool = Tool
                .builder("heartRateMonitor", new JacksonMcpJsonMapper(new JsonMapper()), createHeartRateMonitorInputSchema())
                .title("Human Heart Rate Monitor")
                .description("Returns the heart rate of the user as a simple string value")
                .outputSchema(new JacksonMcpJsonMapper(new JsonMapper()), createHeartRateMonitorOutputSchema())
                .build();

        McpServerFeatures.SyncToolSpecification heartRateToolSpec = McpServerFeatures.SyncToolSpecification.builder()
                .tool(heartRateMonitorTool)
                .callHandler((mcpSyncServerExchange, callToolRequest) -> {
                    String serverMessage = "Received call tool request: " + callToolRequest.toString();
                    System.out.println("Server says: " + serverMessage);
                    mcpSyncServerExchange.loggingNotification(McpSchema.LoggingMessageNotification.builder(LoggingLevel.INFO, serverMessage).build());
                    int days = (int) callToolRequest.arguments().get("days");
                    return getCallToolResult(days);
                })
                .build();

        McpServer.sync(transportProvider)
                .serverInfo("mcpulsor mcp server", "1.0.RELEASE")
                .capabilities(createServerCapabilities())
                .tools(heartRateToolSpec)
                .build();

        Server server = new Server(8091);
        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(new ServletHolder(transportProvider), "/*");
        server.setHandler(contextHandler);
        server.start();
        System.out.println("Jetty started, joining...");
        server.join();
    }

    private static CallToolResult getCallToolResult(int days) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("heart_rate", "Your heart rate is 62bpm / " + days + " days");
        properties.put("state", "You are in trouble");
        properties.put("sleepDeprivation", true);
        CallToolResult toolResult = CallToolResult.builder().structuredContent(properties).build();
        System.out.println("Server says: here is what i will return to the user: " + toolResult.toString());
        return toolResult;
    }

    private static String createHeartRateMonitorInputSchema() {
        ObjectNode root = new ObjectMapper().createObjectNode().put("type", "object");
        root.putObject("properties")
                .putObject("days")
                .put("type", "integer")
                .put("description", "The number of past days to include in the heart rate monitoring request");
        root.putArray("required").add("days");
        return root.toString();
    }

    private static String createHeartRateMonitorOutputSchema() {
        ObjectNode root = new ObjectMapper().createObjectNode().put("type", "object");
        ObjectNode properties = root.putObject("properties");
        properties.putObject("heart_rate")
                .put("type", "string")
                .put("description", "Average heart rate of the user");
        properties.putObject("state")
                .put("type", "string")
                .put("description", "What state of the heart rate of the user");
        properties.putObject("sleepDeprivation")
                .put("type", "boolean")
                .put("description", "Whether the user is sleep deprived YES/NO");
        return root.toString();
    }

    private static ServerCapabilities createServerCapabilities() {
        return ServerCapabilities.builder()
                .tools(true)
                .build();
    }
}
