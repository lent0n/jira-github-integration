package com.healthcanada.jira.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcanada.jira.github.model.GitHubException;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for GitHub Enterprise API with SSL support and connection pooling
 */
@Component
public class GitHubEnterpriseClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubEnterpriseClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Connection pool settings
    private static final int MAX_CONNECTIONS = 20;
    private static final int MAX_PER_ROUTE = 10;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int SOCKET_TIMEOUT = 30000;

    // Retry settings
    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 1000;
    private static final int MAX_RETRY_DELAY_MS = 10000;

    private String baseUrl;
    private String apiToken;
    private CloseableHttpClient httpClient;
    private boolean trustCustomCertificates;

    public GitHubEnterpriseClient() {
        // Default constructor for Spring
    }

    /**
     * Initialize client with configuration
     */
    public void initialize(String baseUrl, String apiToken, boolean trustCustomCertificates) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiToken = apiToken;
        this.trustCustomCertificates = trustCustomCertificates;
        this.httpClient = createHttpClient();
        log.info("GitHub Enterprise client initialized for: {}", this.baseUrl);
    }

    /**
     * Create HTTP client with SSL support and connection pooling
     */
    private CloseableHttpClient createHttpClient() {
        try {
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            cm.setMaxTotal(MAX_CONNECTIONS);
            cm.setDefaultMaxPerRoute(MAX_PER_ROUTE);

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(CONNECT_TIMEOUT)
                    .setSocketTimeout(SOCKET_TIMEOUT)
                    .setConnectionRequestTimeout(CONNECT_TIMEOUT)
                    .build();

            if (trustCustomCertificates) {
                log.info("Configuring HTTP client to trust custom certificates");
                SSLContext sslContext = SSLContextBuilder.create()
                        .loadTrustMaterial(new TrustSelfSignedStrategy())
                        .build();

                SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                        sslContext,
                        new String[]{"TLSv1.2", "TLSv1.3"},
                        null,
                        NoopHostnameVerifier.INSTANCE
                );

                return HttpClients.custom()
                        .setConnectionManager(cm)
                        .setDefaultRequestConfig(requestConfig)
                        .setSSLSocketFactory(sslSocketFactory)
                        .build();
            } else {
                return HttpClients.custom()
                        .setConnectionManager(cm)
                        .setDefaultRequestConfig(requestConfig)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to create HTTP client", e);
            // Fall back to basic client
            return HttpClients.createDefault();
        }
    }

    /**
     * Test connection to GitHub Enterprise
     */
    public boolean testConnection() throws IOException {
        try {
            String url = baseUrl + "/api/v3/user";
            HttpGet request = new HttpGet(url);
            addAuthHeaders(request);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    log.info("Successfully connected to GitHub Enterprise");
                    return true;
                } else {
                    log.warn("GitHub Enterprise connection test failed with status: {}", statusCode);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Failed to test GitHub Enterprise connection", e);
            throw new IOException("Connection test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get base branch SHA
     */
    public String getBranchSha(String owner, String repo, String branch) throws GitHubException {
        try {
            String url = String.format("%s/api/v3/repos/%s/%s/git/refs/heads/%s",
                    baseUrl, owner, repo, branch);

            JsonNode response = executeGet(url);
            return response.get("object").get("sha").asText();
        } catch (Exception e) {
            throw new GitHubException("Failed to get branch SHA: " + e.getMessage(), e);
        }
    }

    /**
     * Create a new branch
     */
    public Map<String, Object> createBranch(String owner, String repo, String branchName, String baseSha)
            throws GitHubException {
        try {
            String url = String.format("%s/api/v3/repos/%s/%s/git/refs", baseUrl, owner, repo);

            Map<String, Object> body = new HashMap<>();
            body.put("ref", "refs/heads/" + branchName);
            body.put("sha", baseSha);

            JsonNode response = executePost(url, body);

            Map<String, Object> result = new HashMap<>();
            result.put("ref", response.get("ref").asText());
            result.put("sha", response.get("object").get("sha").asText());
            result.put("url", response.get("url").asText());

            log.info("Created branch {} in {}/{}", branchName, owner, repo);
            return result;
        } catch (Exception e) {
            throw new GitHubException("Failed to create branch: " + e.getMessage(), e);
        }
    }

    /**
     * Create a pull request
     */
    public Map<String, Object> createPullRequest(String owner, String repo, String title,
                                                   String head, String base, String body) throws GitHubException {
        try {
            String url = String.format("%s/api/v3/repos/%s/%s/pulls", baseUrl, owner, repo);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("title", title);
            requestBody.put("head", head);
            requestBody.put("base", base);
            requestBody.put("body", body);

            JsonNode response = executePost(url, requestBody);

            Map<String, Object> result = new HashMap<>();
            result.put("number", response.get("number").asInt());
            result.put("title", response.get("title").asText());
            result.put("url", response.get("html_url").asText());
            result.put("state", response.get("state").asText());

            log.info("Created pull request #{} in {}/{}", result.get("number"), owner, repo);
            return result;
        } catch (Exception e) {
            throw new GitHubException("Failed to create pull request: " + e.getMessage(), e);
        }
    }

    /**
     * Get pull request by number
     */
    public Map<String, Object> getPullRequest(String owner, String repo, int prNumber) throws GitHubException {
        try {
            String url = String.format("%s/api/v3/repos/%s/%s/pulls/%d", baseUrl, owner, repo, prNumber);

            JsonNode response = executeGet(url);

            Map<String, Object> result = new HashMap<>();
            result.put("number", response.get("number").asInt());
            result.put("title", response.get("title").asText());
            result.put("url", response.get("html_url").asText());
            result.put("state", response.get("state").asText());
            result.put("merged", response.has("merged") && response.get("merged").asBoolean());

            return result;
        } catch (Exception e) {
            throw new GitHubException("Failed to get pull request: " + e.getMessage(), e);
        }
    }

    /**
     * List branches for a repository
     */
    public List<String> listBranches(String owner, String repo) throws GitHubException {
        try {
            String url = String.format("%s/api/v3/repos/%s/%s/branches", baseUrl, owner, repo);

            JsonNode response = executeGet(url);
            List<String> branches = new ArrayList<>();

            if (response.isArray()) {
                for (JsonNode branch : response) {
                    branches.add(branch.get("name").asText());
                }
            }

            return branches;
        } catch (Exception e) {
            throw new GitHubException("Failed to list branches: " + e.getMessage(), e);
        }
    }

    /**
     * Register webhook for repository
     */
    public String registerWebhook(String owner, String repo, String webhookUrl, String secret)
            throws GitHubException {
        try {
            String url = String.format("%s/api/v3/repos/%s/%s/hooks", baseUrl, owner, repo);

            Map<String, Object> config = new HashMap<>();
            config.put("url", webhookUrl);
            config.put("content_type", "json");
            config.put("secret", secret);

            Map<String, Object> body = new HashMap<>();
            body.put("name", "web");
            body.put("active", true);
            body.put("events", new String[]{"pull_request", "push"});
            body.put("config", config);

            JsonNode response = executePost(url, body);

            String webhookId = response.get("id").asText();
            log.info("Registered webhook {} for {}/{}", webhookId, owner, repo);
            return webhookId;
        } catch (Exception e) {
            throw new GitHubException("Failed to register webhook: " + e.getMessage(), e);
        }
    }

    /**
     * Execute GET request with retry logic
     */
    private JsonNode executeGet(String url) throws GitHubException, IOException {
        return executeWithRetry(() -> {
            HttpGet request = new HttpGet(url);
            addAuthHeaders(request);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return handleResponse(response);
            }
        }, "GET " + url);
    }

    /**
     * Execute POST request with retry logic
     */
    private JsonNode executePost(String url, Map<String, Object> body) throws GitHubException, IOException {
        return executeWithRetry(() -> {
            HttpPost request = new HttpPost(url);
            addAuthHeaders(request);
            request.setHeader("Content-Type", "application/json");

            String jsonBody = objectMapper.writeValueAsString(body);
            request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return handleResponse(response);
            }
        }, "POST " + url);
    }

    /**
     * Handle HTTP response and check for errors
     */
    private JsonNode handleResponse(HttpResponse response) throws GitHubException, IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

        // Check rate limit
        if (response.containsHeader("X-RateLimit-Remaining")) {
            int remaining = Integer.parseInt(response.getFirstHeader("X-RateLimit-Remaining").getValue());
            if (remaining < 100) {
                log.warn("GitHub API rate limit low: {} requests remaining", remaining);
            }
        }

        if (statusCode >= 200 && statusCode < 300) {
            return objectMapper.readTree(responseBody);
        } else {
            String errorMessage = String.format("GitHub API error: %d - %s",
                    statusCode, response.getStatusLine().getReasonPhrase());
            throw new GitHubException(errorMessage, statusCode, responseBody);
        }
    }

    /**
     * Add authentication headers to request
     */
    private void addAuthHeaders(HttpRequestBase request) {
        request.setHeader("Authorization", "Bearer " + apiToken);
        request.setHeader("Accept", "application/vnd.github.v3+json");
    }

    /**
     * Execute operation with exponential backoff retry logic
     */
    private <T> T executeWithRetry(RetryableOperation<T> operation, String operationName)
            throws GitHubException, IOException {
        int attempt = 0;
        GitHubException lastException = null;
        IOException lastIOException = null;

        while (attempt < MAX_RETRIES) {
            try {
                return operation.execute();
            } catch (GitHubException e) {
                lastException = e;
                // Don't retry on client errors (4xx), only server errors (5xx) and network issues
                if (e.getStatusCode() > 0 && e.getStatusCode() < 500) {
                    throw e;
                }
                attempt++;
                if (attempt < MAX_RETRIES) {
                    int delay = calculateRetryDelay(attempt);
                    log.warn("GitHub API {} failed (attempt {}/{}): {}. Retrying in {}ms",
                            operationName, attempt, MAX_RETRIES, e.getMessage(), delay);
                    sleep(delay);
                }
            } catch (IOException e) {
                lastIOException = e;
                attempt++;
                if (attempt < MAX_RETRIES) {
                    int delay = calculateRetryDelay(attempt);
                    log.warn("Network error for {} (attempt {}/{}): {}. Retrying in {}ms",
                            operationName, attempt, MAX_RETRIES, e.getMessage(), delay);
                    sleep(delay);
                }
            }
        }

        // All retries exhausted
        if (lastException != null) {
            log.error("GitHub API {} failed after {} attempts", operationName, MAX_RETRIES);
            throw lastException;
        } else {
            log.error("Network operation {} failed after {} attempts", operationName, MAX_RETRIES);
            throw lastIOException;
        }
    }

    /**
     * Calculate exponential backoff delay
     */
    private int calculateRetryDelay(int attempt) {
        int delay = INITIAL_RETRY_DELAY_MS * (int) Math.pow(2, attempt - 1);
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }

    /**
     * Sleep for specified milliseconds
     */
    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry sleep interrupted", e);
        }
    }

    /**
     * Functional interface for retryable operations
     */
    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws GitHubException, IOException;
    }

    /**
     * Close HTTP client
     */
    public void close() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                log.error("Failed to close HTTP client", e);
            }
        }
    }
}
