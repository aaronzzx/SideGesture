package com.aaron.sidegesture.ui.theme

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeThemeUsageTest {

    @Test
    fun composeStyleValuesComeFromMaterialTheme() {
        val projectRoot = generateSequence(
            File(requireNotNull(System.getProperty("user.dir")))
        ) {
            it.parentFile
        }.first { File(it, "app/src/main/java").isDirectory }
        val sourceRoot = File(projectRoot, "app/src/main/java")
        val themeRoot = File(
            sourceRoot,
            "com/aaron/sidegesture/ui/theme"
        ).canonicalFile
        val oldDimensionTokens = listOf(
            "TopBarPaddingExtra",
            "RootPadding",
            "ContentPaddingHorizontal",
            "ContentPaddingVerticalWithSection",
            "ContentPaddingVertical",
            "ItemPadding",
            "IconTextPadding",
            "SectionTitlePadding",
            "SectionPadding",
            "SectionPaddingNoTitle",
            "ScrollBottomPadding",
            "DividerHeight",
            "MainSecondaryTextPadding",
            "EdgeMenuPadding",
            "MarkColorSize",
            "MinItemHeight",
            "MinItemHeightNoSecondary",
            "MinInteractiveSize",
            "SubMinInteractiveSize",
            "MinIconSize",
            "DialogTitlePadding",
            "DialogTitleFontSize"
        )
        val forbiddenPatterns = listOf(
            Regex(
                """(?<![\w.])-?(?:(?:[1-9]\d*)(?:\.\d+)?|0\.\d*[1-9]\d*)\.(?:dp|sp)\b"""
            ) to
                "数字 dp/sp 必须由 MaterialTheme 提供",
            Regex("""(?:RoundedCornerShape|CutCornerShape)\s*\(\s*-?\d""") to
                "数字圆角必须由 MaterialTheme 提供",
            Regex("""TextStyle\s*\([^)]*fontSize\s*=""") to
                "字号必须由 MaterialTheme typography/textStyles 提供",
            Regex("""Color\s*\(\s*0x[0-9A-Fa-f]+""") to
                "固定色值必须由 MaterialTheme 提供",
            Regex("""Color\.(?:Black|White|Red|Green|Blue|Gray|DarkGray|LightGray|Yellow|Cyan|Magenta)\b""") to
                "固定命名颜色必须由 MaterialTheme 提供",
            Regex("""copy\s*\(\s*alpha\s*=\s*\d+(?:\.\d+)?f?""") to
                "视觉透明度必须由 MaterialTheme 提供",
            Regex("""\b(?:DimAlpha|GestureButtonColorAlpha)\b""") to
                "旧视觉透明度常量必须迁入 MaterialTheme"
        )

        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.canonicalFile.toPath().startsWith(themeRoot.toPath()) }
            .filter { it.readText().contains("androidx.compose") }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, rawLine ->
                    val line = rawLine.substringBefore("//").trim()
                    if (line.isEmpty() || line.startsWith("*") || line.startsWith("/*")) {
                        return@mapIndexedNotNull null
                    }
                    val reasons = buildList {
                        forbiddenPatterns.forEach { (pattern, reason) ->
                            if (pattern.containsMatchIn(line)) add(reason)
                        }
                        oldDimensionTokens.forEach { token ->
                            if (Regex("""\b$token\b""").containsMatchIn(line)) {
                                add("仍在使用旧 Dimension token：$token")
                            }
                        }
                    }
                    if (reasons.isEmpty()) {
                        null
                    } else {
                        val relativePath = file.relativeTo(projectRoot).invariantSeparatorsPath
                        "$relativePath:${index + 1}：${reasons.distinct().joinToString()}：$line"
                    }
                }
            }
            .toList()

        assertTrue(
            "发现绕过 MaterialTheme 的 Compose 风格值：\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }
}
