package com.healthcanada.jira.github.storage;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcanada.jira.github.model.GitHubConfig;
import com.healthcanada.jira.github.security.TokenEncryption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Manages plugin configuration persistence using Jira's PluginSettings
 */
@Component
public class PluginConfigurationManager {

    private static final Logger log = LoggerFactory.getLogger(PluginConfigurationManager.class);
    private static final String STORAGE_KEY = "com.healthcanada.jira.github.config";

    private final PluginSettingsFactory pluginSettingsFactory;
    private final TokenEncryption tokenEncryption;
    private final ObjectMapper objectMapper;

    @Autowired
    public PluginConfigurationManager(PluginSettingsFactory pluginSettingsFactory, TokenEncryption tokenEncryption) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.tokenEncryption = tokenEncryption;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Load configuration from plugin settings
     */
    public GitHubConfig getConfiguration() {
        try {
            PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
            String configJson = (String) settings.get(STORAGE_KEY);

            if (configJson == null || configJson.isEmpty()) {
                log.debug("No configuration found, returning empty config");
                return new GitHubConfig();
            }

            GitHubConfig config = objectMapper.readValue(configJson, GitHubConfig.class);

            // Decrypt sensitive fields
            if (config.getGithubToken() != null && !config.getGithubToken().isEmpty()) {
                try {
                    String decryptedToken = tokenEncryption.decrypt(config.getGithubToken());
                    config.setGithubToken(decryptedToken);
                } catch (Exception e) {
                    log.error("Failed to decrypt GitHub token", e);
                    // Leave encrypted, will fail validation
                }
            }

            if (config.getWebhookSecret() != null && !config.getWebhookSecret().isEmpty()) {
                try {
                    String decryptedSecret = tokenEncryption.decrypt(config.getWebhookSecret());
                    config.setWebhookSecret(decryptedSecret);
                } catch (Exception e) {
                    log.error("Failed to decrypt webhook secret", e);
                }
            }

            log.debug("Loaded configuration for {} repositories", config.getRepositories().size());
            return config;

        } catch (Exception e) {
            log.error("Failed to load configuration", e);
            return new GitHubConfig();
        }
    }

    /**
     * Save configuration to plugin settings
     */
    public void saveConfiguration(GitHubConfig config) throws Exception {
        try {
            // Create a copy to avoid modifying the original
            GitHubConfig configToSave = objectMapper.readValue(
                    objectMapper.writeValueAsString(config),
                    GitHubConfig.class
            );

            // Encrypt sensitive fields before storing
            if (configToSave.getGithubToken() != null && !configToSave.getGithubToken().isEmpty()) {
                // Only encrypt if not already encrypted
                if (!tokenEncryption.isEncrypted(configToSave.getGithubToken())) {
                    String encryptedToken = tokenEncryption.encrypt(configToSave.getGithubToken());
                    configToSave.setGithubToken(encryptedToken);
                }
            }

            if (configToSave.getWebhookSecret() != null && !configToSave.getWebhookSecret().isEmpty()) {
                if (!tokenEncryption.isEncrypted(configToSave.getWebhookSecret())) {
                    String encryptedSecret = tokenEncryption.encrypt(configToSave.getWebhookSecret());
                    configToSave.setWebhookSecret(encryptedSecret);
                }
            }

            String configJson = objectMapper.writeValueAsString(configToSave);

            PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
            settings.put(STORAGE_KEY, configJson);

            log.info("Configuration saved successfully for {} repositories", config.getRepositories().size());

        } catch (Exception e) {
            log.error("Failed to save configuration", e);
            throw new Exception("Failed to save configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Delete configuration (for testing or reset)
     */
    public void deleteConfiguration() {
        try {
            PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
            settings.remove(STORAGE_KEY);
            log.info("Configuration deleted");
        } catch (Exception e) {
            log.error("Failed to delete configuration", e);
        }
    }

    /**
     * Check if configuration exists
     */
    public boolean hasConfiguration() {
        try {
            PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
            String configJson = (String) settings.get(STORAGE_KEY);
            return configJson != null && !configJson.isEmpty();
        } catch (Exception e) {
            log.error("Failed to check configuration existence", e);
            return false;
        }
    }
}
