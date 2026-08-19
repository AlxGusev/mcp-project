package tech.gusev.mcpulsor.server;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Set;

public class AllergyChecker {

    private static final Set<String> KNOWN_ALLERGENS = Set.of(
            "peanuts", "pollen", "shellfish", "gluten", "lactose"
    );

    @Tool(
            name = "checkAllergyTool",
            description = "Checks whether a given substance is a known allergen"
    )
    public String detectAllergy(
            @ToolParam(description = "The substance or item to check for potential allergy risk")
            String subject){
        if (subject == null || subject.isBlank()) {
            return "No subject provided to check.";
        }
        boolean isKnownAllergen = KNOWN_ALLERGENS.stream()
                .anyMatch(a -> a.equalsIgnoreCase(subject.trim()));

        return isKnownAllergen
                ? subject + " is a known allergen. Caution is advised."
                : subject + " is not in the known allergen list.";
    }
}
