package com.healthcanada.jira.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mapping between Jira project and GitHub repository
 */
public class RepositoryMapping {

    @JsonProperty("jiraProject")
    private String jiraProject;

    @JsonProperty("githubOwner")
    private String githubOwner;

    @JsonProperty("githubRepo")
    private String githubRepo;

    @JsonProperty("defaultBranch")
    private String defaultBranch;

    @JsonProperty("branchNamingTemplate")
    private String branchNamingTemplate;

    public RepositoryMapping() {
        this.defaultBranch = "main";
        this.branchNamingTemplate = "feature/{issueKey}-{summary}";
    }

    public RepositoryMapping(String jiraProject, String githubOwner, String githubRepo) {
        this();
        this.jiraProject = jiraProject;
        this.githubOwner = githubOwner;
        this.githubRepo = githubRepo;
    }

    // Getters and Setters

    public String getJiraProject() {
        return jiraProject;
    }

    public void setJiraProject(String jiraProject) {
        this.jiraProject = jiraProject;
    }

    public String getGithubOwner() {
        return githubOwner;
    }

    public void setGithubOwner(String githubOwner) {
        this.githubOwner = githubOwner;
    }

    public String getGithubRepo() {
        return githubRepo;
    }

    public void setGithubRepo(String githubRepo) {
        this.githubRepo = githubRepo;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public String getBranchNamingTemplate() {
        return branchNamingTemplate;
    }

    public void setBranchNamingTemplate(String branchNamingTemplate) {
        this.branchNamingTemplate = branchNamingTemplate;
    }

    /**
     * Get full repository name (owner/repo)
     */
    public String getFullRepoName() {
        return githubOwner + "/" + githubRepo;
    }

    @Override
    public String toString() {
        return "RepositoryMapping{" +
                "jiraProject='" + jiraProject + '\'' +
                ", githubOwner='" + githubOwner + '\'' +
                ", githubRepo='" + githubRepo + '\'' +
                ", defaultBranch='" + defaultBranch + '\'' +
                '}';
    }
}
