package com.healthcanada.jira.github.model;

/**
 * Exception for GitHub API errors
 */
public class GitHubException extends Exception {

    private int statusCode;
    private String responseBody;

    public GitHubException(String message) {
        super(message);
    }

    public GitHubException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitHubException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    @Override
    public String toString() {
        if (statusCode > 0) {
            return "GitHubException{" +
                    "message='" + getMessage() + '\'' +
                    ", statusCode=" + statusCode +
                    ", responseBody='" + responseBody + '\'' +
                    '}';
        }
        return super.toString();
    }
}
