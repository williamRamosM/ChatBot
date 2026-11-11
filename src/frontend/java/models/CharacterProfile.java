package models;

import com.google.gson.annotations.SerializedName;

public record CharacterProfile(
        @SerializedName("nome") String name,
        @SerializedName("função") String role,
        @SerializedName("personalidade") String personality,
        @SerializedName("tom de voz") String tone,
        @SerializedName("voice_id") String voiceId,
        @SerializedName("restrições") String restrictions,
        @SerializedName("img personagem") String imagePath,
        @SerializedName("img falando") String speakingImagePath
) {
}
