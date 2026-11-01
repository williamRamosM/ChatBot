package models;

import com.google.gson.annotations.SerializedName;

public class ChatResponse {
    @SerializedName("text_response")
    private String textResponse;

    @SerializedName("audio_url")
    private String audioUrl;

    public String getTextResponse() { return textResponse; }
    public String getAudioUrl() { return audioUrl; }
}