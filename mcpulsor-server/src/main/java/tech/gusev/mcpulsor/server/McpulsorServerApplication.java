package tech.gusev.mcpulsor.server;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.SneakyThrows;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;

public class McpulsorServerApplication {

    @SneakyThrows
    public static void main(String[] args) {
        var transportProvider = HttpServletStreamableServerTransportProvider.builder().mcpEndpoint("/mcpulsor").build();

        McpServer.sync(transportProvider).capabilities(createServerCapabilities()).build();

        Server server = new Server(8091);
        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(new ServletHolder(transportProvider), "/*");
        server.setHandler(contextHandler);
        server.start();
        System.out.println("Jetty started, joining...");
        server.join();
    }

    private static McpSchema.ServerCapabilities createServerCapabilities() {
        return McpSchema.ServerCapabilities.builder().build();
    }
}
