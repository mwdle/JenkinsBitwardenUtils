def call(Map config, Closure body) {
    // Call the main withBitwarden function and automatically parse environment variables
    withBitwarden(config) { credential ->
        // Parse environment variables (Docker .env) from secure note -- Mimics behavior outlined in https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/
        def envList = []
        if (credential.notes) {
            credential.notes.split('\n').each { line ->
                def trimmedLine = line.trim()
                if (trimmedLine =~ /^[A-Z0-9_]+=.*$/) {
                    if (trimmedLine.contains('"') || trimmedLine.contains("'"))
                        envList.add(trimmedLine)
                    else
                        envList.add(trimmedLine.split(' #', 2)[0].trim()) // Remove inline comments if the value has no quotations and the comment character (#) is preceded by a space
                }
            }
        } else {
            error("Error: The 'notes' field in the specified Bitwarden credential JSON is missing or empty.")
        }
        // Execute the closure with environment variables set
        withEnv(envList) {
            body()
        }
    }
}