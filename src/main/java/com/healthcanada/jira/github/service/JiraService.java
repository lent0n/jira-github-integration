package com.healthcanada.jira.github.service;

import com.atlassian.jira.bc.issue.IssueService;
//import com.atlassian.jira.bc.issue.link.RemoteLinkService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
//import com.atlassian.jira.issue.link.RemoteIssueLink;
//import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.TransitionOptions;
import com.atlassian.sal.api.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Business logic for Jira operations
 */
@Component
public class JiraService {

    private static final Logger log = LoggerFactory.getLogger(JiraService.class);

    private final IssueManager issueManager;
    private final IssueService issueService;
    //private final RemoteLinkService remoteLinkService;
    private final JiraAuthenticationContext authenticationContext;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public JiraService(IssueManager issueManager,
                       IssueService issueService,
                       //RemoteLinkService remoteLinkService,
                       JiraAuthenticationContext authenticationContext,
                       ApplicationProperties applicationProperties) {
        this.issueManager = issueManager;
        this.issueService = issueService;
        //this.remoteLinkService = remoteLinkService;
        this.authenticationContext = authenticationContext;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Get issue details
     */
    public Map<String, String> getIssueDetails(String issueKey) {
        Issue issue = issueManager.getIssueByCurrentKey(issueKey);
        if (issue == null) {
            throw new IllegalArgumentException("Issue not found: " + issueKey);
        }

        Map<String, String> details = new HashMap<>();
        details.put("key", issue.getKey());
        details.put("projectKey", issue.getProjectObject().getKey());
        details.put("summary", issue.getSummary());
        details.put("issueType", issue.getIssueType().getName());
        details.put("status", issue.getStatus().getName());

        if (issue.getAssignee() != null) {
            details.put("assignee", issue.getAssignee().getUsername());
        }

        return details;
    }

    /**
     * Get issue URL
     */
    public String getIssueUrl(String issueKey) {
        String baseUrl = applicationProperties.getBaseUrl();
        return baseUrl + "/browse/" + issueKey;
    }

    /**
     * Add comment to issue
     * TODO: Re-enable when IssueService API is available
     */
    public void addComment(String issueKey, String comment) {
        // Temporarily disabled due to API compatibility issues
        log.debug("Would add comment to issue {} (functionality disabled): {}", issueKey, comment);
        /*
        try {
            Issue issue = issueManager.getIssueByCurrentKey(issueKey);
            if (issue == null) {
                log.warn("Cannot add comment - issue not found: {}", issueKey);
                return;
            }

            ApplicationUser user = authenticationContext.getLoggedInUser();
            IssueService.IssueResult commentResult = issueService.validateAddComment(
                    user, issue.getId(), comment);

            if (commentResult.isValid()) {
                issueService.addComment(user, commentResult);
                log.debug("Added comment to issue {}", issueKey);
            } else {
                log.warn("Failed to validate comment for issue {}: {}",
                        issueKey, commentResult.getErrorCollection());
            }
        } catch (Exception e) {
            log.error("Failed to add comment to issue " + issueKey, e);
        }
        */
    }

    /**
     * Create remote link to GitHub resource
     */
    public void createRemoteLink(String issueKey, String url, String title) {
        try {
            Issue issue = issueManager.getIssueByCurrentKey(issueKey);
            if (issue == null) {
                log.warn("Cannot create remote link - issue not found: {}", issueKey);
                return;
            }

            ApplicationUser user = authenticationContext.getLoggedInUser();

            // TODO: Remote link functionality disabled due to dependency issues
            // Re-enable when RemoteLinkService is available
            /*
            RemoteIssueLinkBuilder linkBuilder = new RemoteIssueLinkBuilder();
            linkBuilder.issueId(issue.getId());
            linkBuilder.url(url);
            linkBuilder.title(title);
            linkBuilder.summary(title);
            linkBuilder.applicationType("com.healthcanada.jira.github");
            linkBuilder.applicationName("GitHub");

            RemoteLinkService.RemoteIssueLinkResult result = remoteLinkService.validateCreate(
                    user, linkBuilder.build());

            if (result.isValid()) {
                remoteLinkService.create(user, result);
                log.debug("Created remote link for issue {} to {}", issueKey, url);
            } else {
                log.warn("Failed to validate remote link for issue {}: {}",
                        issueKey, result.getErrorCollection());
            }
            */
            log.debug("Would create remote link for issue {} to {} (functionality disabled)", issueKey, url);
        } catch (Exception e) {
            log.error("Failed to create remote link for issue " + issueKey, e);
        }
    }

    /**
     * Transition issue to a new status
     * TODO: Re-enable when IssueService API is available
     */
    public void transitionIssue(String issueKey, String targetStatus) {
        log.debug("Would transition issue {} to {} (functionality disabled)", issueKey, targetStatus);
        // Temporarily disabled due to API compatibility issues
        /*
        try {
            Issue issue = issueManager.getIssueByCurrentKey(issueKey);
            if (issue == null) {
                log.warn("Cannot transition - issue not found: {}", issueKey);
                return;
            }

            // Current status
            String currentStatus = issue.getStatus().getName();
            if (currentStatus.equalsIgnoreCase(targetStatus)) {
                log.debug("Issue {} already in status {}", issueKey, targetStatus);
                return;
            }

            ApplicationUser user = authenticationContext.getLoggedInUser();

            // Find transition by target status name
            IssueService.IssueResult transitionResult = findAndExecuteTransition(
                    user, issue, targetStatus);

            if (transitionResult != null && transitionResult.isValid()) {
                log.info("Transitioned issue {} from {} to {}", issueKey, currentStatus, targetStatus);
            } else {
                log.warn("Failed to transition issue {} to {}: no valid transition found or error occurred",
                        issueKey, targetStatus);
            }
        } catch (Exception e) {
            log.error("Failed to transition issue " + issueKey + " to " + targetStatus, e);
        }
        */
    }

    /**
     * Find and execute transition to target status
     * TODO: Re-enable when IssueService API is available
     */
    /*
    private IssueService.IssueResult findAndExecuteTransition(ApplicationUser user, Issue issue,
                                                               String targetStatus) {
        try {
            IssueService.TransitionValidationResult validationResult =
                    issueService.validateTransition(user, issue.getId(), -1,
                            new IssueService.IssueInputParameters(), TransitionOptions.defaults());

            if (!validationResult.isValid()) {
                log.warn("Cannot get transitions for issue {}: {}",
                        issue.getKey(), validationResult.getErrorCollection());
                return null;
            }

            // Find transition that leads to target status
            Integer transitionId = validationResult.getTransitions().stream()
                    .filter(t -> t.getDestinationWorkflowStatus().getName().equalsIgnoreCase(targetStatus))
                    .map(t -> t.getId())
                    .findFirst()
                    .orElse(null);

            if (transitionId == null) {
                log.warn("No transition found from {} to {} for issue {}",
                        issue.getStatus().getName(), targetStatus, issue.getKey());
                return null;
            }

            // Execute transition
            IssueService.TransitionValidationResult transitionValidation =
                    issueService.validateTransition(user, issue.getId(), transitionId,
                            new IssueService.IssueInputParameters(), TransitionOptions.defaults());

            if (transitionValidation.isValid()) {
                return issueService.transition(user, transitionValidation);
            } else {
                log.warn("Transition validation failed for issue {}: {}",
                        issue.getKey(), transitionValidation.getErrorCollection());
                return null;
            }
        } catch (Exception e) {
            log.error("Error finding/executing transition for issue " + issue.getKey(), e);
            return null;
        }
    }
    */

    /**
     * Check if user has permission to view issue
     */
    public boolean hasViewPermission(ApplicationUser user, String issueKey) {
        try {
            Issue issue = issueManager.getIssueByCurrentKey(issueKey);
            if (issue == null) {
                return false;
            }

            IssueService.IssueResult result = issueService.getIssue(user, issueKey);
            return result.isValid();
        } catch (Exception e) {
            log.error("Failed to check view permission for issue " + issueKey, e);
            return false;
        }
    }

    /**
     * Check if user has permission to edit issue
     * TODO: Re-enable when IssueService API is available
     */
    public boolean hasEditPermission(ApplicationUser user, String issueKey) {
        // Temporarily disabled - assume true for now
        return true;
        /*
        try {
            Issue issue = issueManager.getIssueByCurrentKey(issueKey);
            if (issue == null) {
                return false;
            }

            IssueService.UpdateValidationResult result = issueService.validateUpdate(
                    user, issue.getId(), new IssueService.IssueInputParameters());
            return result.isValid();
        } catch (Exception e) {
            log.error("Failed to check edit permission for issue " + issueKey, e);
            return false;
        }
        */
    }
}
