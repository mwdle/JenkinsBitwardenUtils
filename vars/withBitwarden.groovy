/**
 * Retrieves one or more Bitwarden items and executes a closure with the results.

 * Note on Responsibility: This function securely fetches and provides secret data.
 * It is the caller's responsibility to ensure this data is handled securely within
 * the provided closure and not exposed to build logs or other insecure outputs.
 *
 * @param itemNames **(Required)** A `List` of Bitwarden item names to fetch.

 * @param bitwardenServerUrl *(Optional)* URL for the Bitwarden server.
 * Defaults to the CLI config or the `BITWARDEN_SERVER_URL` env variable.

 * @param apiKeyCredentialId *(Optional)* Jenkins credential ID for the API key.
 * Defaults to `'bitwarden-api-key'`.

 * @param masterPasswordCredentialId *(Optional)* Jenkins credential ID for the master password
 * Defaults to `'bitwarden-master-password'`.

 * @param body A closure to execute that receives a map of `[itemName: credential]` results.
 */
def call(Map config, Closure body) {
    if (!config.itemNames) {
        error 'withBitwarden: itemNames parameter (a List) is required'
    }

    def bitwardenServerUrl = config.bitwardenServerUrl ?: env.BITWARDEN_SERVER_URL
    def apiKeyCredentialId = config.apiKeyCredentialId ?: 'bitwarden-api-key'
    def masterPasswordCredentialId = config.masterPasswordCredentialId ?: 'bitwarden-master-password'
    
    def credentialsMap = [:]
    withCredentials([
        usernamePassword(credentialsId: apiKeyCredentialId, usernameVariable: 'BW_CLIENTID', passwordVariable: 'BW_CLIENTSECRET'),
        string(credentialsId: masterPasswordCredentialId, variable: 'BITWARDEN_MASTER_PASSWORD')
    ]) {
        try {
            if (bitwardenServerUrl)
                sh "bw config server '${bitwardenServerUrl}'"
            sh 'bw login --apikey'
            def sessionToken = sh(
                    script: 'bw unlock --raw --passwordenv BITWARDEN_MASTER_PASSWORD',
                    returnStdout: true
                ).trim()
            withEnv(["SESSION_TOKEN=${sessionToken}"]) {
                config.itemNames.each { itemName ->
                    echo "+ Fetching secret: '${itemName}'"
                    def credential = readJSON text: sh(
                        // SESSION_TOKEN is provided to shell command using environment variables and no groovy interpolation to avoid leakage
                        script: "set +x; bw get item '${itemName}' --session \$SESSION_TOKEN", // set +x ensures that `$SESSION_TOKEN` is not printed to the build log
                        returnStdout: true
                    ).trim()
                    credentialsMap[itemName] = credential
                }
            }
        } finally {
            sh 'bw logout || true' // Always logout even after failure
        }
    }
    // Closure does not execute within the `withCredentials` block for security reasons
    body(credentialsMap)
}