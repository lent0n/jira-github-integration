package com.healthcanada.jira.github.api;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.healthcanada.jira.github.model.GitHubException;
import com.healthcanada.jira.github.service.GitHubService;
import com.healthcanada.jira.github.service.JiraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API for GitHub integration operations
 */
@Path("/github-integration/1.0")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GitHubIntegrationResource {

    private static final Logger log = LoggerFactory.getLogger(GitHubIntegrationResource.class);

    private final GitHubService githubService;
    private final JiraService jiraService;
    private final JiraAuthenticationContext authenticationContext;

    @Autowired
    public GitHubIntegrationResource(GitHubService githubService,
                                      JiraService jiraService,
                                      JiraAuthenticationContext authenticationContext) {
        this.githubService = githubService;
        this.jiraService = jiraService;
        this.authenticationContext = authenticationContext;
    }

    /**
     * Create a branch from a Jira issue
     * POST /rest/github-integration/1.0/branch/create
     */
    @POST
    @Path("/branch/create")
    public Response createBranch(CreateBranchRequest request) {
        try {
            // Validate user permissions
            ApplicationUser user = authenticationContext.getLoggedInUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(errorResponse("User not authenticated"))
                        .build();
            }

            if (!jiraService.hasViewPermission(user, request.getIssueKey())) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(errorResponse("Permission denied"))
                        .build();
            }

            // Validate request
            if (request.getIssueKey() == null || request.getIssueKey().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorResponse("Issue key is required"))
                        .build();
            }

            if (request.getBranchName() == null || request.getBranchName().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorResponse("Branch name is required"))
                        .build();
            }

            // Create branch
            Map<String, Object> result = githubService.createBranch(
                    request.getIssueKey(),
                    request.getBaseBranch(),
                    request.getBranchName()
            );

            return Response.ok(result).build();

        } catch (GitHubException e) {
            log.error("GitHub error creating branch", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error("Error creating branch", e);
            return Response.serverError()
                    .entity(errorResponse("Internal server error: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Create a pull request from a Jira issue
     * POST /rest/github-integration/1.0/pr/create
     */
    @POST
    @Path("/pr/create")
    public Response createPullRequest(CreatePRRequest request) {
        try {
            // Validate user permissions
            ApplicationUser user = authenticationContext.getLoggedInUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(errorResponse("User not authenticated"))
                        .build();
            }

            if (!jiraService.hasViewPermission(user, request.getIssueKey())) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(errorResponse("Permission denied"))
                        .build();
            }

            // Validate request
            if (request.getIssueKey() == null || request.getIssueKey().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorResponse("Issue key is required"))
                        .build();
            }

            if (request.getSourceBranch() == null || request.getSourceBranch().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorResponse("Source branch is required"))
                        .build();
            }

            if (request.getTitle() == null || request.getTitle().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorResponse("Title is required"))
                        .build();
            }

            // Create pull request
            Map<String, Object> result = githubService.createPullRequest(
                    request.getIssueKey(),
                    request.getSourceBranch(),
                    request.getTargetBranch(),
                    request.getTitle(),
                    request.getDescription()
            );

            return Response.ok(result).build();

        } catch (GitHubException e) {
            log.error("GitHub error creating PR", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error("Error creating PR", e);
            return Response.serverError()
                    .entity(errorResponse("Internal server error: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get GitHub information for an issue
     * GET /rest/github-integration/1.0/issue/{issueKey}/github-info
     */
    @GET
    @Path("/issue/{issueKey}/github-info")
    public Response getGitHubInfo(@PathParam("issueKey") String issueKey) {
        try {
            // Validate user permissions
            ApplicationUser user = authenticationContext.getLoggedInUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(errorResponse("User not authenticated"))
                        .build();
            }

            if (!jiraService.hasViewPermission(user, issueKey)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(errorResponse("Permission denied"))
                        .build();
            }

            // For now, return placeholder data
            // In a full implementation, this would query GitHub for branches and PRs
            // associated with this issue (by parsing remote links or searching GitHub)
            Map<String, Object> result = new HashMap<>();
            result.put("issueKey", issueKey);
            result.put("branches", new java.util.ArrayList<>());
            result.put("pullRequests", new java.util.ArrayList<>());

            return Response.ok(result).build();

        } catch (Exception e) {
            log.error("Error getting GitHub info for issue " + issueKey, e);
            return Response.serverError()
                    .entity(errorResponse("Internal server error: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Health check endpoint
     * GET /rest/github-integration/1.0/health
     */
    @GET
    @Path("/health")
    public Response healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("version", "1.0.0");
        return Response.ok(response).build();
    }

    /**
     * Helper method to create error response
     */
    private Map<String, String> errorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }

    // Request DTOs

    public static class CreateBranchRequest {
        private String issueKey;
        private String baseBranch;
        private String branchName;

        public String getIssueKey() { return issueKey; }
        public void setIssueKey(String issueKey) { this.issueKey = issueKey; }

        public String getBaseBranch() { return baseBranch; }
        public void setBaseBranch(String baseBranch) { this.baseBranch = baseBranch; }

        public String getBranchName() { return branchName; }
        public void setBranchName(String branchName) { this.branchName = branchName; }
    }

    public static class CreatePRRequest {
        private String issueKey;
        private String sourceBranch;
        private String targetBranch;
        private String title;
        private String description;

        public String getIssueKey() { return issueKey; }
        public void setIssueKey(String issueKey) { this.issueKey = issueKey; }

        public String getSourceBranch() { return sourceBranch; }
        public void setSourceBranch(String sourceBranch) { this.sourceBranch = sourceBranch; }

        public String getTargetBranch() { return targetBranch; }
        public void setTargetBranch(String targetBranch) { this.targetBranch = targetBranch; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
