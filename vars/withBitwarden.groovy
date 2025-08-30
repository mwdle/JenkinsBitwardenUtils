// Retrieves a Bitwarden item and executes closure with the credential object
// Requires: itemName parameter
// Optional: bitwardenServerUrl (defaults to Bitwarden CLI default), apiKeyCredentialId (default: 'bitwarden-api-key'), masterPasswordCredentialId (default: 'bitwarden-master-password')
def call(Map config, Closure body) {
    if (!config.itemName) {
        error 'withBitwarden: itemName parameter is required'
    }
    def bitwardenServerUrl = config.bitwardenServerUrl ?: env.BITWARDEN_SERVER_URL
    def apiKeyCredentialId = config.apiKeyCredentialId ?: 'bitwarden-api-key'
    def masterPasswordCredentialId = config.masterPasswordCredentialId ?: 'bitwarden-master-password'
    def credential
    withCredentials([
        usernamePassword(credentialsId: apiKeyCredentialId, usernameVariable: 'BW_CLIENTID', passwordVariable: 'BW_CLIENTSECRET'),
        string(credentialsId: masterPasswordCredentialId, variable: 'BITWARDEN_MASTER_PASSWORD')
    ]) {
        try {
            if (bitwardenServerUrl)
                sh "bw config server ${bitwardenServerUrl}"
            sh 'bw login --apikey'
            def sessionToken = sh(
                    script: 'bw unlock --raw --passwordenv BITWARDEN_MASTER_PASSWORD',
                    returnStdout: true
                ).trim()
            // Provides the secrets to the shell command using environment variables and no groovy interpolation to maximize security
            withEnv(["ITEM_NAME=${config.itemName}", "SESSION_TOKEN=${sessionToken}"]) {
                credential = readJSON text: sh(
                    script: 'bw get item $ITEM_NAME --session $SESSION_TOKEN',
                    returnStdout: true
                ).trim()
            }
        } finally {
            sh 'bw logout || true' // Always logout even after failure
        }
    }
    // Closure does not execute within the `withCredentials` block for security reasons
    body(credential)
}
