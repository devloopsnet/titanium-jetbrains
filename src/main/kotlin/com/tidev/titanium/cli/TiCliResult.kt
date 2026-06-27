package com.tidev.titanium.cli

/** Result of a synchronous CLI invocation. */
data class TiCliResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false,
) {
    val success: Boolean get() = exitCode == 0 && !timedOut
}

/** Raised when the CLI binary cannot be found / launched. */
class TiCliException(message: String, cause: Throwable? = null) : Exception(message, cause)
