package com.example.data.local

import android.content.Context
import com.example.ui.DOODLE_PALETTE
import com.example.ui.DoodleStroke
import com.example.ui.DoodleSerializer
import com.example.ui.TestUserSlot
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class CoupleNoteTextBox(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val xOffsetDp: Float = 16f,
    val yOffsetDp: Float = 16f,
    val widthDp: Float = 130f,
    val heightDp: Float = 60f,
    val fontSizeSp: Float = 13f,
    val colorArgb: Long = 0xFFFFF176L // Soft warm sticky note yellow
)

data class CoupleNoteImage(
    val id: String = UUID.randomUUID().toString(),
    val imageBlobOrUri: String,
    val xOffsetDp: Float = 16f,
    val yOffsetDp: Float = 16f,
    val widthDp: Float = 80f,
    val heightDp: Float = 80f
)

data class CoupleNoteBoard(
    val authorSlot: String, // "USER_1" or "USER_2"
    val authorName: String,
    val textItems: List<CoupleNoteTextBox> = emptyList(),
    val images: List<CoupleNoteImage> = emptyList(),
    val doodleStrokes: List<DoodleStroke> = emptyList(),
    val doodleOffsetXDp: Float = 0f,
    val doodleOffsetYDp: Float = 0f,
    val doodleScale: Float = 1f,
    val lastUpdatedAtMs: Long = System.currentTimeMillis()
)

object CoupleNotePersistence {
    private const val PREFS_NAME = "duo_couple_notes_store"

    private fun noteKey(spaceCode: String, slot: TestUserSlot): String {
        val safeCode = spaceCode.trim().uppercase().ifBlank { "HIVE01" }
        return "note_${safeCode}_${slot.name}"
    }

    fun loadNote(context: Context, spaceCode: String, slot: TestUserSlot, authorName: String): CoupleNoteBoard {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(noteKey(spaceCode, slot), null)
        if (rawJson.isNullOrBlank()) {
            return CoupleNoteBoard(
                authorSlot = slot.name,
                authorName = authorName,
                textItems = emptyList(),
                images = emptyList(),
                doodleStrokes = emptyList()
            )
        }

        return try {
            val root = JSONObject(rawJson)
            val author = root.optString("author_slot", slot.name)
            val name = root.optString("author_name", authorName)
            val updated = root.optLong("updated_at_ms", System.currentTimeMillis())

            val textList = mutableListOf<CoupleNoteTextBox>()
            val textArr = root.optJSONArray("texts")
            if (textArr != null) {
                for (i in 0 until textArr.length()) {
                    val obj = textArr.getJSONObject(i)
                    textList.add(
                        CoupleNoteTextBox(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            text = obj.optString("text", ""),
                            xOffsetDp = obj.optDouble("x", 16.0).toFloat(),
                            yOffsetDp = obj.optDouble("y", 16.0).toFloat(),
                            widthDp = obj.optDouble("w", 130.0).toFloat(),
                            heightDp = obj.optDouble("h", 60.0).toFloat(),
                            fontSizeSp = obj.optDouble("font_size", 13.0).toFloat(),
                            colorArgb = obj.optLong("color", 0xFFFFF176L)
                        )
                    )
                }
            }

            val imgList = mutableListOf<CoupleNoteImage>()
            val imgArr = root.optJSONArray("images")
            if (imgArr != null) {
                for (i in 0 until imgArr.length()) {
                    val obj = imgArr.getJSONObject(i)
                    imgList.add(
                        CoupleNoteImage(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            imageBlobOrUri = obj.optString("uri", ""),
                            xOffsetDp = obj.optDouble("x", 16.0).toFloat(),
                            yOffsetDp = obj.optDouble("y", 16.0).toFloat(),
                            widthDp = obj.optDouble("w", 80.0).toFloat(),
                            heightDp = obj.optDouble("h", 80.0).toFloat()
                        )
                    )
                }
            }

            val doodleStr = root.optString("doodle", "")
            val strokes = DoodleSerializer.decode(doodleStr)
            val doodleX = root.optDouble("doodle_x", 0.0).toFloat()
            val doodleY = root.optDouble("doodle_y", 0.0).toFloat()
            val doodleScale = root.optDouble("doodle_scale", 1.0).toFloat()

            CoupleNoteBoard(
                authorSlot = author,
                authorName = name,
                textItems = textList,
                images = imgList,
                doodleStrokes = strokes,
                doodleOffsetXDp = doodleX,
                doodleOffsetYDp = doodleY,
                doodleScale = if (doodleScale > 0.1f) doodleScale else 1f,
                lastUpdatedAtMs = updated
            )
        } catch (_: Exception) {
            CoupleNoteBoard(
                authorSlot = slot.name,
                authorName = authorName,
                textItems = emptyList(),
                images = emptyList(),
                doodleStrokes = emptyList()
            )
        }
    }

    fun saveNote(context: Context, spaceCode: String, note: CoupleNoteBoard) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val root = JSONObject()
        root.put("author_slot", note.authorSlot)
        root.put("author_name", note.authorName)
        root.put("updated_at_ms", System.currentTimeMillis())

        val textArr = JSONArray()
        note.textItems.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("text", item.text)
            obj.put("x", item.xOffsetDp.toDouble())
            obj.put("y", item.yOffsetDp.toDouble())
            obj.put("w", item.widthDp.toDouble())
            obj.put("h", item.heightDp.toDouble())
            obj.put("font_size", item.fontSizeSp.toDouble())
            obj.put("color", item.colorArgb)
            textArr.put(obj)
        }
        root.put("texts", textArr)

        val imgArr = JSONArray()
        note.images.forEach { img ->
            val obj = JSONObject()
            obj.put("id", img.id)
            obj.put("uri", img.imageBlobOrUri)
            obj.put("x", img.xOffsetDp.toDouble())
            obj.put("y", img.yOffsetDp.toDouble())
            obj.put("w", img.widthDp.toDouble())
            obj.put("h", img.heightDp.toDouble())
            imgArr.put(obj)
        }
        root.put("images", imgArr)

        val doodleStr = DoodleSerializer.encode(note.doodleStrokes)
        root.put("doodle", doodleStr)
        root.put("doodle_x", note.doodleOffsetXDp.toDouble())
        root.put("doodle_y", note.doodleOffsetYDp.toDouble())
        root.put("doodle_scale", note.doodleScale.toDouble())

        val slot = if (note.authorSlot == TestUserSlot.USER_1.name) TestUserSlot.USER_1 else TestUserSlot.USER_2
        prefs.edit().putString(noteKey(spaceCode, slot), root.toString()).apply()
    }

    fun clearNote(context: Context, spaceCode: String, slot: TestUserSlot) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(noteKey(spaceCode, slot)).apply()
    }
}
