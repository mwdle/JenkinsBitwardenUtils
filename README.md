# Jenkins Bitwarden Utils

A Jenkins shared library for securely retrieving secrets from Bitwarden in Jenkins pipelines.

## Features

- Secure Bitwarden integration with Jenkins credentials
- Memory-only secret handling (no disk writes)
- Full credential object access
- Automatic session management and cleanup
- Helper functions for common use cases

## Getting Started

### Prerequisites

1. **Jenkins Credentials**: Configure these in Jenkins:

   - `bitwarden-api-key` (Username/Password): Bitwarden API client credentials
   - `bitwarden-master-password` (Secret Text): Bitwarden master password

2. **Environment Variables**: Set in Jenkins agent configuration:

   - `BITWARDEN_SERVER_URL`: Your Bitwarden server URL

3. **Agent Requirements**: Jenkins agents must have certain software installed, see [mwdle/jenkins-agent](https://github.com/mwdle/jenkins-agent/blob/master/Dockerfile) as an example

## Usage

### Basic Usage

```groovy
@Library('JenkinsBitwardenUtils') _ // Must be added to Jenkins shared library configurations -- See https://www.jenkins.io/doc/book/pipeline/shared-libraries/

pipeline {
    agent any

    stages {
        stage('Deploy') {
            steps {
                script {
                    withBitwarden(itemName: 'MySecrets') { credential ->
                        // Access full credential object
                        echo "Item name: ${credential.name}"
                        echo "Username: ${credential.login?.username}"

                        // Parse environment variables from secure notes
                        def envList = []
                        if (credential.notes) {
                            credential.notes.split('\n').each { line ->
                                line = line.trim()
                                if (line && line.contains('=') && !line.startsWith('#')) {
                                    envList.add(line)
                                }
                            }
                        }

                        withEnv(envList) {
                            sh 'docker compose up -d'
                        }
                    }
                }
            }
        }
    }
}
```

### Environment Variables from Secure Notes (Recommended)

For the common use case of loading environment variables from Bitwarden secure notes:

```groovy
@Library('JenkinsBitwardenUtils') _ // Must be added to Jenkins shared library configurations -- See https://www.jenkins.io/doc/book/pipeline/shared-libraries/

pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                script {
                    withBitwardenEnv(itemName: 'CaddyConfig') {
                        // Environment variables automatically available
                        sh 'docker compose build'
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    withBitwardenEnv(itemName: 'CaddyConfig') {
                        // Environment variables automatically available
                        sh 'docker compose up -d'
                    }
                }
            }
        }
    }
}
```

### Custom Credential IDs

```groovy
withBitwarden(
    itemName: 'MySecrets',
    apiKeyCredentialId: 'my-custom-api-key',
    masterPasswordCredentialId: 'my-custom-master-password'
) { credential ->
    // Use credential object
}
```

## API Reference

### `withBitwardenEnv(config, closure)` (Recommended)

Retrieves a Bitwarden item and automatically parses environment variables from the secure notes, then executes the closure with those variables available.

**Parameters:**

- `itemName` (String, required): Name of the Bitwarden item to retrieve
- `apiKeyCredentialId` (String, optional): Jenkins credential ID for Bitwarden API key (default: 'bitwarden-api-key')
- `masterPasswordCredentialId` (String, optional): Jenkins credential ID for master password (default: 'bitwarden-master-password')

**Usage:**
This is the recommended function for loading environment variables from Bitwarden secure notes. It automatically parses the `notes` field and makes the environment variables available to your closure.

### `withBitwarden(config, closure)`

Retrieves a Bitwarden item and executes the closure with the full credential object.

**Parameters:**

- `itemName` (String, required): Name of the Bitwarden item to retrieve
- `apiKeyCredentialId` (String, optional): Jenkins credential ID for Bitwarden API key (default: 'bitwarden-api-key')
- `masterPasswordCredentialId` (String, optional): Jenkins credential ID for master password (default: 'bitwarden-master-password')

**Closure Parameter:**

- `credential` (Object): Full Bitwarden credentials as a JSON object, e.g. containing properties such as:
  - `name`: Item name
  - `notes`: Secure notes content (use this for environment variables)
  - And other Bitwarden item properties

**Environment Variables from Notes:**
Environment variables should be stored in Bitwarden secure note in this format:

```env
# Comments are ignored
KEY1=value1
KEY2=value2
# Another comment
KEY3=value3
```

## Security Features

- **No disk writes**: All secrets remain in memory only for the duration of the `withBitwarden` block
- **Error handling**: Ensures logout even on pipeline failures
- **Credential masking**: Jenkins automatically masks credential values in logs

## Configuration in Jenkins

1. **Install the shared library**:

   - Go to "Manage Jenkins" → "Configure System"
   - Under "Global Pipeline Libraries", add:
     - Name: `JenkinsBitwardenUtils`
     - Default version: `main`
     - Retrieval method: Modern SCM → Git
     - Project Repository: `https://github.com/mwdle/JenkinsBitwardenUtils.git`

2. **Configure credentials**:

   - Go to "Manage Jenkins" → "Manage Credentials" → Add the required credentials with the exact IDs mentioned above
   - Alternatively, manage and configure it via JCasC

3. **Set environment variables**:

   - Configure `BITWARDEN_SERVER_URL` in your Jenkins agent configuration

4. **Install required plugins**:
   - Pipeline Utility Steps plugin (for `readJSON` function)
