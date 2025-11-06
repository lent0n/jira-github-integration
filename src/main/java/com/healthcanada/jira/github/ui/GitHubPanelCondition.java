package com.healthcanada.jira.github.ui;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.healthcanada.jira.github.model.GitHubConfig;
import com.healthcanada.jira.github.storage.PluginConfigurationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Condition that determines whether to show the GitHub integration panel
 * Shows panel only if:
 * 1. Plugin is configured
 * 2. There's a repository mapping for the issue's project
 * Compatible with Jira 9.12+ and 10.3+
 */
@Component
public class GitHubPanelCondition extends AbstractWebCondition {

    private final PluginConfigurationManager configManager;

    @Autowired
    public GitHubPanelCondition(PluginConfigurationManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper) {
        // Get the current issue
        Issue issue = (Issue) jiraHelper.getContextParams().get("issue");
        if (issue == null) {
            return false;
        }

        // Check if configuration exists
        if (!configManager.hasConfiguration()) {
            return false;
        }

        // Check if there's a repository mapping for this project
        GitHubConfig config = configManager.getConfiguration();
        if (!config.isValid()) {
            return false;
        }

        String projectKey = issue.getProjectObject().getKey();
        return config.getRepositoryMapping(projectKey) != null;
    }
}
