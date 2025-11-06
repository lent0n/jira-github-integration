package com.healthcanada.jira.github.ui;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.ApplicationProperties;
import com.healthcanada.jira.github.storage.PluginConfigurationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides context data for the GitHub integration web panel
 * Compatible with Jira 9.12+ and 10.3+
 */
@Component
public class GitHubPanelContextProvider extends AbstractJiraContextProvider {

    private final PluginConfigurationManager configManager;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public GitHubPanelContextProvider(PluginConfigurationManager configManager,
                                       ApplicationProperties applicationProperties) {
        this.configManager = configManager;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public Map<String, Object> getContextMap(ApplicationUser user, JiraHelper jiraHelper) {
        Map<String, Object> context = new HashMap<>();

        // Get the current issue
        Issue issue = (Issue) jiraHelper.getContextParams().get("issue");
        if (issue != null) {
            context.put("issue", issue);
            context.put("issueKey", issue.getKey());
            context.put("projectKey", issue.getProjectObject().getKey());
            context.put("summary", issue.getSummary());
        }

        // Check if configuration exists
        boolean hasConfiguration = configManager.hasConfiguration();
        context.put("hasConfiguration", hasConfiguration);

        // Provide base URLs for REST API calls
        String baseUrl = applicationProperties.getBaseUrl();
        context.put("baseUrl", baseUrl);
        context.put("restUrl", baseUrl + "/rest/github-integration/1.0");

        // Provide configuration URL for admin link
        context.put("configUrl", baseUrl + "/plugins/servlet/github-integration/admin");

        // User information
        if (user != null) {
            context.put("currentUser", user.getUsername());
            context.put("userDisplayName", user.getDisplayName());
        }

        return context;
    }
}
