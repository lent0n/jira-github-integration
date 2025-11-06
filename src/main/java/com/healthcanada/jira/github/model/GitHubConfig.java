package com.healthcanada.jira.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration model for GitHub Enterprise integration
 */
public class GitHubConfig {

    @JsonProperty("githubEnterpriseUrl")
    private String githubEnterpriseUrl;

    @JsonProperty("githubApiUrl")
    private String githubApiUrl;

    @JsonProperty("githubToken")
    private String githubToken;

    @JsonProperty("trustCustomCertificates")
    private boolean trustCustomCertificates;

    @JsonProperty("repositories")
    private List<RepositoryMapping> repositories;

    @JsonProperty("branchNaming")
    private String branchNaming;

    @JsonProperty("transitionMappings")
    private Map<String, String> transitionMappings;

    @JsonProperty("webhookSecret")
    private String webhookSecret;

    @JsonProperty("webhookUrl")
    private String webhookUrl;

    @JsonProperty("webhookIds")
    private Map<String, String> webhookIds;

    public GitHubConfig() {
        this.repositories = new ArrayList<>();
        this.transitionMappings = new HashMap<>();
        this.webhookIds = new HashMap<>();
        this.branchNaming = "feature/{issueKey}-{summary}";
        this.trustCustomCertificates = false;
    }

    // Getters and Setters

    public String getGithubEnterpriseUrl() {
        return githubEnterpriseUrl;
    }

    public void setGithubEnterpriseUrl(String githubEnterpriseUrl) {
        this.githubEnterpriseUrl = githubEnterpriseUrl;
        // Auto-set API URL if not explicitly set
        if (githubEnterpriseUrl != null && !githubEnterpriseUrl.isEmpty()) {
            this.githubApiUrl = githubEnterpriseUrl.endsWith("/")
                    ? githubEnterpriseUrl + "api/v3"
                    : githubEnterpriseUrl + "/api/v3";
        }
    }

    public String getGithubApiUrl() {
        return githubApiUrl;
    }

    public void setGithubApiUrl(String githubApiUrl) {
        this.githubApiUrl = githubApiUrl;
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

    public List<RepositoryMapping> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<RepositoryMapping> repositories) {
        this.repositories = repositories;
    }

    public String getBranchNaming() {
        return branchNaming;
    }

    public void setBranchNaming(String branchNaming) {
        this.branchNaming = branchNaming;
    }

    public Map<String, String> getTransitionMappings() {
        return transitionMappings;
    }

    public void setTransitionMappings(Map<String, String> transitionMappings) {
        this.transitionMappings = transitionMappings;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public Map<String, String> getWebhookIds() {
        return webhookIds;
    }

    public void setWebhookIds(Map<String, String> webhookIds) {
        this.webhookIds = webhookIds;
    }

    /**
     * Find repository mapping for a given Jira project
     */
    public RepositoryMapping getRepositoryMapping(String jiraProjectKey) {
        if (repositories == null) {
            return null;
        }
        return repositories.stream()
                .filter(mapping -> mapping.getJiraProject().equals(jiraProjectKey))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if configuration is valid
     */
    public boolean isValid() {
        return githubEnterpriseUrl != null && !githubEnterpriseUrl.isEmpty()
                && githubToken != null && !githubToken.isEmpty()
                && repositories != null && !repositories.isEmpty();
    }
}
