package com.healthcanada.jira.github.service;

import com.healthcanada.jira.github.model.GitHubConfig;
import com.healthcanada.jira.github.model.WebhookPayload;
import com.healthcanada.jira.github.storage.PluginConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates synchronization between GitHub events and Jira updates
 */
@Component
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    // Pattern to extract Jira issue keys (PROJECT-123)
    private static final Pattern ISSUE_KEY_PATTERN = Pattern.compile("([A-Z][A-Z0-9]+-\\d+)");

    private final JiraService jiraService;
    private final PluginConfigurationManager configManager;

    @Autowired
    public SyncService(JiraService jiraService, PluginConfigurationManager configManager) {
        this.jiraService = jiraService;
        this.configManager = configManager;
    }

    /**
     * Handle pull request opened event
     */
    public void handlePROpened(String issueKey, WebhookPayload.PullRequest pr) {
        try {
            log.info("Handling PR opened event for issue {} - PR #{}", issueKey, pr.getNumber());

            GitHubConfig config = configManager.getConfiguration();

            // Get configured transition for "pr_opened"
            String targetStatus = config.getTransitionMappings().get("pr_opened");

            if (targetStatus != null && !targetStatus.isEmpty()) {
                jiraService.transitionIssue(issueKey, targetStatus);
                log.info("Transitioned issue {} to {}", issueKey, targetStatus);
            } else {
                log.debug("No transition configured for pr_opened");
            }

            // Add comment
            String comment = buildPROpenedComment(pr);
            jiraService.addComment(issueKey, comment);

            // Update or create remote link
            jiraService.createRemoteLink(issueKey, pr.getHtmlUrl(),
                    String.format("PR #%d: %s", pr.getNumber(), pr.getTitle()));

            log.info("Successfully handled PR opened event for issue {}", issueKey);

        } catch (Exception e) {
            log.error("Failed to handle PR opened event for issue " + issueKey, e);
        }
    }

    /**
     * Handle pull request merged event
     */
    public void handlePRMerged(String issueKey, WebhookPayload.PullRequest pr) {
        try {
            log.info("Handling PR merged event for issue {} - PR #{}", issueKey, pr.getNumber());

            GitHubConfig config = configManager.getConfiguration();

            // Get configured transition for "pr_merged"
            String targetStatus = config.getTransitionMappings().get("pr_merged");

            if (targetStatus != null && !targetStatus.isEmpty()) {
                jiraService.transitionIssue(issueKey, targetStatus);
                log.info("Transitioned issue {} to {}", issueKey, targetStatus);
            } else {
                log.debug("No transition configured for pr_merged");
            }

            // Add comment
            String comment = buildPRMergedComment(pr);
            jiraService.addComment(issueKey, comment);

            log.info("Successfully handled PR merged event for issue {}", issueKey);

        } catch (Exception e) {
            log.error("Failed to handle PR merged event for issue " + issueKey, e);
        }
    }

    /**
     * Handle pull request closed (not merged) event
     */
    public void handlePRClosed(String issueKey, WebhookPayload.PullRequest pr) {
        try {
            log.info("Handling PR closed event for issue {} - PR #{}", issueKey, pr.getNumber());

            GitHubConfig config = configManager.getConfiguration();

            // Get configured transition for "pr_closed"
            String targetStatus = config.getTransitionMappings().get("pr_closed");

            if (targetStatus != null && !targetStatus.isEmpty()) {
                jiraService.transitionIssue(issueKey, targetStatus);
                log.info("Transitioned issue {} to {}", issueKey, targetStatus);
            } else {
                log.debug("No transition configured for pr_closed");
            }

            // Add comment
            String comment = buildPRClosedComment(pr);
            jiraService.addComment(issueKey, comment);

            log.info("Successfully handled PR closed event for issue {}", issueKey);

        } catch (Exception e) {
            log.error("Failed to handle PR closed event for issue " + issueKey, e);
        }
    }

    /**
     * Handle pull request reopened event
     */
    public void handlePRReopened(String issueKey, WebhookPayload.PullRequest pr) {
        try {
            log.info("Handling PR reopened event for issue {} - PR #{}", issueKey, pr.getNumber());

            GitHubConfig config = configManager.getConfiguration();

            // Get configured transition for "pr_reopened"
            String targetStatus = config.getTransitionMappings().get("pr_reopened");

            if (targetStatus != null && !targetStatus.isEmpty()) {
                jiraService.transitionIssue(issueKey, targetStatus);
                log.info("Transitioned issue {} to {}", issueKey, targetStatus);
            } else {
                log.debug("No transition configured for pr_reopened");
            }

            // Add comment
            String comment = buildPRReopenedComment(pr);
            jiraService.addComment(issueKey, comment);

            log.info("Successfully handled PR reopened event for issue {}", issueKey);

        } catch (Exception e) {
            log.error("Failed to handle PR reopened event for issue " + issueKey, e);
        }
    }

    /**
     * Extract issue key from text (branch name or PR title)
     */
    public String extractIssueKey(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        Matcher matcher = ISSUE_KEY_PATTERN.matcher(text);
        if (matcher.find()) {
            String issueKey = matcher.group(1);
            log.debug("Extracted issue key {} from text: {}", issueKey, text);
            return issueKey;
        }

        log.debug("No issue key found in text: {}", text);
        return null;
    }

    /**
     * Build comment for PR opened event
     */
    private String buildPROpenedComment(WebhookPayload.PullRequest pr) {
        StringBuilder comment = new StringBuilder();
        comment.append("Pull request opened: [PR #").append(pr.getNumber());
        comment.append("|").append(pr.getHtmlUrl()).append("]\n");
        comment.append("Title: ").append(pr.getTitle()).append("\n");

        if (pr.getUser() != null) {
            comment.append("Author: @").append(pr.getUser().getLogin()).append("\n");
        }

        if (pr.getHead() != null) {
            comment.append("Branch: ").append(pr.getHead().getRef()).append("\n");
        }

        return comment.toString();
    }

    /**
     * Build comment for PR merged event
     */
    private String buildPRMergedComment(WebhookPayload.PullRequest pr) {
        StringBuilder comment = new StringBuilder();
        comment.append("Pull request merged: [PR #").append(pr.getNumber());
        comment.append("|").append(pr.getHtmlUrl()).append("]\n");

        if (pr.getMergedBy() != null) {
            comment.append("Merged by: @").append(pr.getMergedBy().getLogin()).append("\n");
        }

        if (pr.getMergedAt() != null) {
            comment.append("Merged at: ").append(pr.getMergedAt()).append("\n");
        }

        return comment.toString();
    }

    /**
     * Build comment for PR closed event
     */
    private String buildPRClosedComment(WebhookPayload.PullRequest pr) {
        StringBuilder comment = new StringBuilder();
        comment.append("Pull request closed without merging: [PR #").append(pr.getNumber());
        comment.append("|").append(pr.getHtmlUrl()).append("]\n");

        if (pr.getClosedBy() != null) {
            comment.append("Closed by: @").append(pr.getClosedBy().getLogin()).append("\n");
        }

        if (pr.getClosedAt() != null) {
            comment.append("Closed at: ").append(pr.getClosedAt()).append("\n");
        }

        return comment.toString();
    }

    /**
     * Build comment for PR reopened event
     */
    private String buildPRReopenedComment(WebhookPayload.PullRequest pr) {
        StringBuilder comment = new StringBuilder();
        comment.append("Pull request reopened: [PR #").append(pr.getNumber());
        comment.append("|").append(pr.getHtmlUrl()).append("]\n");

        if (pr.getUser() != null) {
            comment.append("Reopened by: @").append(pr.getUser().getLogin()).append("\n");
        }

        return comment.toString();
    }
}
