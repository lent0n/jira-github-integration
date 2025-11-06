/**
 * GitHub Integration Admin Configuration Page JavaScript
 * Compatible with Jira 9.12+ and 10.3+
 * Uses AUI (Atlassian User Interface) framework
 */
(function($) {
    'use strict';

    var restUrl;
    var baseUrl;

    // Initialize when DOM is ready
    $(document).ready(function() {
        restUrl = $('#rest-url').val();
        baseUrl = $('#base-url').val();

        initializeAdminPage();
    });

    function initializeAdminPage() {
        console.log('GitHub Integration: Initializing admin configuration page');

        // Initialize button handlers
        initializeSaveButton();
        initializeTestConnectionButton();
        initializeGenerateSecretButton();
        initializeRepositoryMappings();
        initializeRegisterWebhooksButton();
        initializeCancelButton();
    }

    /**
     * Initialize save configuration button
     */
    function initializeSaveButton() {
        $('#github-config-form').on('submit', function(e) {
            e.preventDefault();
            saveConfiguration();
        });
    }

    /**
     * Save configuration via REST API
     */
    function saveConfiguration() {
        var statusDiv = $('#save-status');
        var saveBtn = $('#save-config-btn');

        // Collect form data
        var config = {
            githubEnterpriseUrl: $('#githubEnterpriseUrl').val().trim(),
            githubApiUrl: $('#githubApiUrl').val().trim(),
            githubToken: $('#githubToken').val().trim(),
            trustCustomCertificates: $('#trustCustomCertificates').is(':checked'),
            webhookUrl: $('#webhookUrl').val().trim(),
            webhookSecret: $('#webhookSecret').val().trim(),
            branchNaming: $('#branchNaming').val().trim(),
            repositories: collectRepositoryMappings(),
            transitionMappings: collectTransitionMappings()
        };

        // Validate required fields
        if (!config.githubEnterpriseUrl) {
            statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-error">Error</span> GitHub Enterprise URL is required');
            return;
        }

        if (config.repositories.length === 0) {
            statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-error">Error</span> At least one repository mapping is required');
            return;
        }

        // Disable button and show loading
        saveBtn.attr('disabled', 'disabled');
        saveBtn.attr('aria-disabled', 'true');
        statusDiv.html('<span class="aui-icon aui-icon-wait">Saving configuration...</span>');

        // Make API call
        $.ajax({
            url: restUrl + '/config',
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify(config),
            success: function(response) {
                // Show success flag
                AJS.flag({
                    type: 'success',
                    title: 'Configuration Saved',
                    body: 'GitHub integration configuration saved successfully!',
                    close: 'auto'
                });

                statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-approve" style="color: #00875a;"></span> Configuration saved successfully');

                // Clear password fields (they were saved)
                if ($('#githubToken').val()) {
                    $('#githubToken').attr('placeholder', 'Token is set - enter new token to update');
                    $('#githubToken').val('');
                }
                if ($('#webhookSecret').val()) {
                    $('#webhookSecret').attr('placeholder', 'Secret is set - enter new secret to update');
                    $('#webhookSecret').val('');
                }

                // Reload page after 2 seconds to show updated status
                setTimeout(function() {
                    window.location.reload();
                }, 2000);
            },
            error: function(xhr) {
                var errorMsg = 'Failed to save configuration';
                if (xhr.responseJSON && xhr.responseJSON.error) {
                    errorMsg = xhr.responseJSON.error;
                } else if (xhr.responseText) {
                    errorMsg = xhr.responseText;
                }

                // Show error flag
                AJS.flag({
                    type: 'error',
                    title: 'Configuration Error',
                    body: errorMsg,
                    close: 'auto'
                });

                statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-error">Error</span> ' + errorMsg);
            },
            complete: function() {
                saveBtn.removeAttr('disabled');
                saveBtn.removeAttr('aria-disabled');
            }
        });
    }

    /**
     * Collect repository mappings from form
     */
    function collectRepositoryMappings() {
        var mappings = [];
        $('#repository-mappings .repository-mapping').each(function() {
            var mapping = {
                jiraProject: $(this).find('input[name="jiraProject"]').val().trim(),
                githubOwner: $(this).find('input[name="githubOwner"]').val().trim(),
                githubRepo: $(this).find('input[name="githubRepo"]').val().trim(),
                defaultBranch: $(this).find('input[name="defaultBranch"]').val().trim()
            };

            // Only add if all required fields are filled
            if (mapping.jiraProject && mapping.githubOwner && mapping.githubRepo) {
                mappings.push(mapping);
            }
        });
        return mappings;
    }

    /**
     * Collect transition mappings from form
     */
    function collectTransitionMappings() {
        return {
            pr_opened: $('#transition-pr-opened').val().trim(),
            pr_merged: $('#transition-pr-merged').val().trim(),
            pr_closed: $('#transition-pr-closed').val().trim(),
            pr_reopened: $('#transition-pr-reopened').val().trim()
        };
    }

    /**
     * Initialize test connection button
     */
    function initializeTestConnectionButton() {
        $('#test-connection-btn').on('click', function() {
            testConnection();
        });
    }

    /**
     * Test connection to GitHub Enterprise
     */
    function testConnection() {
        var statusDiv = $('#test-connection-status');
        var testBtn = $('#test-connection-btn');

        var githubEnterpriseUrl = $('#githubEnterpriseUrl').val().trim();
        var githubToken = $('#githubToken').val().trim();
        var trustCustomCertificates = $('#trustCustomCertificates').is(':checked');

        if (!githubEnterpriseUrl) {
            statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-error">Error</span> Enter GitHub Enterprise URL first');
            return;
        }

        // Disable button and show loading
        testBtn.attr('disabled', 'disabled');
        testBtn.attr('aria-disabled', 'true');
        statusDiv.html('<span class="aui-icon aui-icon-wait">Testing connection...</span>');

        // Make API call
        $.ajax({
            url: restUrl + '/config/test-connection',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                githubEnterpriseUrl: githubEnterpriseUrl,
                githubToken: githubToken || null,
                trustCustomCertificates: trustCustomCertificates
            }),
            success: function(response) {
                if (response.success) {
                    AJS.flag({
                        type: 'success',
                        title: 'Connection Successful',
                        body: response.message || 'Successfully connected to GitHub Enterprise',
                        close: 'auto'
                    });
                    statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-approve" style="color: #00875a;"></span> Connection successful');
                } else {
                    AJS.flag({
                        type: 'error',
                        title: 'Connection Failed',
                        body: response.message || 'Failed to connect to GitHub Enterprise',
                        close: 'auto'
                    });
                    statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-error">Error</span> ' + response.message);
                }
            },
            error: function(xhr) {
                var errorMsg = 'Connection test failed';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    errorMsg = xhr.responseJSON.message;
                } else if (xhr.responseText) {
                    errorMsg = xhr.responseText;
                }

                AJS.flag({
                    type: 'error',
                    title: 'Connection Failed',
                    body: errorMsg,
                    close: 'auto'
                });

                statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-error">Error</span> ' + errorMsg);
            },
            complete: function() {
                testBtn.removeAttr('disabled');
                testBtn.removeAttr('aria-disabled');
            }
        });
    }

    /**
     * Initialize generate secret button
     */
    function initializeGenerateSecretButton() {
        $('#generate-secret-btn').on('click', function() {
            generateWebhookSecret();
        });
    }

    /**
     * Generate webhook secret
     */
    function generateWebhookSecret() {
        var generateBtn = $('#generate-secret-btn');

        generateBtn.attr('disabled', 'disabled');
        generateBtn.attr('aria-disabled', 'true');

        $.ajax({
            url: restUrl + '/config/generate-secret',
            type: 'POST',
            success: function(response) {
                if (response.secret) {
                    $('#webhookSecret').val(response.secret);
                    $('#webhookSecret').attr('type', 'text'); // Show generated secret

                    AJS.flag({
                        type: 'info',
                        title: 'Secret Generated',
                        body: 'A new webhook secret has been generated. Click Save to apply.',
                        close: 'auto'
                    });
                }
            },
            error: function(xhr) {
                var errorMsg = 'Failed to generate secret';
                if (xhr.responseJSON && xhr.responseJSON.error) {
                    errorMsg = xhr.responseJSON.error;
                }

                AJS.flag({
                    type: 'error',
                    title: 'Generation Failed',
                    body: errorMsg,
                    close: 'auto'
                });
            },
            complete: function() {
                generateBtn.removeAttr('disabled');
                generateBtn.removeAttr('aria-disabled');
            }
        });
    }

    /**
     * Initialize repository mapping controls
     */
    function initializeRepositoryMappings() {
        // Add mapping button
        $('#add-mapping-btn').on('click', function() {
            addRepositoryMapping();
        });

        // Remove mapping buttons (use delegation for dynamically added elements)
        $('#repository-mappings').on('click', '.remove-mapping-btn', function() {
            $(this).closest('.repository-mapping').remove();
        });

        // If no mappings exist, add one by default
        if ($('#repository-mappings .repository-mapping').length === 0) {
            addRepositoryMapping();
        }
    }

    /**
     * Add new repository mapping row
     */
    function addRepositoryMapping() {
        var index = $('#repository-mappings .repository-mapping').length + 1;
        var html = '<div class="repository-mapping aui-group" data-index="' + index + '">' +
                   '    <div class="aui-item">' +
                   '        <div class="field-group">' +
                   '            <label>Jira Project Key</label>' +
                   '            <input class="text" type="text" name="jiraProject" placeholder="PROJ" />' +
                   '        </div>' +
                   '    </div>' +
                   '    <div class="aui-item">' +
                   '        <div class="field-group">' +
                   '            <label>GitHub Owner</label>' +
                   '            <input class="text" type="text" name="githubOwner" placeholder="organization" />' +
                   '        </div>' +
                   '    </div>' +
                   '    <div class="aui-item">' +
                   '        <div class="field-group">' +
                   '            <label>GitHub Repository</label>' +
                   '            <input class="text" type="text" name="githubRepo" placeholder="repository" />' +
                   '        </div>' +
                   '    </div>' +
                   '    <div class="aui-item">' +
                   '        <div class="field-group">' +
                   '            <label>Default Branch</label>' +
                   '            <input class="text" type="text" name="defaultBranch" value="main" />' +
                   '        </div>' +
                   '    </div>' +
                   '    <div class="aui-item" style="align-self: flex-end;">' +
                   '        <button type="button" class="aui-button remove-mapping-btn">' +
                   '            <span class="aui-icon aui-icon-small aui-iconfont-remove"></span>' +
                   '            Remove' +
                   '        </button>' +
                   '    </div>' +
                   '</div>';

        $('#repository-mappings').append(html);
    }

    /**
     * Initialize register webhooks button
     */
    function initializeRegisterWebhooksButton() {
        $('#register-webhooks-btn').on('click', function() {
            registerWebhooks();
        });
    }

    /**
     * Register webhooks with GitHub Enterprise
     */
    function registerWebhooks() {
        var statusDiv = $('#register-webhooks-status');
        var registerBtn = $('#register-webhooks-btn');

        // Disable button and show loading
        registerBtn.attr('disabled', 'disabled');
        registerBtn.attr('aria-disabled', 'true');
        statusDiv.html('<span class="aui-icon aui-icon-wait">Registering webhooks...</span>');

        // Make API call
        $.ajax({
            url: restUrl + '/config/register-webhooks',
            type: 'POST',
            success: function(response) {
                var successCount = response.successCount || 0;
                var totalCount = response.totalCount || 0;

                if (successCount === totalCount && successCount > 0) {
                    AJS.flag({
                        type: 'success',
                        title: 'Webhooks Registered',
                        body: 'Successfully registered ' + successCount + ' webhook(s)',
                        close: 'auto'
                    });
                    statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-approve" style="color: #00875a;"></span> ' +
                                 successCount + ' webhook(s) registered successfully');

                    // Reload page after 2 seconds to show updated status
                    setTimeout(function() {
                        window.location.reload();
                    }, 2000);
                } else {
                    var message = 'Registered ' + successCount + ' of ' + totalCount + ' webhooks';
                    AJS.flag({
                        type: 'warning',
                        title: 'Partial Success',
                        body: message,
                        close: 'auto'
                    });
                    statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-warning">Warning</span> ' + message);
                }

                // Show detailed results if available
                if (response.results && response.results.length > 0) {
                    console.log('Webhook registration results:', response.results);
                }
            },
            error: function(xhr) {
                var errorMsg = 'Failed to register webhooks';
                if (xhr.responseJSON && xhr.responseJSON.error) {
                    errorMsg = xhr.responseJSON.error;
                } else if (xhr.responseText) {
                    errorMsg = xhr.responseText;
                }

                AJS.flag({
                    type: 'error',
                    title: 'Registration Failed',
                    body: errorMsg,
                    close: 'auto'
                });

                statusDiv.html('<span class="aui-icon aui-icon-small aui-iconfont-error">Error</span> ' + errorMsg);
            },
            complete: function() {
                registerBtn.removeAttr('disabled');
                registerBtn.removeAttr('aria-disabled');
            }
        });
    }

    /**
     * Initialize cancel button
     */
    function initializeCancelButton() {
        $('#cancel-btn').on('click', function() {
            window.location.reload();
        });
    }

})(AJS.$);
