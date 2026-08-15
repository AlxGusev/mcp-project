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

import java.util.List;

import static io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import static io.modelcontextprotocol.spec.McpSchema.Tool;

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
                .callHandler(
                        (mcpSyncServerExchange, callToolRequest) ->
                                new McpSchema.CallToolResult(
                                        List.of(McpSchema.TextContent.builder("user's heart rate is 50").build()),
                                        false, null, null))
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
        return new ObjectMapper().createObjectNode().put("type", "object").toString();
    }

    private static ServerCapabilities createServerCapabilities() {
        return ServerCapabilities.builder()
                .tools(true)
                .build();
    }
}
