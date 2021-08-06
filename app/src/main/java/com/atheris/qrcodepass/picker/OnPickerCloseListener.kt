package com.atheris.qrcodepass.picker

/*https://github.com/robertlevonyan/media-picker*/
import android.net.Uri

fun interface OnPickerCloseListener {
  fun onPickerClosed(type: ItemType, uris: List<Uri>)
}
