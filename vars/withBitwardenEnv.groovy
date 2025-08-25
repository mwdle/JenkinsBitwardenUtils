def call(Map config, Closure body) {
    // Call the main withBitwarden function and automatically parse environment variables
    withBitwarden(config) { credential ->
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
        
        // Execute the closure with environment variables set
        withEnv(envList) {
            body()
        }
    }
}
