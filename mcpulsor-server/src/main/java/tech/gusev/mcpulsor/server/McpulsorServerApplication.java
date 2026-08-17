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
import tech.gusev.mcpulsor.server.service.HeartRateCalculator;
import tech.gusev.mcpulsor.server.service.MedicalProfileProvider;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.modelcontextprotocol.spec.McpSchema.*;

public class McpulsorServerApplication {

    private static final String SAMPLING_SYSTEM_PROMPT = """
            You make a diagnosis in a single word.
            At the start, you are always given the patient’s medical record and their current pulse.
            Your task is to provide exactly one of the following:
            the name of an existing illness (it can be 1–3 words, including rare or funny-sounding ones),
            or


            Answer: -say that the patient is healthy.
            Rules:
             — Analyze the patient’s chart and pulse and select the appropriate illness.
             — Respond only with the name of the illness or the phrase “the patient is healthy.”
             — No explanations, no surrounding text.
            """;

    @SneakyThrows
    public static void main(String[] args) {
        var transportProvider = HttpServletStreamableServerTransportProvider.builder().mcpEndpoint("/mcpulsor").build();

        Tool diagnoserTool = Tool
                .builder("Diagnoser", new JacksonMcpJsonMapper(new JsonMapper()), createDiagnoserInputSchema())
                .build();

        McpServerFeatures.SyncToolSpecification diagnoserToolSpec = McpServerFeatures.SyncToolSpecification.builder()
                .tool(diagnoserTool)
                .callHandler((mcpSyncServerExchange, callToolRequest) -> {
                    System.out.println("Server says: Asked the Client if he can do sampling. The answer is: " + mcpSyncServerExchange.getClientCapabilities().sampling());
                    String name = callToolRequest.arguments().get("name").toString();
                    int heartRate = HeartRateCalculator.getHeartRate(name);
                    String medicalProfile = MedicalProfileProvider.getMedicalProfile(name);
                    String samplingPrompt = "Here is our patient, and this is his medical record " + medicalProfile + ", and this is his current heart rate: " + heartRate;

                    CreateMessageRequest samplingMessageRequest = CreateMessageRequest
                            .builder(List.of(new SamplingMessage(Role.USER, TextContent.builder(samplingPrompt).build())), 50)
                            .systemPrompt(SAMPLING_SYSTEM_PROMPT)
                            .temperature(0.1)
                            .build();

                    CreateMessageResult samplingResult = mcpSyncServerExchange.createMessage(samplingMessageRequest);
                    LoggingMessageNotification
                            .builder(LoggingLevel.INFO,
                                    "I am the Server, I got the request and decided to ask with use of sampling: " + samplingPrompt +
                                            "\n and this is what i got back: " + samplingResult.content())
                            .build();
                    return CallToolResult.builder().addContent(samplingResult.content()).build();
                }).build();

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
                .tools(heartRateToolSpec, diagnoserToolSpec)
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

    private static String createDiagnoserInputSchema() {
        ObjectNode root = new ObjectMapper().createObjectNode().put("type", "object");
        root.putObject("properties")
                .putObject("name")
                .put("type", "string")
                .put("description", "The name of the patient, by which the diagnosis will be performed");
        root.putArray("required").add("name");
        return root.toString();
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
