package it.sapienza.appinterpreter.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Material icons the core icon set lacks, path for path from google/material-design-icons (Apache 2.0, the
 * "materialicons" 24px style). They're copied in rather than taken from material-icons-extended, a very large
 * library for two icons (and all of it in any build R8 doesn't shrink, such as -Pminify=false).
 */
object MaterialIcons {
    val AddPhotoAlternate: ImageVector by lazy {
        icon(
            "AddPhotoAlternate",
            "M19 7v2.99s-1.99.01-2 0V7h-3s.01-1.99 0-2h3V2h2v3h3v2h-3zm-3 4V8h-3V5H5c-1.1 0-2 .9-2 2v12c0 1.1." +
                "9 2 2 2h12c1.1 0 2-.9 2-2v-8h-3zM5 19l3-4 2 3 3-4 4 5H5z"
        )
    }

    val BrokenImage: ImageVector by lazy {
        icon(
            "BrokenImage",
            "M21 5v6.59l-3-3.01-4 4.01-4-4-4 4-3-3.01V5c0-1.1.9-2 2-2h14c1.1 0 2 .9 2 2zm-3 6.42l3 3.01V19c0 1.1-" +
                ".9 2-2 2H5c-1.1 0-2-.9-2-2v-6.58l3 2.99 4-4 4 4 4-3.99z"
        )
    }

    val ContentCopy: ImageVector by lazy {
        icon(
            "ContentCopy",
            "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1" +
                "-.9-2-2-2zm0 16H8V7h11v14z"
        )
    }

    val OpenInNew: ImageVector by lazy {
        icon(
            "OpenInNew",
            "M19 19H5V5h7V3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83" +
                " 1.41 1.41L19 6.41V10h2V3h-7z"
        )
    }

    private fun icon(name: String, path: String): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
        .build()
}
