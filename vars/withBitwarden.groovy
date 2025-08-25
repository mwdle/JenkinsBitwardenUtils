// Retrieves a Bitwarden item and executes closure with the credential object
// Requires: BITWARDEN_SERVER_URL env var, itemName parameter
// Optional: apiKeyCredentialId (default: 'bitwarden-api-key'), masterPasswordCredentialId (default: 'bitwarden-master-password')
def call(Map config, Closure body) {
    if (!config.itemName) {
        error 'withBitwarden: itemName parameter is required'
    }
    def apiKeyCredentialId = config.apiKeyCredentialId ?: 'bitwarden-api-key'
    def masterPasswordCredentialId = config.masterPasswordCredentialId ?: 'bitwarden-master-password'
    withCredentials([
        usernamePassword(credentialsId: apiKeyCredentialId, 
                       usernameVariable: 'BW_CLIENTID', 
                       passwordVariable: 'BW_CLIENTSECRET'),
        string(credentialsId: masterPasswordCredentialId, 
               variable: 'BITWARDEN_MASTER_PASSWORD')
    ]) {
        try {
            def credentialJson = sh(
                script: '''
                    set +x
                    bw config server "$BITWARDEN_SERVER_URL" >&2
                    bw login --apikey >&2
                    SESSION_TOKEN=$(bw unlock --raw --passwordenv BITWARDEN_MASTER_PASSWORD)
                    bw get item "''' + config.itemName + '''" --session "$SESSION_TOKEN"
                    bw logout >&2
                ''',
                returnStdout: true
            ).trim()
            def credential = readJSON text: credentialJson
            body(credential)
        } finally {
            sh 'bw logout || true'
        }
    }
}
