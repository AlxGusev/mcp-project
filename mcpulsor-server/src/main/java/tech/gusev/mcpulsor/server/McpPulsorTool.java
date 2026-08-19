package tech.gusev.mcpulsor.server;

import org.jspecify.annotations.NonNull;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.modelcontextprotocol.spec.McpSchema.*;
import static io.modelcontextprotocol.spec.McpSchema.Role.USER;
import static tech.gusev.mcpulsor.server.service.HeartRateCalculator.getHeartRate;
import static tech.gusev.mcpulsor.server.service.MedicalProfileProvider.getMedicalProfile;

@Service
public class McpPulsorTool {

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

    @McpTool(
            name = "diagnoser",
            title = "Diagnoser",
            description = "Returns the diagnosis of the user by its name. Always return the name of the illness, or the phrase the patient is healthy.")
    public static String callDiagnoser(
            McpSyncRequestContext requestContext,
            @McpToolParam(description = "The name of the patient, by which the diagnosis will be performed") String patientName) {

        int heartRate = getHeartRate(patientName);
        String medicalProfile = getMedicalProfile(patientName);
        String samplingPrompt = "Here is our patient, and this is his medical record " + medicalProfile + ", and this is his current heart rate: " + heartRate;

        CreateMessageRequest samplingMessageRequest = CreateMessageRequest
                .builder(List.of(new SamplingMessage(USER, TextContent.builder(samplingPrompt).build())), 50)
                .systemPrompt(SAMPLING_SYSTEM_PROMPT)
                .temperature(0.1)
                .build();

        CreateMessageResult samplingResult = requestContext.sample(samplingMessageRequest);
        return samplingResult.content().toString();
    }

    @McpTool(
            name = "heartRateMonitor",
            title = "Heart Rate Monitor",
            description = "Returns the heart rate of the user as a simple string value")
    public static @NonNull Map<String, Object> callHeartRateSensor(int numberOfDays) {
        return getCallToolResult(numberOfDays);
    }

    private static Map<String, Object> getCallToolResult(int days) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("heart_rate", "Your heart rate is 62bpm / " + days + " days");
        properties.put("state", "You are in trouble");
        properties.put("sleepDeprivation", true);
        return properties;
    }
}
