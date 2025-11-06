package com.healthcanada.jira.github.api;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.ApplicationProperties;
import com.healthcanada.jira.github.model.GitHubConfig;
import com.healthcanada.jira.github.model.GitHubException;
import com.healthcanada.jira.github.model.RepositoryMapping;
import com.healthcanada.jira.github.security.WebhookValidator;
import com.healthcanada.jira.github.service.GitHubService;
import com.healthcanada.jira.github.storage.PluginConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for plugin configuration
 */
@Path("/github-integration/1.0/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigurationResource {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationResource.class);

    private final PluginConfigurationManager configManager;
    private final GitHubService githubService;
    private final JiraAuthenticationContext authenticationContext;
    private final GlobalPermissionManager globalPermissionManager;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public ConfigurationResource(PluginConfigurationManager configManager,
                                  GitHubService githubService,
                                  JiraAuthenticationContext authenticationContext,
                                  GlobalPermissionManager globalPermissionManager,
                                  ApplicationProperties applicationProperties) {
        this.configManager = configManager;
        this.githubService = githubService;
        this.authenticationContext = authenticationContext;
        this.globalPermissionManager = globalPermissionManager;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Get current configuration
     * GET /rest/github-integration/1.0/config
     */
    @GET
    public Response getConfiguration() {
        try {
            // Check admin permissions
            if (!isAdmin()) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(errorResponse("Administrator access required"))
                        .build();
            }

            GitHubConfig config = configManager.getConfiguration();

            // For security, don't return the actual token in GET request
            // Return a masked version
            if (config.getGithubToken() != null && !config.getGithubToken().isEmpty()) {
                config.setGithubToken("********");
            }
            if (config.getWebhookSecret() != null && !config.getWebhookSecret().isEmpty()) {
                config.setWebhookSecret("********");
            }

            return Response.ok(config).build();

        } catch (Exception e) {
            log.error("Error getting configuration", e);
            return Response.serverError()
                    .entity(errorResponse("Failed to get configuration: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Update configuration
     * PUT /rest/github-integration/1.0/config
     */
    @PUT
    public Response updateConfiguration(GitHubConfig config) {
        try {
            // Check admin permissions
            if (!isAdmin()) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(errorResponse("Administrator access required"))
                        .build();
            }

            // Validate configuration
            List<String> errors = validateConfiguration(config);
            if (!errors.isEmpty()) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("errors", errors);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorMap)
                        .build();
            }

            // If token is masked (********), keep the existing token
            if (config.getGithubToken() != null && config.getGithubToken().equals("********")) {
                GitHubConfig existingConfig = configManager.getConfiguration();
                config.setGithubToken(existingConfig.getGithubToken());
            }

            // Same for webhook secret
            if (config.getWebhookSecret() != null && config.getWebhookSecret().equals("********")) {
                GitHubConfig existingConfig = configManager.getConfiguration();
                config.setWebhookSecret(existingConfig.getWebhookSecret());
            }

            // Set webhook URL
            String baseUrl = applicationProperties.getBaseUrl();
            config.setWebhookUrl(baseUrl + "/plugins/servlet/github-webhook");

            // Save configuration
            configManager.saveConfiguration(config);

            log.info("Configuration updated by {}", authenticationContext.getLoggedInUser().getUsername());

            // Return masked version
            if (config.getGithubToken() != null && !config.getGithubToken().isEmpty()) {
                config.setGithubToken("********");
            }
            if (config.getWebhookSecret() != null && !config.getWebhookSecret().isEmpty()) {
                config.setWebhookSecret("********");
            }

            return Response.ok(config).build();

        } catch (Exception e) {
            log.error("Error updating configuration", e);
            return Response.serverError()
                    .entity(errorResponse("Failed to update configuration: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Test connection to GitHub Enterprise
     * POST /rest/github-integration/1.0/config/test-connection
     */
    @POST
    @Path("/test-connection")
    public Response testConnection(ConnectionTestRequest request) {
        try {
            // Check admin permissions
            if (!isAdmin()) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(errorResponse("Administrator access required"))
                        .build();
            }

            if (request.getGithubEnterpriseUrl() == null || request.getGithubEnterpriseUrl().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorResponse("GitHub Enterprise URL is required"))
                        .build();
            }

            if (request.getGithubToken() == null || request.getGithubToken().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorResponse("GitHub token is required"))
                        .build();
            }

            // Test connection
            boolean success = githubService.testConnection(
                    request.getGithubEnterpriseUrl(),
                    request.getGithubToken(),
                    request.isTrustCustomCertificates()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success
                    ? "Connection successful"
                    : "Connection failed - check logs for details");

            return Response.ok(response).build();

        } catch (Exception e) {
            log.error("Connection test failed", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Connection failed: " + e.getMessage());
            return Response.ok(response).build();
        }
    }

    /**
     * Register webhooks for all configured repositories
     * POST /rest/github-integration/1.0/config/register-webhooks
     */
    @POST
    @Path("/register-webhooks")
    public Response registerWebhooks() {
        try {
            // Check admin permissions
            if (!isAdmin()) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(errorResponse("Administrator access required"))
                        .build();
            }

            GitHubConfig config = configManager.getConfiguration();
            if (!config.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorResponse("Configuration is invalid"))
                        .build();
            }

            if (config.getWebhookSecret() == null || config.getWebhookSecret().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorResponse("Webhook secret is required"))
                        .build();
            }

            List<WebhookRegistrationResult> results = new ArrayList<>();

            for (RepositoryMapping mapping : config.getRepositories()) {
                try {
                    String webhookId = githubService.registerWebhook(
                            mapping.getGithubOwner(),
                            mapping.getGithubRepo(),
                            config.getWebhookUrl(),
                            config.getWebhookSecret()
                    );

                    // Store webhook ID
                    config.getWebhookIds().put(mapping.getFullRepoName(), webhookId);

                    results.add(new WebhookRegistrationResult(
                            mapping.getFullRepoName(),
                            true,
                            "Webhook registered: " + webhookId
                    ));

                    log.info("Registered webhook for {}", mapping.getFullRepoName());

                } catch (GitHubException e) {
                    results.add(new WebhookRegistrationResult(
                            mapping.getFullRepoName(),
                            false,
                            "Failed: " + e.getMessage()
                    ));
                    log.error("Failed to register webhook for " + mapping.getFullRepoName(), e);
                }
            }

            // Save updated config with webhook IDs
            configManager.saveConfiguration(config);

            Map<String, Object> response = new HashMap<>();
            response.put("results", results);
            response.put("totalCount", results.size());
            response.put("successCount", results.stream().filter(r -> r.success).count());

            return Response.ok(response).build();

        } catch (Exception e) {
            log.error("Error registering webhooks", e);
            return Response.serverError()
                    .entity(errorResponse("Failed to register webhooks: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Generate new webhook secret
     * POST /rest/github-integration/1.0/config/generate-secret
     */
    @POST
    @Path("/generate-secret")
    public Response generateSecret() {
        try {
            // Check admin permissions
            if (!isAdmin()) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(errorResponse("Administrator access required"))
                        .build();
            }

            String secret = WebhookValidator.generateWebhookSecret();

            Map<String, String> response = new HashMap<>();
            response.put("secret", secret);

            return Response.ok(response).build();

        } catch (Exception e) {
            log.error("Error generating secret", e);
            return Response.serverError()
                    .entity(errorResponse("Failed to generate secret: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Check if current user is admin
     */
    private boolean isAdmin() {
        ApplicationUser user = authenticationContext.getLoggedInUser();
        if (user == null) {
            return false;
        }
        return globalPermissionManager.hasPermission(Permissions.ADMINISTER, user);
    }

    /**
     * Validate configuration
     */
    private List<String> validateConfiguration(GitHubConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getGithubEnterpriseUrl() == null || config.getGithubEnterpriseUrl().isEmpty()) {
            errors.add("GitHub Enterprise URL is required");
        }

        if (config.getGithubToken() == null || config.getGithubToken().isEmpty()) {
            errors.add("GitHub token is required");
        }

        if (config.getRepositories() == null || config.getRepositories().isEmpty()) {
            errors.add("At least one repository mapping is required");
        } else {
            // Validate each repository mapping
            for (int i = 0; i < config.getRepositories().size(); i++) {
                RepositoryMapping mapping = config.getRepositories().get(i);
                if (mapping.getJiraProject() == null || mapping.getJiraProject().isEmpty()) {
                    errors.add("Repository mapping " + (i + 1) + ": Jira project is required");
                }
                if (mapping.getGithubOwner() == null || mapping.getGithubOwner().isEmpty()) {
                    errors.add("Repository mapping " + (i + 1) + ": GitHub owner is required");
                }
                if (mapping.getGithubRepo() == null || mapping.getGithubRepo().isEmpty()) {
                    errors.add("Repository mapping " + (i + 1) + ": GitHub repository is required");
                }
            }
        }

        return errors;
    }

    /**
     * Helper method to create error response
     */
    private Map<String, String> errorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }

    // DTOs

    public static class ConnectionTestRequest {
        private String githubEnterpriseUrl;
        private String githubToken;
        private boolean trustCustomCertificates;

        public String getGithubEnterpriseUrl() {
            return githubEnterpriseUrl;
        }

        public void setGithubEnterpriseUrl(String githubEnterpriseUrl) {
            this.githubEnterpriseUrl = githubEnterpriseUrl;
        }

        public String getGithubToken() {
            return githubToken;
        }

        public void setGithubToken(String githubToken) {
            this.githubToken = githubToken;
        }

        public boolean isTrustCustomCertificates() {
            return trustCustomCertificates;
        }

        public void setTrustCustomCertificates(boolean trustCustomCertificates) {
            this.trustCustomCertificates = trustCustomCertificates;
        }
    }

    public static class WebhookRegistrationResult {
        private String repository;
        private boolean success;
        private String message;

        public WebhookRegistrationResult(String repository, boolean success, String message) {
            this.repository = repository;
            this.success = success;
            this.message = message;
        }

        public String getRepository() {
            return repository;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
