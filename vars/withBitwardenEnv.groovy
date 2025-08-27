def call(Map config, Closure body) {
    // Call the main withBitwarden function and automatically parse environment variables
    withBitwarden(config) { credential ->
        // Parse environment variables / properties from secure notes
        def envList = []
        if (credential.notes) {
            def props = new Properties()
            props.load(new StringReader(credential.notes))
            props.each { key, value ->
                envList.add("${key}=${value}")
            }
        }
        
        // Execute the closure with environment variables set
        withEnv(envList) {
            body()
        }
    }
}
