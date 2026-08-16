package tech.gusev.mcpulsorhost;

import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CallToolUtil {

    private final static ObjectMapper mapper = new ObjectMapper();


    private static final Pattern TOOL_CALL_PATTERN =
            Pattern.compile("<tool_call>\\s*(\\{.*?})\\s*</tool_call>",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static boolean isToolRequired(String modelAnswer) {
        return TOOL_CALL_PATTERN.matcher(modelAnswer).find();
    }

    public static McpSchema.CallToolRequest getRequiredTool(String modelAnswer) {
        Matcher matcher = TOOL_CALL_PATTERN.matcher(modelAnswer);
        matcher.find();
        String toolCallRequestJson = matcher.group(1).trim();
        JsonNode tool = mapper.readTree(toolCallRequestJson);
        String toolName = tool.path("name").asString();
        JsonNode parameters = tool.path("parameters");
        Map<String, Object> args = mapper.convertValue(parameters, new TypeReference<>() {});
        return McpSchema.CallToolRequest.builder(toolName).arguments(args).build();
    }

    public static String wrapResponse(String toolResult) {
        return String.format("<tool_response>%n%s%n</tool_response>", toolResult);
    }
}
