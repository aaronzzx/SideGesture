package com.aaron.sidegesture.config

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SigningConfigurationTest {

    @Test
    fun debugConfigurationDoesNotRequireReleaseSigningProperties() {
        val projectRoot = generateSequence(
            File(requireNotNull(System.getProperty("user.dir")))
        ) {
            it.parentFile
        }.first { File(it, "app/build.gradle.kts").isFile }
        val buildScript = File(projectRoot, "app/build.gradle.kts").readText()
        val debugBlock = requireNotNull(
            Regex(
                """debug\s*\{([\s\S]*?)\n\s{8}\}"""
            ).find(buildScript)
        ).groupValues[1]

        assertTrue(buildScript.contains("if (hasReleaseSigningProperties)"))
        assertTrue(buildScript.contains("releasePackageRequested && !hasReleaseSigningProperties"))
        assertFalse(debugBlock.contains("signingConfig"))
    }
}
