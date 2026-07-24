package com.aaron.sidegesture.platform.shizuku

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ShizukuReleaseConfigurationTest {

    @Test
    fun userServicePackageIsCoveredByReleaseKeepRule() {
        val projectRoot = generateSequence(
            File(requireNotNull(System.getProperty("user.dir")))
        ) {
            it.parentFile
        }.first { File(it, "app/proguard-rules.pro").isFile }
        val source = File(
            projectRoot,
            "app/src/main/java/com/aaron/sidegesture/platform/shizuku/" +
                "ShizukuShellUserService.kt"
        ).readText()
        val packageName = requireNotNull(
            Regex("^package\\s+(\\S+)", RegexOption.MULTILINE)
                .find(source)
                ?.groupValues
                ?.getOrNull(1)
        )
        val rules = File(projectRoot, "app/proguard-rules.pro").readText()

        assertTrue(rules.contains("-keep class $packageName.** { *; }"))
        assertFalse(rules.contains("-keep class com.aaron.sidegesture.shizuku.** { *; }"))
    }

    @Test
    fun reflectivelyCreatedUserServiceIsKept() {
        val projectRoot = generateSequence(
            File(requireNotNull(System.getProperty("user.dir")))
        ) {
            it.parentFile
        }.first { File(it, "app/proguard-rules.pro").isFile }
        val source = File(
            projectRoot,
            "app/src/main/java/com/aaron/sidegesture/platform/shizuku/" +
                "ShizukuShellUserService.kt"
        ).readText()

        assertTrue(
            Regex("@Keep\\s+class\\s+ShizukuShellUserService")
                .containsMatchIn(source)
        )
    }
}
