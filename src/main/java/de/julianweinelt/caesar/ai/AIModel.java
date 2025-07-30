package de.julianweinelt.caesar.ai;

public enum AIModel {
    GEMINI_2_5_FLASH("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="),
    GEMINI_2_5_FLASH_LITE("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key="),
    GEMINI_2_5_FLASH_LIVE("https://generativelanguage.googleapis.com/v1beta/models/gemini-live-2.5-flash-preview:generateContent?key="),
    GEMINI_2_5_PRO("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent?key="),
    GEMINI_2_0_FLASH("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="),
    GEMINI_2_0_FLASH_LITE("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent?key="),
    GEMINI_2_0_FLASH_LIVE("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-live-001:generateContent?key="),
    ;

    public final String apiEndpoint;
    AIModel(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }
}
