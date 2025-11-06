# Build and Test Guide

## Quick Start (5 minutes)

### 1. Build the Plugin

```bash
cd C:\Users\dylan\Documents\jira-github-integration
atlas-clean && atlas-package
```

**Expected Output**:
```
[INFO] BUILD SUCCESS
[INFO] JAR: target/github-integration-1.0.0-SNAPSHOT.jar
```

### 2. Run Locally

```bash
atlas-run
```

**Wait for**:
```
[INFO] jira started successfully in 60s at http://localhost:2990/jira
[INFO] Type Ctrl-C to shutdown gracefully
```

### 3. Access Jira

- **URL**: http://localhost:2990/jira
- **Username**: `admin`
- **Password**: `admin`

### 4. Verify Plugin

**Check Health**:
```bash
curl http://localhost:2990/jira/rest/github-integration/1.0/health
```

**Expected**:
```json
{"status":"ok","version":"1.0.0"}
```

---

## Configuration (First Time Setup)

### Step 1: Generate Webhook Secret

```bash
curl -X POST \
  http://localhost:2990/jira/rest/github-integration/1.0/config/generate-secret \
  -u admin:admin
```

Save the returned secret!

### Step 2: Configure Plugin

```bash
curl -X PUT \
  http://localhost:2990/jira/rest/github-integration/1.0/config \
  -H 'Content-Type: application/json' \
  -u admin:admin \
  -d '{
    "githubEnterpriseUrl": "https://github.your-company.com",
    "githubToken": "ghp_YOUR_TOKEN_HERE",
    "trustCustomCertificates": true,
    "webhookSecret": "YOUR_GENERATED_SECRET",
    "repositories": [
      {
        "jiraProject": "TEST",
        "githubOwner": "your-org",
        "githubRepo": "your-repo",
        "defaultBranch": "main",
        "branchNamingTemplate": "feature/{issueKey}-{summary}"
      }
    ],
    "branchNaming": "feature/{issueKey}-{summary}",
    "transitionMappings": {
      "pr_opened": "In Review",
      "pr_merged": "Done",
      "pr_closed": "Cancelled"
    }
  }'
```

### Step 3: Test Connection

```bash
curl -X POST \
  http://localhost:2990/jira/rest/github-integration/1.0/config/test-connection \
  -H 'Content-Type: application/json' \
  -u admin:admin \
  -d '{
    "githubEnterpriseUrl": "https://github.your-company.com",
    "githubToken": "ghp_YOUR_TOKEN_HERE",
    "trustCustomCertificates": true
  }'
```

**Expected**:
```json
{"success":true,"message":"Connection successful"}
```

### Step 4: Register Webhooks

```bash
curl -X POST \
  http://localhost:2990/jira/rest/github-integration/1.0/config/register-webhooks \
  -u admin:admin
```

---

## Testing Features

### Test 1: Create Branch

1. **Create test issue**: `TEST-1` in Jira
2. **Create branch**:
   ```bash
   curl -X POST http://localhost:2990/jira/rest/github-integration/1.0/branch/create \
     -H 'Content-Type: application/json' \
     -u admin:admin \
     -d '{
       "issueKey": "TEST-1",
       "baseBranch": "main",
       "branchName": "feature/TEST-1-test-integration"
     }'
   ```

3. **Verify**:
   - Branch exists in GitHub
   - Comment added to Jira issue
   - Remote link visible in Jira

### Test 2: Create Pull Request

```bash
curl -X POST http://localhost:2990/jira/rest/github-integration/1.0/pr/create \
  -H 'Content-Type: application/json' \
  -u admin:admin \
  -d '{
    "issueKey": "TEST-1",
    "sourceBranch": "feature/TEST-1-test-integration",
    "targetBranch": "main",
    "title": "[TEST-1] Test Integration",
    "description": "Testing the Jira-GitHub integration"
  }'
```

**Verify**:
- PR created in GitHub
- Issue transitioned to "In Review"
- Comment added to Jira
- Remote link updated

### Test 3: Webhook (PR Merged)

1. **Merge PR in GitHub**
2. **Check Jira**:
   - Issue transitioned to "Done"
   - Comment added: "Pull request merged..."

---

## Debugging

### Enable Debug Logging

1. Go to: http://localhost:2990/jira/secure/admin/ViewLogging.jspa
2. Add logger: `com.healthcanada.jira.github`
3. Set level: `DEBUG`
4. View logs at: `target/jira/home/log/atlassian-jira.log`

### Common Issues

**Build Fails**:
```bash
# Clean and retry
atlas-clean
rm -rf target
atlas-package
```

**Port 2990 Already in Use**:
```bash
# Kill existing Jira
atlas-stop
# Or change port
atlas-run --port 3000
```

**Connection to GitHub Fails**:
- Check GitHub Enterprise URL
- Verify token has `repo` scope
- Try with `trustCustomCertificates: true`

---

## Compatibility Testing

### Test on Jira 9.12

```bash
atlas-run --version 9.12.0
```

### Test on Jira 10.3

```bash
atlas-run --version 10.3.0
```

Both should work identically!

---

## Production Deployment

### 1. Build Production JAR

```bash
atlas-clean
atlas-package
```

### 2. Upload to Jira

1. Navigate to: **Jira Administration → Manage Apps**
2. Click **Upload app**
3. Select: `target/github-integration-1.0.0-SNAPSHOT.jar`
4. Click **Upload**

### 3. Configure

Use REST API (as documented above) to configure.

### 4. Monitor

```bash
# Watch logs
tail -f /var/atlassian/application-data/jira/log/atlassian-jira.log | grep GITHUB-INTEGRATION
```

---

## Performance Testing

### Load Test Webhook

```bash
# Send 100 webhook requests
for i in {1..100}; do
  curl -X POST http://localhost:2990/jira/plugins/servlet/github-webhook \
    -H 'X-GitHub-Event: ping' \
    -H 'X-Hub-Signature-256: sha256=test' \
    -d '{"zen":"Design for failure."}' &
done
wait
```

### Monitor Performance

```bash
# Check processing times in logs
grep "Webhook processed successfully" atlassian-jira.log
```

Expected: < 500ms per webhook

---

## Rollback

If issues occur:

1. **Disable Plugin**:
   - Jira Admin → Manage Apps → GitHub Integration → Disable

2. **Uninstall**:
   - Jira Admin → Manage Apps → GitHub Integration → Uninstall

3. **Configuration Preserved**: Stored in database, will be restored if plugin reinstalled

---

## CI/CD Integration

### Jenkins Pipeline

```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'atlas-clean'
                sh 'atlas-package'
            }
        }
        stage('Test') {
            steps {
                sh 'atlas-unit-test'
            }
        }
        stage('Deploy to Staging') {
            steps {
                sh 'curl -u admin:password -F file=@target/github-integration-1.0.0-SNAPSHOT.jar https://jira-staging.company.com/rest/plugins/1.0/'
            }
        }
    }
}
```

---

## Metrics

Track these metrics:

- **Branch Creation Success Rate**: > 95%
- **PR Creation Success Rate**: > 95%
- **Webhook Processing Time**: < 500ms
- **Issue Transition Success Rate**: > 90%
- **API Error Rate**: < 1%

---

## Documentation

- **README.md**: General overview
- **IMPLEMENTATION_STATUS.md**: Current status and capabilities
- **PHASE3_TESTING_GUIDE.md**: Detailed webhook testing
- **BUILD_AND_TEST.md**: This file

---

**Ready to Build**: ✅
**Ready to Test**: ✅
**Ready to Deploy**: ✅

For questions, check logs with DEBUG level enabled!
