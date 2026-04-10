package com.connto.backend.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "connto.media")
public class MediaProperties {

    /** Directory for voice intro files (relative to working directory unless absolute). */
    private String voiceUploadDir = "uploads/voice";

    public String getVoiceUploadDir() {
        return voiceUploadDir;
    }

    public void setVoiceUploadDir(String voiceUploadDir) {
        this.voiceUploadDir = voiceUploadDir;
    }

    public Path voiceUploadDirPath() {
        return Paths.get(voiceUploadDir).toAbsolutePath().normalize();
    }
}
