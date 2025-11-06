package com.healthcanada.jira.github.service;

import com.healthcanada.jira.github.model.GitHubConfig;
import com.healthcanada.jira.github.model.GitHubException;
import com.healthcanada.jira.github.model.RepositoryMapping;
import com.healthcanada.jira.github.storage.PluginConfigurationManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Business logic for GitHub operations
 */
@Component
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    private final PluginConfigurationManager configManager;
    private final GitHubEnterpriseClient githubClient;
    private final JiraService jiraService;

    @Autowired
    public GitHubService(PluginConfigurationManager configManager,
                         GitHubEnterpriseClient githubClient,
                         JiraService jiraService) {
        this.configManager = configManager;
        this.githubClient = githubClient;
        this.jiraService = jiraService;
    }

    /**
     * Create a branch from a Jira issue
     */
    public Map<String, Object> createBranch(String issueKey, String baseBranch, String branchName)
            throws GitHubException {
        try {
            // Load configuration
            GitHubConfig config = configManager.getConfiguration();
            if (!config.isValid()) {
                throw new GitHubException("GitHub integration is not configured");
            }

            // Get issue details
            Map<String, String> issueDetails = jiraService.getIssueDetails(issueKey);
            String projectKey = issueDetails.get("projectKey");

            // Find repository mapping
            RepositoryMapping mapping = config.getRepositoryMapping(projectKey);
            if (mapping == null) {
                throw new GitHubException("No repository mapping found for project: " + projectKey);
            }

            // Initialize GitHub client
            githubClient.initialize(config.getGithubEnterpriseUrl(), config.getGithubToken(),
                    config.isTrustCustomCertificates());

            // Sanitize branch name
            String sanitizedBranchName = sanitizeBranchName(branchName, issueKey, issueDetails);

            // Get base branch SHA
            String baseSha = githubClient.getBranchSha(mapping.getGithubOwner(), mapping.getGithubRepo(),
                    baseBranch != null ? baseBranch : mapping.getDefaultBranch());

            // Create branch
            Map<String, Object> result = githubClient.createBranch(
                    mapping.getGithubOwner(),
                    mapping.getGithubRepo(),
                    sanitizedBranchName,
                    baseSha
            );

            // Add comment to Jira issue
            String branchUrl = String.format("%s/%s/%s/tree/%s",
                    config.getGithubEnterpriseUrl(),
                    mapping.getGithubOwner(),
                    mapping.getGithubRepo(),
                    sanitizedBranchName);

            jiraService.addComment(issueKey, String.format("Branch created: [%s|%s]",
                    sanitizedBranchName, branchUrl));

            // Create remote link
            jiraService.createRemoteLink(issueKey, branchUrl, "Branch: " + sanitizedBranchName);

            log.info("Successfully created branch {} for issue {}", sanitizedBranchName, issueKey);
            return result;

        } catch (GitHubException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create branch for issue " + issueKey, e);
            throw new GitHubException("Failed to create branch: " + e.getMessage(), e);
        }
    }

    /**
     * Create a pull request from a Jira issue
     */
    public Map<String, Object> createPullRequest(String issueKey, String sourceBranch,
                                                   String targetBranch, String title, String description)
            throws GitHubException {
        try {
            // Load configuration
            GitHubConfig config = configManager.getConfiguration();
            if (!config.isValid()) {
                throw new GitHubException("GitHub integration is not configured");
            }

            // Get issue details
            Map<String, String> issueDetails = jiraService.getIssueDetails(issueKey);
            String projectKey = issueDetails.get("projectKey");

            // Find repository mapping
            RepositoryMapping mapping = config.getRepositoryMapping(projectKey);
            if (mapping == null) {
                throw new GitHubException("No repository mapping found for project: " + projectKey);
            }

            // Initialize GitHub client
            githubClient.initialize(config.getGithubEnterpriseUrl(), config.getGithubToken(),
                    config.isTrustCustomCertificates());

            // Build PR body with Jira link
            String jiraUrl = jiraService.getIssueUrl(issueKey);
            String prBody = String.format("Related to %s\n\n%s", jiraUrl, description != null ? description : "");

            // Create pull request
            Map<String, Object> result = githubClient.createPullRequest(
                    mapping.getGithubOwner(),
                    mapping.getGithubRepo(),
                    title,
                    sourceBranch,
                    targetBranch != null ? targetBranch : mapping.getDefaultBranch(),
                    prBody
            );

            // Transition issue to "In Review" if configured
            String prOpenedStatus = config.getTransitionMappings().get("pr_opened");
            if (prOpenedStatus != null && !prOpenedStatus.isEmpty()) {
                try {
                    jiraService.transitionIssue(issueKey, prOpenedStatus);
                } catch (Exception e) {
                    log.warn("Failed to transition issue {} to {}: {}", issueKey, prOpenedStatus, e.getMessage());
                }
            }

            // Add comment to Jira issue
            jiraService.addComment(issueKey, String.format("Pull request created: [PR #%d|%s]",
                    result.get("number"), result.get("url")));

            // Create remote link
            jiraService.createRemoteLink(issueKey, (String) result.get("url"),
                    "PR #" + result.get("number"));

            log.info("Successfully created PR #{} for issue {}", result.get("number"), issueKey);
            return result;

        } catch (GitHubException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create pull request for issue " + issueKey, e);
            throw new GitHubException("Failed to create pull request: " + e.getMessage(), e);
        }
    }

    /**
     * Register webhook for a repository
     */
    public String registerWebhook(String owner, String repo, String webhookUrl, String secret)
            throws GitHubException {
        try {
            GitHubConfig config = configManager.getConfiguration();
            githubClient.initialize(config.getGithubEnterpriseUrl(), config.getGithubToken(),
                    config.isTrustCustomCertificates());

            return githubClient.registerWebhook(owner, repo, webhookUrl, secret);
        } catch (Exception e) {
            log.error("Failed to register webhook for {}/{}", owner, repo, e);
            throw new GitHubException("Failed to register webhook: " + e.getMessage(), e);
        }
    }

    /**
     * Sanitize branch name (remove special characters, limit length)
     */
    private String sanitizeBranchName(String branchName, String issueKey, Map<String, String> issueDetails) {
        String result = branchName;

        // Replace template variables
        result = result.replace("{issueKey}", issueKey);
        result = result.replace("{project}", issueDetails.get("projectKey"));
        result = result.replace("{issueType}", issueDetails.get("issueType"));

        // Handle {summary} - sanitize and truncate
        if (result.contains("{summary}")) {
            String summary = issueDetails.get("summary");
            String sanitizedSummary = summary
                    .toLowerCase()
                    .replaceAll("[^a-z0-9-]", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");

            // Truncate to 50 characters
            if (sanitizedSummary.length() > 50) {
                sanitizedSummary = sanitizedSummary.substring(0, 50);
            }

            result = result.replace("{summary}", sanitizedSummary);
        }

        return result;
    }

    /**
     * Test GitHub connection
     */
    public boolean testConnection(String baseUrl, String apiToken, boolean trustCustomCertificates) {
        try {
            GitHubEnterpriseClient testClient = new GitHubEnterpriseClient();
            testClient.initialize(baseUrl, apiToken, trustCustomCertificates);
            boolean success = testClient.testConnection();
            testClient.close();
            return success;
        } catch (Exception e) {
            log.error("Connection test failed", e);
            return false;
        }
    }
}
