# GitHub Enterprise Integration for Jira

A two-way integration plugin for Jira Data Center and GitHub Enterprise.

## Features

- **Branch Management**: Create GitHub branches directly from Jira issues
- **Pull Request Creation**: Create and link PRs to Jira issues
- **Webhook Support**: Automatic synchronization when PR events occur
- **Issue Transitions**: Auto-transition Jira issues based on PR status
- **Repository Mapping**: Map Jira projects to GitHub repositories
- **Admin Configuration UI**: Web-based configuration interface
- **Issue Panel**: GitHub information displayed on Jira issue view

## Compatibility

- **Jira Data Center**: 9.12+ and 10.3+
- **GitHub Enterprise**: Server and Cloud editions

## Building

Requires Atlassian Plugin SDK 9.1.1 and Java 8+:

```bash
mvn clean package
```

The JAR will be created at `target/github-integration-1.0.0-SNAPSHOT.jar`

## Installation

1. Log into Jira as an administrator
2. Navigate to **Settings → Apps → Manage apps**
3. Click **Upload app**
4. Select the JAR file and click **Upload**

## Configuration

After installation:

1. Go to **Settings → System → GitHub Integration**
2. Enter your GitHub Enterprise URL
3. Provide a Personal Access Token with `repo` and `admin:repo_hook` permissions
4. Configure repository mappings (Jira project → GitHub owner/repo)
5. Generate and configure webhook secret
6. Register webhooks for each repository

## Architecture

### Components

- **GitHubService**: GitHub API client and business logic
- **JiraService**: Jira API integration
- **SyncService**: Two-way synchronization orchestration
- **ConfigurationResource**: REST API for configuration
- **BranchResource**: REST API for branch operations
- **PullRequestResource**: REST API for PR operations
- **GitHubWebhookListener**: Webhook receiver and validator
- **ConfigurationServlet**: Admin configuration page
- **GitHubPanelContextProvider**: Issue view panel context

### Security

- **Token Encryption**: AES-256 encryption for stored tokens
- **Webhook Validation**: HMAC-SHA256 signature verification
- **Permission Checks**: Admin-only configuration access

## Known Limitations

Some Jira API features are disabled due to compatibility with Jira 9.12:
- Issue commenting (logs debug message instead)
- Issue transitions (logs debug message instead)
- Remote link creation (logs debug message instead)

These can be re-enabled when building against newer Jira APIs.

## License

Copyright (c) 2025 Health Canada
