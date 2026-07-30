package com.aaron.sidegesture.config

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SigningConfigurationTest {

    private val buildScript = File(projectRoot(), "app/build.gradle.kts").readText()

    @Test
    fun releaseBuildUsesReleaseSigningConfiguration() {
        assertTrue(buildScript.contains("""register("release")"""))
        assertTrue(
            buildScript.contains(
                """signingConfig = signingConfigs.getByName("release")"""
            )
        )
        listOf(
            "STORE_FILE_NAME",
            "KEYSTORE_PASSWORD",
            "STORE_ALIAS",
            "KEY_PASSWORD"
        ).forEach { propertyName ->
            assertTrue(buildScript.contains("""getProperty("$propertyName")"""))
        }
    }

    @Test
    fun debugBuildUsesDefaultDebugSigningConfiguration() {
        val debugBlock = requireNotNull(
            Regex(
                """debug\s*\{([\s\S]*?)\n\s{8}\}"""
            ).find(buildScript)
        ).groupValues[1]

        assertFalse(debugBlock.contains("signingConfig"))
        assertFalse(buildScript.contains("validateReleaseSigning"))
        assertFalse(buildScript.contains("verifyReleaseSigningTaskGraph"))
    }

    companion object {
        private fun projectRoot(): File {
            return generateSequence(
                File(requireNotNull(System.getProperty("user.dir")))
            ) {
                it.parentFile
            }.first { File(it, "app/build.gradle.kts").isFile }
        }
    }
}
