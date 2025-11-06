package com.healthcanada.jira.github.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub webhook payload model
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookPayload {

    @JsonProperty("action")
    private String action;

    @JsonProperty("pull_request")
    private PullRequest pullRequest;

    @JsonProperty("repository")
    private Repository repository;

    @JsonProperty("sender")
    private User sender;

    // Getters and Setters

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    /**
     * Pull Request information from webhook
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequest {
        @JsonProperty("number")
        private int number;

        @JsonProperty("title")
        private String title;

        @JsonProperty("html_url")
        private String htmlUrl;

        @JsonProperty("state")
        private String state;

        @JsonProperty("merged")
        private boolean merged;

        @JsonProperty("head")
        private Branch head;

        @JsonProperty("base")
        private Branch base;

        @JsonProperty("user")
        private User user;

        @JsonProperty("merged_by")
        private User mergedBy;

        @JsonProperty("merged_at")
        private String mergedAt;

        @JsonProperty("closed_by")
        private User closedBy;

        @JsonProperty("closed_at")
        private String closedAt;

        // Getters and Setters

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getHtmlUrl() {
            return htmlUrl;
        }

        public void setHtmlUrl(String htmlUrl) {
            this.htmlUrl = htmlUrl;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public boolean isMerged() {
            return merged;
        }

        public void setMerged(boolean merged) {
            this.merged = merged;
        }

        public Branch getHead() {
            return head;
        }

        public void setHead(Branch head) {
            this.head = head;
        }

        public Branch getBase() {
            return base;
        }

        public void setBase(Branch base) {
            this.base = base;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public User getMergedBy() {
            return mergedBy;
        }

        public void setMergedBy(User mergedBy) {
            this.mergedBy = mergedBy;
        }

        public String getMergedAt() {
            return mergedAt;
        }

        public void setMergedAt(String mergedAt) {
            this.mergedAt = mergedAt;
        }

        public User getClosedBy() {
            return closedBy;
        }

        public void setClosedBy(User closedBy) {
            this.closedBy = closedBy;
        }

        public String getClosedAt() {
            return closedAt;
        }

        public void setClosedAt(String closedAt) {
            this.closedAt = closedAt;
        }
    }

    /**
     * Branch information
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Branch {
        @JsonProperty("ref")
        private String ref;

        @JsonProperty("sha")
        private String sha;

        @JsonProperty("repo")
        private Repository repo;

        public String getRef() {
            return ref;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }

        public String getSha() {
            return sha;
        }

        public void setSha(String sha) {
            this.sha = sha;
        }

        public Repository getRepo() {
            return repo;
        }

        public void setRepo(Repository repo) {
            this.repo = repo;
        }
    }

    /**
     * Repository information
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Repository {
        @JsonProperty("name")
        private String name;

        @JsonProperty("full_name")
        private String fullName;

        @JsonProperty("owner")
        private User owner;

        @JsonProperty("html_url")
        private String htmlUrl;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public User getOwner() {
            return owner;
        }

        public void setOwner(User owner) {
            this.owner = owner;
        }

        public String getHtmlUrl() {
            return htmlUrl;
        }

        public void setHtmlUrl(String htmlUrl) {
            this.htmlUrl = htmlUrl;
        }
    }

    /**
     * User information
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        @JsonProperty("login")
        private String login;

        @JsonProperty("html_url")
        private String htmlUrl;

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getHtmlUrl() {
            return htmlUrl;
        }

        public void setHtmlUrl(String htmlUrl) {
            this.htmlUrl = htmlUrl;
        }
    }
}
