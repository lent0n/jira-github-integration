package com.healthcanada.jira.github.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcanada.jira.github.model.GitHubConfig;
import com.healthcanada.jira.github.model.WebhookPayload;
import com.healthcanada.jira.github.security.WebhookValidator;
import com.healthcanada.jira.github.service.SyncService;
import com.healthcanada.jira.github.storage.PluginConfigurationManager;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Servlet that receives GitHub Enterprise webhook events
 * Compatible with Jira 9.12+ and 10.3+
 */
public class GitHubWebhookListener extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookListener.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final PluginConfigurationManager configManager;
    private final SyncService syncService;

    // Constructor injection for Spring components
    @Autowired
    public GitHubWebhookListener(PluginConfigurationManager configManager, SyncService syncService) {
        this.configManager = configManager;
        this.syncService = syncService;
        log.info("GitHubWebhookListener initialized");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String eventType = request.getHeader("X-GitHub-Event");

        log.info("Received GitHub webhook: event={}, remoteAddr={}", eventType, request.getRemoteAddr());

        try {
            // Read request body
            String payload = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);

            if (payload == null || payload.isEmpty()) {
                log.warn("Received empty webhook payload");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Empty payload\"}");
                return;
            }

            // Load configuration
            GitHubConfig config = configManager.getConfiguration();
            if (!config.isValid() || config.getWebhookSecret() == null || config.getWebhookSecret().isEmpty()) {
                log.error("Webhook received but configuration is invalid or webhook secret not set");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"Configuration error\"}");
                return;
            }

            // Verify GitHub signature
            String signature = request.getHeader("X-Hub-Signature-256");
            if (signature == null || signature.isEmpty()) {
                log.warn("Webhook received without signature");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Missing signature\"}");
                return;
            }

            boolean isValid = WebhookValidator.verifySignature(payload, signature, config.getWebhookSecret());
            if (!isValid) {
                log.error("Webhook signature verification failed");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid signature\"}");
                return;
            }

            log.debug("Webhook signature verified successfully");

            // Handle different event types
            switch (eventType != null ? eventType : "") {
                case "pull_request":
                    handlePullRequestEvent(payload);
                    break;

                case "push":
                    handlePushEvent(payload);
                    break;

                case "ping":
                    log.info("Received ping event from GitHub");
                    break;

                default:
                    log.debug("Ignoring unsupported event type: {}", eventType);
            }

            // Success response
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\": \"ok\"}");

            long duration = System.currentTimeMillis() - startTime;
            log.info("Webhook processed successfully in {}ms", duration);

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Internal server error\"}");
        }
    }

    /**
     * Handle pull_request webhook events
     */
    private void handlePullRequestEvent(String payload) {
        try {
            WebhookPayload webhookPayload = objectMapper.readValue(payload, WebhookPayload.class);
            String action = webhookPayload.getAction();
            WebhookPayload.PullRequest pr = webhookPayload.getPullRequest();

            if (pr == null) {
                log.warn("Pull request event received but PR data is null");
                return;
            }

            log.info("Processing pull_request event: action={}, pr=#{}, title={}",
                    action, pr.getNumber(), pr.getTitle());

            // Extract issue key from branch name or PR title
            String issueKey = extractIssueKey(pr);
            if (issueKey == null) {
                log.debug("No Jira issue key found in PR #{} - skipping", pr.getNumber());
                return;
            }

            log.info("Found issue key {} in PR #{}", issueKey, pr.getNumber());

            // Handle based on action
            switch (action != null ? action : "") {
                case "opened":
                    syncService.handlePROpened(issueKey, pr);
                    break;

                case "closed":
                    if (pr.isMerged()) {
                        syncService.handlePRMerged(issueKey, pr);
                    } else {
                        syncService.handlePRClosed(issueKey, pr);
                    }
                    break;

                case "reopened":
                    syncService.handlePRReopened(issueKey, pr);
                    break;

                case "synchronize":
                    // PR updated with new commits - could add handling here
                    log.debug("PR #{} synchronized (new commits pushed)", pr.getNumber());
                    break;

                default:
                    log.debug("Ignoring pull_request action: {}", action);
            }

        } catch (Exception e) {
            log.error("Error handling pull_request event", e);
        }
    }

    /**
     * Handle push webhook events (future enhancement)
     */
    private void handlePushEvent(String payload) {
        try {
            log.debug("Push event received (not yet implemented)");
            // Future: Parse commits, extract issue keys, add comments to Jira
        } catch (Exception e) {
            log.error("Error handling push event", e);
        }
    }

    /**
     * Extract Jira issue key from PR
     * Tries branch name first, then PR title
     */
    private String extractIssueKey(WebhookPayload.PullRequest pr) {
        // Try branch name first (e.g., "feature/PROJ-123-description")
        if (pr.getHead() != null && pr.getHead().getRef() != null) {
            String issueKey = syncService.extractIssueKey(pr.getHead().getRef());
            if (issueKey != null) {
                return issueKey;
            }
        }

        // Try PR title (e.g., "[PROJ-123] Feature description")
        if (pr.getTitle() != null) {
            String issueKey = syncService.extractIssueKey(pr.getTitle());
            if (issueKey != null) {
                return issueKey;
            }
        }

        return null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Return simple info page for GET requests (useful for debugging)
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"message\": \"GitHub Webhook Listener\", \"status\": \"operational\"}");
    }
}
