/**
 * GitHub Integration Panel JavaScript
 * Compatible with Jira 9.12+ and 10.3+
 * Uses AUI (Atlassian User Interface) framework
 */
(function($) {
    'use strict';

    // Initialize when DOM is ready
    $(document).ready(function() {
        initializeGitHubPanel();
    });

    function initializeGitHubPanel() {
        // Get issue key and REST URL from hidden inputs
        var issueKey = $('#github-issue-key').val();
        var restUrl = $('#github-rest-url').val();

        if (!issueKey || !restUrl) {
            console.log('GitHub Integration: No issue key or REST URL found');
            return;
        }

        console.log('GitHub Integration: Initializing for issue ' + issueKey);

        // Load GitHub info
        loadGitHubInfo(issueKey, restUrl);

        // Initialize button handlers
        initializeCreateBranchButton(issueKey, restUrl);
        initializeCreatePRButton(issueKey, restUrl);
    }

    /**
     * Load GitHub information for the issue
     */
    function loadGitHubInfo(issueKey, restUrl) {
        $.ajax({
            url: restUrl + '/issue/' + issueKey + '/github-info',
            type: 'GET',
            success: function(data) {
                displayGitHubInfo(data);
            },
            error: function(xhr) {
                console.error('Failed to load GitHub info:', xhr);
                displayGitHubError('Failed to load GitHub information');
            }
        });
    }

    /**
     * Display GitHub information in the panel
     */
    function displayGitHubInfo(data) {
        var container = $('#github-info-container');
        var html = '';

        var hasBranches = data.branches && data.branches.length > 0;
        var hasPRs = data.pullRequests && data.pullRequests.length > 0;

        if (!hasBranches && !hasPRs) {
            html = '<div class="aui-message aui-message-generic">' +
                   '<p class="title"><span class="aui-icon icon-generic"></span>' +
                   'No GitHub activity for this issue yet.</p>' +
                   '<p>Create a branch to get started!</p></div>';
        } else {
            // Display branches
            if (hasBranches) {
                html += '<h4>Branches</h4><ul class="github-branches">';
                data.branches.forEach(function(branch) {
                    html += '<li><a href="' + branch.url + '" target="_blank">' +
                           branch.name + '</a></li>';
                });
                html += '</ul>';
            }

            // Display pull requests
            if (hasPRs) {
                html += '<h4>Pull Requests</h4><ul class="github-prs">';
                data.pullRequests.forEach(function(pr) {
                    var statusClass = 'status-' + pr.state;
                    html += '<li><a href="' + pr.url + '" target="_blank">' +
                           '#' + pr.number + ': ' + pr.title + '</a> ' +
                           '<span class="aui-lozenge ' + statusClass + '">' +
                           pr.state + '</span></li>';
                });
                html += '</ul>';
            }
        }

        container.html(html);
    }

    /**
     * Display error message
     */
    function displayGitHubError(message) {
        var container = $('#github-info-container');
        container.html('<div class="aui-message aui-message-error">' +
                      '<p class="title"><span class="aui-icon icon-error"></span>' +
                      message + '</p></div>');
    }

    /**
     * Initialize Create Branch button and dialog
     */
    function initializeCreateBranchButton(issueKey, restUrl) {
        var dialog = $('#github-create-branch-dialog');

        // Show dialog when button clicked
        $('#github-create-branch-btn').on('click', function() {
            AJS.dialog2('#github-create-branch-dialog').show();
        });

        // Auto-fill button
        $('#github-branch-autofill').on('click', function() {
            var summary = $('#github-issue-summary').val();
            var sanitized = sanitizeBranchName(summary);
            var branchName = 'feature/' + issueKey + '-' + sanitized;
            $('#github-branch-name').val(branchName);
        });

        // Cancel button
        $('#github-branch-cancel, #github-create-branch-dialog .aui-dialog2-header-close').on('click', function() {
            AJS.dialog2('#github-create-branch-dialog').hide();
            clearBranchForm();
        });

        // Create button
        $('#github-branch-create').on('click', function() {
            createBranch(issueKey, restUrl);
        });

        // Enter key submits form
        $('#github-branch-form').on('keypress', function(e) {
            if (e.which === 13) {
                e.preventDefault();
                createBranch(issueKey, restUrl);
            }
        });
    }

    /**
     * Create branch via REST API
     */
    function createBranch(issueKey, restUrl) {
        var baseBranch = $('#github-base-branch').val();
        var branchName = $('#github-branch-name').val();
        var statusDiv = $('#github-branch-status');

        // Validate
        if (!branchName) {
            statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-error">Error</span> Branch name is required');
            return;
        }

        // Disable button and show loading
        var createBtn = $('#github-branch-create');
        createBtn.attr('disabled', 'disabled');
        createBtn.attr('aria-disabled', 'true');
        statusDiv.html('<span class="aui-icon aui-icon-wait">Creating branch...</span>');

        // Make API call
        $.ajax({
            url: restUrl + '/branch/create',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                issueKey: issueKey,
                baseBranch: baseBranch,
                branchName: branchName
            }),
            success: function(response) {
                // Show success flag
                AJS.flag({
                    type: 'success',
                    title: 'Branch Created',
                    body: 'Branch <strong>' + branchName + '</strong> created successfully!',
                    close: 'auto'
                });

                // Close dialog and refresh
                AJS.dialog2('#github-create-branch-dialog').hide();
                clearBranchForm();
                loadGitHubInfo(issueKey, restUrl);
            },
            error: function(xhr) {
                var errorMsg = 'Failed to create branch';
                if (xhr.responseJSON && xhr.responseJSON.error) {
                    errorMsg = xhr.responseJSON.error;
                }

                // Show error flag
                AJS.flag({
                    type: 'error',
                    title: 'Error',
                    body: errorMsg,
                    close: 'auto'
                });

                statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-error">Error</span> ' + errorMsg);
            },
            complete: function() {
                createBtn.removeAttr('disabled');
                createBtn.removeAttr('aria-disabled');
            }
        });
    }

    /**
     * Clear branch form
     */
    function clearBranchForm() {
        $('#github-branch-name').val('');
        $('#github-base-branch').val('main');
        $('#github-branch-status').html('');
    }

    /**
     * Initialize Create PR button and dialog
     */
    function initializeCreatePRButton(issueKey, restUrl) {
        // Show dialog when button clicked
        $('#github-create-pr-btn').on('click', function() {
            AJS.dialog2('#github-create-pr-dialog').show();
        });

        // Cancel button
        $('#github-pr-cancel, #github-create-pr-dialog .aui-dialog2-header-close').on('click', function() {
            AJS.dialog2('#github-create-pr-dialog').hide();
            clearPRForm();
        });

        // Create button
        $('#github-pr-create').on('click', function() {
            createPullRequest(issueKey, restUrl);
        });
    }

    /**
     * Create pull request via REST API
     */
    function createPullRequest(issueKey, restUrl) {
        var sourceBranch = $('#github-source-branch').val();
        var targetBranch = $('#github-target-branch').val();
        var title = $('#github-pr-title').val();
        var description = $('#github-pr-description').val();
        var statusDiv = $('#github-pr-status');

        // Validate
        if (!sourceBranch) {
            statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-error">Error</span> Source branch is required');
            return;
        }
        if (!title) {
            statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-error">Error</span> Title is required');
            return;
        }

        // Disable button and show loading
        var createBtn = $('#github-pr-create');
        createBtn.attr('disabled', 'disabled');
        createBtn.attr('aria-disabled', 'true');
        statusDiv.html('<span class="aui-icon aui-icon-wait">Creating pull request...</span>');

        // Make API call
        $.ajax({
            url: restUrl + '/pr/create',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                issueKey: issueKey,
                sourceBranch: sourceBranch,
                targetBranch: targetBranch,
                title: title,
                description: description
            }),
            success: function(response) {
                // Show success flag with PR link
                AJS.flag({
                    type: 'success',
                    title: 'Pull Request Created',
                    body: 'PR <a href="' + response.url + '" target="_blank">#' +
                          response.number + '</a> created successfully!',
                    close: 'auto'
                });

                // Close dialog and refresh
                AJS.dialog2('#github-create-pr-dialog').hide();
                clearPRForm();
                loadGitHubInfo(issueKey, restUrl);
            },
            error: function(xhr) {
                var errorMsg = 'Failed to create pull request';
                if (xhr.responseJSON && xhr.responseJSON.error) {
                    errorMsg = xhr.responseJSON.error;
                }

                // Show error flag
                AJS.flag({
                    type: 'error',
                    title: 'Error',
                    body: errorMsg,
                    close: 'auto'
                });

                statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-error">Error</span> ' + errorMsg);
            },
            complete: function() {
                createBtn.removeAttr('disabled');
                createBtn.removeAttr('aria-disabled');
            }
        });
    }

    /**
     * Clear PR form
     */
    function clearPRForm() {
        $('#github-source-branch').val('');
        $('#github-target-branch').val('main');
        $('#github-pr-description').val('');
        $('#github-pr-status').html('');
    }

    /**
     * Sanitize branch name (remove special characters, lowercase)
     */
    function sanitizeBranchName(text) {
        return text
            .toLowerCase()
            .replace(/[^a-z0-9-]/g, '-')
            .replace(/-+/g, '-')
            .replace(/^-|-$/g, '')
            .substring(0, 50);
    }

})(AJS.$);
