package com.atheris.qrcodepass.picker

/*https://github.com/robertlevonyan/media-picker*/
import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class ItemModel(
    val type: ItemType,
    val itemLabel: String = "",
    @DrawableRes
    val itemIcon: Int = 0,
    val hasBackground: Boolean = true,
    val backgroundType: ShapeType = ShapeType.TYPE_CIRCLE,
    @ColorInt
    val itemBackgroundColor: Int = 0,
) : Parcelable
