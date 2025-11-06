package com.healthcanada.jira.github.ui;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.healthcanada.jira.github.storage.PluginConfigurationManager;
import com.healthcanada.jira.github.model.GitHubConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet for GitHub Integration admin configuration page
 * Provides web UI for plugin configuration
 * Compatible with Jira 9.12+ and 10.3+
 */
public class ConfigurationServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationServlet.class);
    private static final String TEMPLATE_PATH = "templates/config-page.vm";

    private final TemplateRenderer templateRenderer;
    private final PluginConfigurationManager configManager;
    private final GlobalPermissionManager globalPermissionManager;
    private final JiraAuthenticationContext authenticationContext;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public ConfigurationServlet(TemplateRenderer templateRenderer,
                                 PluginConfigurationManager configManager,
                                 GlobalPermissionManager globalPermissionManager,
                                 JiraAuthenticationContext authenticationContext,
                                 ApplicationProperties applicationProperties) {
        this.templateRenderer = templateRenderer;
        this.configManager = configManager;
        this.globalPermissionManager = globalPermissionManager;
        this.authenticationContext = authenticationContext;
        this.applicationProperties = applicationProperties;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Check admin permissions
        ApplicationUser currentUser = authenticationContext.getLoggedInUser();
        if (currentUser == null || !isSystemAdmin(currentUser)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Administrator access required to access this page");
            return;
        }

        log.info("Admin configuration page accessed by user: {}", currentUser.getUsername());

        // Prepare context for template
        Map<String, Object> context = buildContext();

        // Render template
        resp.setContentType("text/html;charset=utf-8");
        templateRenderer.render(TEMPLATE_PATH, context, resp.getWriter());
    }

    /**
     * Build context map for Velocity template
     */
    private Map<String, Object> buildContext() {
        Map<String, Object> context = new HashMap<>();

        // Base URLs
        String baseUrl = applicationProperties.getBaseUrl();
        context.put("baseUrl", baseUrl);
        context.put("restUrl", baseUrl + "/rest/github-integration/1.0");

        // Current user information
        ApplicationUser currentUser = authenticationContext.getLoggedInUser();
        if (currentUser != null) {
            context.put("currentUser", currentUser.getUsername());
            context.put("userDisplayName", currentUser.getDisplayName());
        }

        // Configuration status
        boolean hasConfiguration = configManager.hasConfiguration();
        context.put("hasConfiguration", hasConfiguration);

        if (hasConfiguration) {
            GitHubConfig config = configManager.getConfiguration();

            // Mask sensitive data for display
            GitHubConfig displayConfig = new GitHubConfig();
            displayConfig.setGithubEnterpriseUrl(config.getGithubEnterpriseUrl());
            displayConfig.setGithubApiUrl(config.getGithubApiUrl());
            displayConfig.setTrustCustomCertificates(config.isTrustCustomCertificates());
            displayConfig.setWebhookUrl(config.getWebhookUrl());
            displayConfig.setBranchNaming(config.getBranchNaming());
            displayConfig.setRepositories(config.getRepositories());
            displayConfig.setTransitionMappings(config.getTransitionMappings());

            // Mask tokens
            displayConfig.setGithubToken(config.getGithubToken() != null ? "********" : null);
            displayConfig.setWebhookSecret(config.getWebhookSecret() != null ? "********" : null);

            context.put("config", displayConfig);
            context.put("hasToken", config.getGithubToken() != null);
            context.put("hasWebhookSecret", config.getWebhookSecret() != null);

            // Repository count
            int repoCount = config.getRepositories() != null ? config.getRepositories().size() : 0;
            context.put("repositoryCount", repoCount);

            // Webhook registration status
            Map<String, String> webhookIds = config.getWebhookIds();
            int registeredWebhooks = webhookIds != null ? webhookIds.size() : 0;
            context.put("registeredWebhooks", registeredWebhooks);
            context.put("allWebhooksRegistered", repoCount > 0 && registeredWebhooks == repoCount);
        }

        // Default values for new configuration
        context.put("defaultBaseBranch", "main");
        context.put("defaultBranchNaming", "feature/{issueKey}-{summary}");
        context.put("defaultTransitionMappings", getDefaultTransitionMappings());

        // Help text and documentation links
        context.put("githubApiDocs", "https://docs.github.com/en/enterprise-server/rest");
        context.put("webhookDocs", "https://docs.github.com/en/enterprise-server/webhooks");

        return context;
    }

    /**
     * Get default transition mappings
     */
    private Map<String, String> getDefaultTransitionMappings() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("pr_opened", "In Review");
        defaults.put("pr_merged", "Done");
        defaults.put("pr_closed", "Cancelled");
        defaults.put("pr_reopened", "In Progress");
        return defaults;
    }

    /**
     * Check if user is system administrator
     */
    private boolean isSystemAdmin(ApplicationUser user) {
        if (user == null) {
            return false;
        }
        return globalPermissionManager.hasPermission(Permissions.ADMINISTER, user);
    }
}
