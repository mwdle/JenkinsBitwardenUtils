// Call the main withBitwarden function and automatically parse and set environment variables from the secure note credential
// Supports Docker .env style KEY=VALUE pairs separated by newlines in a secure note stored within a Bitwarden vault
def call(Map config, Closure body) {
    withBitwarden(config) { credential ->
        def envList = []
        if (credential.notes) {
            credential.notes.eachLine { line ->
                def envVar = parseEnvLine(line)
                if (envVar) {
                    envList.add(envVar)
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

/**
 * Parses a single line of a .env file.
 * Returns a formatted "KEY=VALUE" string or null if the line is invalid/empty.
 * Gracefully strips inline comments in a manner that is similar to typical Docker .env file parsing
 */
private String parseEnvLine(String line) {
    def trimmedLine = line.trim()
    // Skip empty lines and full-line comments
    if (trimmedLine.isEmpty() || trimmedLine.startsWith('#')) {
        return null
    }
    // Split at the equals sign
    def parts = trimmedLine.split('=', 2)
    // Skip if no equals sign was found
    if (parts.size() == 2) {
        def key = parts[0].trim()
        def rawValue = parts[1].trim()
        def value
        // If the value is quoted, find the end of the quote
        if (rawValue.startsWith('"') || rawValue.startsWith("'")) {
            def quoteChar = rawValue[0]
            def endIndex = rawValue.indexOf(quoteChar, 1)
            // If no closing quote was found, use the whole string as the value
            if (endIndex == -1) {
                value = rawValue
            } else { // Set value to the quoted string - stripping inline comments
                value = rawValue.substring(0, endIndex + 1)
            }
        } else { // Not quoted - strip inline comments
            value = rawValue.split(' #', 2)[0].trim()
        }
        return "${key}=${value}"
    }
    return null // Return null for malformed lines
}