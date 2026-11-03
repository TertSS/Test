package com.readtracker.lite.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Локальные иконки вместо material-icons-extended: в APK не грузится библиотека целиком,
 * только 7 реально используемых векторов (в точности совпадают с оригинальными путями Material).
 */
object AppIcons {

    private fun icon(name: String, viewport: Float, d: String, filled: Boolean = true): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = viewport,
            viewportHeight = viewport,
        ).addPath(pathData = addPathNodes(d), fill = if (filled) SolidColor(Color.Black) else null).build()

    val Add: ImageVector by lazy {
        icon(
            "Add", 24f,
            "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6V13z",
        )
    }

    val Close: ImageVector by lazy {
        icon(
            "Close", 24f,
            "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z",
        )
    }

    val Remove: ImageVector by lazy {
        icon(
            "Remove", 24f,
            "M19,13H5v-2h14V13z",
        )
    }

    val Search: ImageVector by lazy {
        icon(
            "Search", 24f,
            "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z",
        )
    }

    /** AppIcons.KeyboardArrowRight (RTL-зеркалится автоматически через layoutDirection). */
    val KeyboardArrowRight: ImageVector by lazy {
        icon(
            "KeyboardArrowRight", 24f,
            "M8.59,16.59L13.17,12 8.59,7.41 10,6l6,6 -6,6 -1.41,-1.41z",
        )
    }

    val Book: ImageVector by lazy {
        icon(
            "Book", 24f,
            "M18,2H6C4.34,2 3,3.34 3,5v14c0,1.66 1.34,3 3,3h12c1.66,0 3,-1.34 3,-3V5C21,3.34 19.66,2 18,2zM18,4v12.93c-0.02,0 -0.03,0 -0.04,0.01C16.37,15.95 14.54,15.5 12,15.5c-2.54,0 -4.37,0.45 -5.96,1.44C6.03,16.93 6.02,16.93 6,16.93V4h12zM9.07,19.76C9.73,19.24 10.73,18.5 12,18.5c1.27,0 2.27,0.74 2.93,1.26H9.07z",
        )
    }

    val Star: ImageVector by lazy {
        icon(
            "Star", 24f,
            "M22,9.24l-7.19,-0.62L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21 12,17.27 18.18,21l-1.63,-7.03L22,9.24zM12,15.4l-3.76,2.27 1,-4.28 -3.32,-2.88 4.38,-0.38L12,6.1l1.71,4.04 4.38,0.38 -3.32,2.88 1,4.28L12,15.4z",
        )
    }

    val BrokenImage: ImageVector by lazy {
        icon(
            "BrokenImage", 24f,
            "M21,5v6.59l-3,-3.01L12,16.5l-3,-3.01L4,18.59V5C4,3.9 4.9,3 6,3h12C19.1,3 21,3.9 21,5zM3,20.59C3,21.37 3.63,22 4.41,22h15.18c0.78,0 1.41,-0.63 1.41,-1.41V19.6l-5.99,-6 -3,3 -2.99,-3 -4,4.01L3,20.59z",
        )
    }
}

