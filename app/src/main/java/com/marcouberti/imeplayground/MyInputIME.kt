package com.marcouberti.imeplayground

import android.annotation.SuppressLint
import android.app.Application
import android.content.ClipDescription
import android.content.ContentUris
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * This class sets up and handles virtual keyboard events
 */
class MyInputIME : InputMethodService(), OnKeyboardActionListener {

    private lateinit var keyboard: Keyboard
    private lateinit var keyboardView: KeyboardView
    private val DEFAULT_HEIGHT = 250

    // called initially when inflating keyboard
    override fun onCreateInputView(): View {
        val layout = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = layout.findViewById(R.id.keyboard_view)
        val ctx = layout.context

        keyboard = MyKeyboard(ctx, R.xml.keyboard_qwerty, DEFAULT_HEIGHT)

        keyboard.isShifted = false
        keyboardView.keyboard = keyboard
        keyboardView.setOnKeyboardActionListener(this)
        keyboardView.invalidateAllKeys()

        return layout
    }


    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point keyboard is
     * bound to client, and is now receiving all of the detailed information
     * about the target of edits.
     */
    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
    }

    /**
     * Reload user preferences every time keyboard is inflated
     * as these preferences may have changed
     */
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        println("### App package that is using the keyboard: ${info?.packageName}")

        // Image content setup
        val mimeTypes: Array<String> = EditorInfoCompat.getContentMimeTypes(info)

        val gifSupported: Boolean = mimeTypes.any {
            ClipDescription.compareMimeTypes(it, "image/gif")
        }

        if (gifSupported) {
            // the target editor supports GIFs. enable corresponding content
        } else {
            // the target editor does not support GIFs. disable corresponding content
        }
    }

    /**
     * Handle keyboard key presses; if key is held down this fires multiple times
     */
    override fun onKey(primaryCode: Int, keyCodes: IntArray) {
        currentInputConnection?.commitText(primaryCode.toChar().toString(), 0)
        if (primaryCode.toChar() == 'g') {
            GlobalScope.launch {
                val gif = queryImages().firstOrNull() ?: return@launch
                commitGifImage(gif.contentUri, "gif test")
            }
        }
    }

    /**
     * Fired each time key is pressed; if key is held down this fires only once
     */
    override fun onPress(i: Int) {

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyUp(keyCode, event)
    }

    override fun onRelease(i: Int) {}

    override fun onText(charSequence: CharSequence) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}

    /**
     * Commits a GIF image
     *
     * @param contentUri Content URI of the GIF image to be sent
     * @param imageDescription Description of the GIF image to be sent
     */
    fun commitGifImage(contentUri: Uri, imageDescription: String) {
        val inputContentInfo = InputContentInfoCompat(
            contentUri,
            ClipDescription(imageDescription, arrayOf("image/gif")),
            null
        )
        val inputConnection = currentInputConnection
        val editorInfo = currentInputEditorInfo
        var flags = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            flags = flags or InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
        }
        InputConnectionCompat.commitContent(
            inputConnection,
            editorInfo,
            inputContentInfo,
            flags,
            null
        )
    }

    private suspend fun queryImages(): List<MediaStoreImage> {
        val images = mutableListOf<MediaStoreImage>()

        /**
         * Working with [ContentResolver]s can be slow, so we'll do this off the main
         * thread inside a coroutine.
         */
        withContext(Dispatchers.IO) {

            /**
             * A key concept when working with Android [ContentProvider]s is something called
             * "projections". A projection is the list of columns to request from the provider,
             * and can be thought of (quite accurately) as the "SELECT ..." clause of a SQL
             * statement.
             *
             * It's not _required_ to provide a projection. In this case, one could pass `null`
             * in place of `projection` in the call to [ContentResolver.query], but requesting
             * more data than is required has a performance impact.
             *
             * For this sample, we only use a few columns of data, and so we'll request just a
             * subset of columns.
             */
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )

            /**
             * The `selection` is the "WHERE ..." clause of a SQL statement. It's also possible
             * to omit this by passing `null` in its place, and then all rows will be returned.
             * In this case we're using a selection based on the date the image was taken.
             *
             * Note that we've included a `?` in our selection. This stands in for a variable
             * which will be provided by the next variable.
             */
            val selection = "${MediaStore.Images.Media.MIME_TYPE} = 'image/gif'"

            /**
             * The `selectionArgs` is a list of values that will be filled in for each `?`
             * in the `selection`.
             */
            val selectionArgs = arrayOf<String>(
                // Release day of the G1. :)
                //dateToTimestamp(day = 5, month = 0, year = 2021).toString()
            )

            /**
             * Sort order to use. This can also be null, which will use the default sort
             * order. For [MediaStore.Images], the default sort order is ascending by date taken.
             */
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            application.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->

                /**
                 * In order to retrieve the data from the [Cursor] that's returned, we need to
                 * find which index matches each column that we're interested in.
                 *
                 * There are two ways to do this. The first is to use the method
                 * [Cursor.getColumnIndex] which returns -1 if the column ID isn't found. This
                 * is useful if the code is programmatically choosing which columns to request,
                 * but would like to use a single method to parse them into objects.
                 *
                 * In our case, since we know exactly which columns we'd like, and we know
                 * that they must be included (since they're all supported from API 1), we'll
                 * use [Cursor.getColumnIndexOrThrow]. This method will throw an
                 * [IllegalArgumentException] if the column named isn't found.
                 *
                 * In either case, while this method isn't slow, we'll want to cache the results
                 * to avoid having to look them up for each row.
                 */
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

                Log.i("XXXTAG", "Found ${cursor.count} images")
                while (cursor.moveToNext()) {

                    // Here we'll use the column indexs that we found above.
                    val id = cursor.getLong(idColumn)
                    val dateModified =
                        Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))
                    val displayName = cursor.getString(displayNameColumn)


                    /**
                     * This is one of the trickiest parts:
                     *
                     * Since we're accessing images (using
                     * [MediaStore.Images.Media.EXTERNAL_CONTENT_URI], we'll use that
                     * as the base URI and append the ID of the image to it.
                     *
                     * This is the exact same way to do it when working with [MediaStore.Video] and
                     * [MediaStore.Audio] as well. Whatever `Media.EXTERNAL_CONTENT_URI` you
                     * query to get the items is the base, and the ID is the document to
                     * request there.
                     */
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val image = MediaStoreImage(id, displayName, dateModified, contentUri)
                    images += image

                    // For debugging, we'll output the image objects we create to logcat.
                    Log.v("XXXTAG", "Added image: $image")
                }
            }
        }

        Log.v("XXXTAG", "Found ${images.size} images")
        return images
    }

    /**
     * Simple data class to hold information about an image included in the device's MediaStore.
     */
    data class MediaStoreImage(
        val id: Long,
        val displayName: String,
        val dateAdded: Date,
        val contentUri: Uri
    )

    /**
     * Convenience method to convert a day/month/year date into a UNIX timestamp.
     *
     * We're suppressing the lint warning because we're not actually using the date formatter
     * to format the date to display, just to specify a format to use to parse it, and so the
     * locale warning doesn't apply.
     */
    @Suppress("SameParameterValue")
    @SuppressLint("SimpleDateFormat")
    private fun dateToTimestamp(day: Int, month: Int, year: Int): Long =
        SimpleDateFormat("dd.MM.yyyy").let { formatter ->
            TimeUnit.MICROSECONDS.toSeconds(formatter.parse("$day.$month.$year")?.time ?: 0)
        }

}
