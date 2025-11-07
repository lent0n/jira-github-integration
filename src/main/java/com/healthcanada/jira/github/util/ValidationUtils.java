package com.healthcanada.jira.github.util;

import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Utility class for input validation and sanitization
 */
public class ValidationUtils {

    // Branch name must contain only alphanumeric, hyphens, underscores, and forward slashes
    private static final Pattern VALID_BRANCH_NAME = Pattern.compile("^[a-zA-Z0-9/_-]+$");

    // Issue key format (e.g., PROJ-123)
    private static final Pattern VALID_ISSUE_KEY = Pattern.compile("^[A-Z][A-Z0-9]+-[0-9]+$");

    // Repository owner and name (GitHub username/org and repo name)
    private static final Pattern VALID_REPO_NAME = Pattern.compile("^[a-zA-Z0-9._-]+$");

    // Maximum lengths to prevent abuse
    private static final int MAX_BRANCH_NAME_LENGTH = 255;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 10000;
    private static final int MAX_URL_LENGTH = 2048;

    private ValidationUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Validate and sanitize branch name
     */
    public static String validateBranchName(String branchName) throws IllegalArgumentException {
        if (StringUtils.isBlank(branchName)) {
            throw new IllegalArgumentException("Branch name cannot be empty");
        }

        String sanitized = branchName.trim();

        if (sanitized.length() > MAX_BRANCH_NAME_LENGTH) {
            throw new IllegalArgumentException("Branch name exceeds maximum length of " + MAX_BRANCH_NAME_LENGTH);
        }

        if (!VALID_BRANCH_NAME.matcher(sanitized).matches()) {
            throw new IllegalArgumentException(
                    "Branch name contains invalid characters. Only alphanumeric, hyphens, underscores, and forward slashes are allowed");
        }

        // Prevent path traversal
        if (sanitized.contains("..")) {
            throw new IllegalArgumentException("Branch name cannot contain '..'");
        }

        return sanitized;
    }

    /**
     * Validate issue key format
     */
    public static String validateIssueKey(String issueKey) throws IllegalArgumentException {
        if (StringUtils.isBlank(issueKey)) {
            throw new IllegalArgumentException("Issue key cannot be empty");
        }

        String sanitized = issueKey.trim().toUpperCase();

        if (!VALID_ISSUE_KEY.matcher(sanitized).matches()) {
            throw new IllegalArgumentException(
                    "Invalid issue key format. Expected format: PROJECT-123");
        }

        return sanitized;
    }

    /**
     * Validate repository owner or name
     */
    public static String validateRepoName(String repoName, String fieldName) throws IllegalArgumentException {
        if (StringUtils.isBlank(repoName)) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }

        String sanitized = repoName.trim();

        if (!VALID_REPO_NAME.matcher(sanitized).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " contains invalid characters. Only alphanumeric, dots, hyphens, and underscores are allowed");
        }

        return sanitized;
    }

    /**
     * Validate and sanitize URL
     */
    public static String validateUrl(String url, String fieldName) throws IllegalArgumentException {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }

        String sanitized = url.trim();

        if (sanitized.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException(fieldName + " exceeds maximum length of " + MAX_URL_LENGTH);
        }

        try {
            URL parsedUrl = new URL(sanitized);

            // Only allow HTTP and HTTPS
            String protocol = parsedUrl.getProtocol().toLowerCase();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                throw new IllegalArgumentException(fieldName + " must use HTTP or HTTPS protocol");
            }

            // Validate host is not empty
            if (StringUtils.isBlank(parsedUrl.getHost())) {
                throw new IllegalArgumentException(fieldName + " must have a valid host");
            }

            return sanitized;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(fieldName + " is not a valid URL: " + e.getMessage());
        }
    }

    /**
     * Validate and sanitize title
     */
    public static String validateTitle(String title) throws IllegalArgumentException {
        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("Title cannot be empty");
        }

        String sanitized = title.trim();

        if (sanitized.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title exceeds maximum length of " + MAX_TITLE_LENGTH);
        }

        // Remove control characters
        sanitized = sanitized.replaceAll("\\p{Cntrl}", "");

        return sanitized;
    }

    /**
     * Validate and sanitize description
     */
    public static String validateDescription(String description) throws IllegalArgumentException {
        if (description == null) {
            return "";
        }

        String sanitized = description.trim();

        if (sanitized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description exceeds maximum length of " + MAX_DESCRIPTION_LENGTH);
        }

        // Allow markdown but remove potentially dangerous control characters
        sanitized = sanitized.replaceAll("\\p{C}", "");

        return sanitized;
    }

    /**
     * Validate GitHub Personal Access Token format
     */
    public static String validateToken(String token) throws IllegalArgumentException {
        if (StringUtils.isBlank(token)) {
            throw new IllegalArgumentException("Token cannot be empty");
        }

        String sanitized = token.trim();

        // GitHub tokens have specific prefixes
        if (!sanitized.startsWith("ghp_") &&
            !sanitized.startsWith("gho_") &&
            !sanitized.startsWith("ghu_") &&
            !sanitized.startsWith("ghs_") &&
            !sanitized.startsWith("ghr_") &&
            !sanitized.startsWith("github_pat_")) {
            throw new IllegalArgumentException(
                    "Token does not appear to be a valid GitHub Personal Access Token");
        }

        if (sanitized.length() < 20 || sanitized.length() > 255) {
            throw new IllegalArgumentException("Token length is invalid");
        }

        return sanitized;
    }

    /**
     * Check if string is null or empty
     */
    public static boolean isEmpty(String value) {
        return StringUtils.isBlank(value);
    }

    /**
     * Safe null-to-empty string conversion
     */
    public static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
