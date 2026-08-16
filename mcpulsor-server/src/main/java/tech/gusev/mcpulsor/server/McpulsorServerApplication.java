package tech.gusev.mcpulsor.server;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import lombok.SneakyThrows;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

import static io.modelcontextprotocol.spec.McpSchema.*;

public class McpulsorServerApplication {

    @SneakyThrows
    public static void main(String[] args) {
        var transportProvider = HttpServletStreamableServerTransportProvider.builder().mcpEndpoint("/mcpulsor").build();

        Tool heartRateMonitorTool = Tool.builder("heartRateMonitor", new JacksonMcpJsonMapper(new JsonMapper()), createHeartRateMonitorSchema())
                .title("Human Heart Rate Monitor")
                .description("Returns the heart rate of the user as a simple string value")
                .build();

        McpServerFeatures.SyncToolSpecification heartRateToolSpec = McpServerFeatures.SyncToolSpecification.builder()
                .tool(heartRateMonitorTool)
                .callHandler((mcpSyncServerExchange, callToolRequest) -> {
                    String days = callToolRequest.arguments().get("days").toString();
                    return new CallToolResult(
                            List.of(TextContent
                                    .builder("user's heart rate for the last " + days + " days is 62bpm")
                                    .build()),
                            false,
                            null,
                            null);
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

    private static String createHeartRateMonitorSchema() {
        ObjectNode root = new ObjectMapper().createObjectNode().put("type", "object");
        root.putObject("properties")
                .putObject("days")
                .put("type", "integer")
                .put("description", "The number of past days to include in the heart rate monitoring request");
        root.putArray("required").add("days");
        return root.toString();
    }

    private static ServerCapabilities createServerCapabilities() {
        return ServerCapabilities.builder()
                .tools(true)
                .build();
    }
}
