package tech.gusev.mcpulsor.server.service;

import java.util.Map;

public class MedicalProfileProvider {

    private static final Map<String, String> PROFILES = Map.of(
            "Tom", """
                    If the heart rate is above 55, it's golden fever; if it's 55 or less, it's any other disease, but it must include the word golden. For example, golden staphylococcus come up with any other one.""",

            "Pete", """
                    When his heart rate is over 70, Kolya gets seasick; if it's lower than or equal to 70, he has a mild hangover.
                    """,

            "Jade", """
                    If your heart rate is less than 20, it's a bout of philanthropy; if it's higher, it's ego-syndrome."
                    """
    );

    public static String getMedicalProfile(String name) {
        return PROFILES.get(name);
    }
}
