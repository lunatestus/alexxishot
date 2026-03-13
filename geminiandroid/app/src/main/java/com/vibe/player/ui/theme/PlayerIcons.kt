package com.vibe.player.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object PlayerIcons {
    val Play: ImageVector
        get() = ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(6f, 4f)
            verticalLineToRelative(16f)
            curveToRelative(0f, 0.552f, 0.448f, 1f, 1f, 1f)
            curveToRelative(0.184f, 0f, 0.364f, -0.051f, 0.524f, -0.148f)
            lineToRelative(13f, -8f)
            curveToRelative(0.468f, -0.288f, 0.614f, -0.902f, 0.326f, -1.37f)
            curveToRelative(-0.082f, -0.133f, -0.193f, -0.244f, -0.326f, -0.326f)
            lineToRelative(-13f, -8f)
            curveToRelative(-0.468f, -0.288f, -1.082f, -0.142f, -1.37f, 0.326f)
            curveToRelative(-0.097f, 0.16f, -0.148f, 0.34f, -0.148f, 0.524f)
            close()
        }.build()

    val Pause: ImageVector
        get() = ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(9f, 4f)
            horizontalLineToRelative(-2f)
            curveToRelative(-1.105f, 0f, -2f, 0.895f, -2f, 2f)
            verticalLineToRelative(12f)
            curveToRelative(0f, 1.105f, 0.895f, 2f, 2f, 2f)
            horizontalLineToRelative(2f)
            curveToRelative(1.105f, 0f, 2f, -0.895f, 2f, -2f)
            verticalLineToRelative(-12f)
            curveToRelative(0f, -1.105f, -0.895f, -2f, -2f, -2f)
            close()
            moveTo(17f, 4f)
            horizontalLineToRelative(-2f)
            curveToRelative(-1.105f, 0f, -2f, 0.895f, -2f, 2f)
            verticalLineToRelative(12f)
            curveToRelative(0f, 1.105f, 0.895f, 2f, 2f, 2f)
            horizontalLineToRelative(2f)
            curveToRelative(1.105f, 0f, 2f, -0.895f, 2f, -2f)
            verticalLineToRelative(-12f)
            curveToRelative(0f, -1.105f, -0.895f, -2f, -2f, -2f)
            close()
        }.build()

    val Rewind: ImageVector
        get() = ImageVector.Builder(
            name = "Rewind",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(20.341f, 4.247f)
            lineToRelative(-8f, 7f)
            curveToRelative(-0.457f, 0.4f, -0.457f, 1.106f, 0f, 1.506f)
            lineToRelative(8f, 7f)
            curveToRelative(0.647f, 0.565f, 1.659f, 0.106f, 1.659f, -0.753f)
            verticalLineToRelative(-14f)
            curveToRelative(0f, -0.86f, -1.012f, -1.318f, -1.659f, -0.753f)
            close()
            moveTo(9.341f, 4.247f)
            lineToRelative(-8f, 7f)
            curveToRelative(-0.457f, 0.4f, -0.457f, 1.106f, 0f, 1.506f)
            lineToRelative(8f, 7f)
            curveToRelative(0.647f, 0.565f, 1.659f, 0.106f, 1.659f, -0.753f)
            verticalLineToRelative(-14f)
            curveToRelative(0f, -0.86f, -1.012f, -1.318f, -1.659f, -0.753f)
            close()
        }.build()

    val Forward: ImageVector
        get() = ImageVector.Builder(
            name = "Forward",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(2f, 5f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 0.86f, 1.012f, 1.318f, 1.659f, 0.753f)
            lineToRelative(8f, -7f)
            curveToRelative(0.457f, -0.4f, 0.457f, -1.106f, 0f, -1.506f)
            lineToRelative(-8f, -7f)
            curveToRelative(-0.647f, -0.565f, -1.659f, -0.106f, -1.659f, 0.753f)
            close()
            moveTo(13f, 5f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 0.86f, 1.012f, 1.318f, 1.659f, 0.753f)
            lineToRelative(8f, -7f)
            curveToRelative(0.457f, -0.4f, 0.457f, -1.106f, 0f, -1.506f)
            lineToRelative(-8f, -7f)
            curveToRelative(-0.647f, -0.565f, -1.659f, -0.106f, -1.659f, 0.753f)
            close()
        }.build()

    val Settings: ImageVector
        get() = ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(14.647f, 4.081f)
            curveToRelative(0.344f, 0.54f, 0.81f, 0.69f, 1.08f, 0.448f)
            curveToRelative(2.439f, -1.485f, 5.23f, 1.305f, 3.745f, 3.744f)
            curveToRelative(-0.242f, 0.27f, -0.092f, 0.736f, 0.447f, 1.08f)
            curveToRelative(2.775f, 0.673f, 2.775f, 4.62f, 0f, 5.294f)
            curveToRelative(-0.54f, 0.344f, -0.69f, 0.81f, -0.448f, 1.08f)
            curveToRelative(1.485f, 2.439f, -1.305f, 5.23f, -3.744f, 3.745f)
            curveToRelative(-0.27f, -0.242f, -0.736f, -0.092f, -1.08f, 0.447f)
            curveToRelative(-0.673f, 2.775f, -4.62f, 2.775f, -5.294f, 0f)
            curveToRelative(-0.344f, -0.54f, -0.81f, -0.69f, -1.08f, -0.448f)
            curveToRelative(-2.439f, 1.485f, -5.23f, -1.305f, -3.745f, -3.744f)
            curveToRelative(0.242f, -0.27f, 0.092f, -0.736f, -0.447f, -1.08f)
            curveToRelative(-2.775f, -0.673f, -2.775f, -4.62f, 0f, -5.294f)
            curveToRelative(0.54f, -0.344f, 0.69f, -0.81f, 0.448f, -1.08f)
            curveToRelative(-1.485f, -2.439f, 1.305f, -5.23f, 3.744f, -3.745f)
            curveToRelative(0.27f, 0.242f, 0.736f, 0.092f, 1.08f, -0.447f)
            curveToRelative(0.673f, -2.775f, 4.62f, -2.775f, 5.294f, 0f)
            close()
            moveTo(12f, 9f)
            curveToRelative(-1.657f, 0f, -3f, 1.343f, -3f, 3f)
            reflectiveCurveToRelative(1.343f, 3f, 3f, 3f)
            reflectiveCurveToRelative(3f, -1.343f, 3f, -3f)
            reflectiveCurveToRelative(-1.343f, -3f, -3f, -3f)
            close()
        }.build()

    val LayoutGrid: ImageVector
        get() = ImageVector.Builder(
            name = "LayoutGrid",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(9f, 3f)
            curveToRelative(1.105f, 0f, 2f, 0.895f, 2f, 2f)
            verticalLineToRelative(4f)
            curveToRelative(0f, 1.105f, -0.895f, 2f, -2f, 2f)
            horizontalLineToRelative(-4f)
            curveToRelative(-1.105f, 0f, -2f, -0.895f, -2f, -2f)
            verticalLineToRelative(-4f)
            curveToRelative(0f, -1.105f, 0.895f, -2f, 2f, -2f)
            close()
            moveTo(19f, 3f)
            curveToRelative(1.105f, 0f, 2f, 0.895f, 2f, 2f)
            verticalLineToRelative(4f)
            curveToRelative(0f, 1.105f, -0.895f, 2f, -2f, 2f)
            horizontalLineToRelative(-4f)
            curveToRelative(-1.105f, 0f, -2f, -0.895f, -2f, -2f)
            verticalLineToRelative(-4f)
            curveToRelative(0f, -1.105f, 0.895f, -2f, 2f, -2f)
            close()
            moveTo(9f, 13f)
            curveToRelative(1.105f, 0f, 2f, 0.895f, 2f, 2f)
            verticalLineToRelative(4f)
            curveToRelative(0f, 1.105f, -0.895f, 2f, -2f, 2f)
            horizontalLineToRelative(-4f)
            curveToRelative(-1.105f, 0f, -2f, -0.895f, -2f, -2f)
            verticalLineToRelative(-4f)
            curveToRelative(0f, -1.105f, 0.895f, -2f, 2f, -2f)
            close()
            moveTo(19f, 13f)
            curveToRelative(1.105f, 0f, 2f, 0.895f, 2f, 2f)
            verticalLineToRelative(4f)
            curveToRelative(0f, 1.105f, -0.895f, 2f, -2f, 2f)
            horizontalLineToRelative(-4f)
            curveToRelative(-1.105f, 0f, -2f, -0.895f, -2f, -2f)
            verticalLineToRelative(-4f)
            curveToRelative(0f, -1.105f, 0.895f, -2f, 2f, -2f)
            close()
        }.build()

    val LayoutList: ImageVector
        get() = ImageVector.Builder(
            name = "LayoutList",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.White)) {
            moveTo(18f, 3f)
            curveToRelative(1.657f, 0f, 3f, 1.343f, 3f, 3f)
            verticalLineToRelative(2f)
            curveToRelative(0f, 1.657f, -1.343f, 3f, -3f, 3f)
            horizontalLineToRelative(-12f)
            curveToRelative(-1.657f, 0f, -3f, -1.343f, -3f, -3f)
            verticalLineToRelative(-2f)
            curveToRelative(0f, -1.657f, 1.343f, -3f, 3f, -3f)
            close()
            moveTo(18f, 13f)
            curveToRelative(1.657f, 0f, 3f, 1.343f, 3f, 3f)
            verticalLineToRelative(2f)
            curveToRelative(0f, 1.657f, -1.343f, 3f, -3f, 3f)
            horizontalLineToRelative(-12f)
            curveToRelative(-1.657f, 0f, -3f, -1.343f, -3f, -3f)
            verticalLineToRelative(-2f)
            curveToRelative(0f, -1.657f, 1.343f, -3f, 3f, -3f)
            close()
        }.build()
}
