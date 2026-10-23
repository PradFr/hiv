package com.example.ui

import android.content.Context
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class JournalInkOption(
    val id: String,
    val label: String,
    val lightColor: Color,
    val darkColor: Color
) {
    fun resolveColor(isDark: Boolean): Color = if (isDark) darkColor else lightColor
}

val JOURNAL_INK_PALETTE = listOf(
    JournalInkOption(
        id = "cobalt_blue",
        label = "Cobalt",
        lightColor = Color(0xFF1D4ED8),
        darkColor = Color(0xFF60A5FA)
    ),
    JournalInkOption(
        id = "crimson_rose",
        label = "Crimson",
        lightColor = Color(0xFFBE123C),
        darkColor = Color(0xFFFB7185)
    ),
    JournalInkOption(
        id = "emerald_pine",
        label = "Emerald",
        lightColor = Color(0xFF047857),
        darkColor = Color(0xFF34D399)
    ),
    JournalInkOption(
        id = "royal_plum",
        label = "Plum",
        lightColor = Color(0xFF6D28D9),
        darkColor = Color(0xFFC084FC)
    ),
    JournalInkOption(
        id = "amber_ochre",
        label = "Amber",
        lightColor = Color(0xFFB45309),
        darkColor = Color(0xFFFBBF24)
    ),
    JournalInkOption(
        id = "ocean_teal",
        label = "Teal",
        lightColor = Color(0xFF0F766E),
        darkColor = Color(0xFF2DD4BF)
    ),
    JournalInkOption(
        id = "terracotta",
        label = "Clay",
        lightColor = Color(0xFFC2410C),
        darkColor = Color(0xFFFB923C)
    ),
    JournalInkOption(
        id = "midnight_slate",
        label = "Slate",
        lightColor = Color(0xFF334155),
        darkColor = Color(0xFFCBD5E1)
    )
)

data class AuthoredChar(
    val ch: Char,
    val author: TestUserSlot
)

data class AuthoredSegment(
    val author: TestUserSlot,
    val text: String
)

data class DailyDuoJournalLog(
    val dateIso: String,
    val user1TabChars: List<AuthoredChar>,
    val user2TabChars: List<AuthoredChar>
)

data class JournalStateSnapshot(
    val user1InkId: String,
    val user2InkId: String,
    val logsByDate: Map<String, DailyDuoJournalLog>
)

fun compressAuthoredChars(chars: List<AuthoredChar>): List<AuthoredSegment> {
    if (chars.isEmpty()) return emptyList()
    val result = mutableListOf<AuthoredSegment>()
    var currentAuthor = chars.first().author
    val sb = StringBuilder()
    for (item in chars) {
        if (item.author == currentAuthor) {
            sb.append(item.ch)
        } else {
            result.add(AuthoredSegment(currentAuthor, sb.toString()))
            currentAuthor = item.author
            sb.clear()
            sb.append(item.ch)
        }
    }
    if (sb.isNotEmpty()) {
        result.add(AuthoredSegment(currentAuthor, sb.toString()))
    }
    return result
}

fun expandAuthoredSegments(segments: List<AuthoredSegment>): List<AuthoredChar> {
    val list = ArrayList<AuthoredChar>(segments.sumOf { it.text.length })
    for (seg in segments) {
        for (ch in seg.text) {
            list.add(AuthoredChar(ch = ch, author = seg.author))
        }
    }
    return list
}

fun List<AuthoredChar>.asPlainString(): String {
    val sb = StringBuilder(size)
    for (item in this) sb.append(item.ch)
    return sb.toString()
}

data class NonDestructiveEditResult(
    val updatedChars: List<AuthoredChar>,
    val updatedSelection: TextRange,
    val preserveComposition: Boolean
)

/**
 * Cursor-anchored non-destructive diff engine:
 * Uses `previousSelection` and `previousComposition` to anchor the edit span so that even
 * when typing very fast in the middle of the other person's text (including typing letters
 * that match the partner's adjacent letters), the partner's characters never get swallowed
 * into the prefix or interleaved into what the active user is typing.
 */
fun applyNonDestructiveEdit(
    oldChars: List<AuthoredChar>,
    previousSelection: TextRange,
    previousComposition: TextRange?,
    newFieldValue: TextFieldValue,
    activeUser: TestUserSlot
): NonDestructiveEditResult {
    val oldStr = oldChars.asPlainString()
    val newStr = newFieldValue.text

    if (oldStr == newStr) {
        return NonDestructiveEditResult(
            updatedChars = oldChars,
            updatedSelection = newFieldValue.selection,
            preserveComposition = true
        )
    }

    val newCursor = newFieldValue.selection.end.coerceIn(0, newStr.length)

    // Determine the upper bound on prefix and suffix using the known cursor/composition position
    // before the keystroke. Any text after `editAnchorRightInOld` was to the right of the cursor.
    val prevMin = previousSelection.min.coerceIn(0, oldStr.length)
    val prevMax = previousSelection.max.coerceIn(0, oldStr.length)

    // The text to the right of the new cursor in `newStr` has length `newStr.length - newCursor`.
    // The suffix cannot cross to the left of `newCursor` in `newStr`, nor to the left of `prevMax`
    // if the tail after `prevMax` is unchanged.
    val rightOfNewCursorLen = (newStr.length - newCursor).coerceAtLeast(0)
    val rightOfPrevSelectionLen = (oldStr.length - prevMax).coerceAtLeast(0)

    val tailUnchanged = rightOfNewCursorLen == rightOfPrevSelectionLen &&
        oldStr.regionMatches(
            prevMax,
            newStr,
            newCursor,
            rightOfPrevSelectionLen
        )

    val suffixLen: Int
    val prefixLen: Int

    if (tailUnchanged) {
        // Fast path (covers all normal typing, fast typing in the middle of partner text,
        // IME word composition, and backspacing at the cursor):
        // The suffix is strictly anchored at `prevMax` in `oldStr` and `newCursor` in `newStr`!
        // This guarantees partner characters at `prevMax` are NEVER absorbed into `prefixLen`.
        suffixLen = rightOfPrevSelectionLen

        val maxAllowedPrefix = minOf(prevMin, newCursor)
        var p = 0
        while (p < maxAllowedPrefix && oldStr[p] == newStr[p]) {
            p++
        }
        prefixLen = p
    } else {
        // Fallback for full-range replacements or non-tail-preserving edits:
        // Still cap prefix at `minOf(prevMin, newCursor)` if prefix up to `prevMin` matches,
        // and cap suffix at `rightOfNewCursorLen` so cursor position is respected.
        var p = 0
        val maxPrefix = minOf(oldStr.length, newStr.length, newCursor)
        while (p < maxPrefix && oldStr[p] == newStr[p]) {
            p++
        }
        prefixLen = p

        var s = 0
        val maxSuffix = minOf(
            oldStr.length - prefixLen,
            newStr.length - prefixLen,
            rightOfNewCursorLen
        )
        while (
            s < maxSuffix &&
            oldStr[oldStr.length - 1 - s] == newStr[newStr.length - 1 - s]
        ) {
            s++
        }
        suffixLen = s
    }

    val removedEndInOld = (oldStr.length - suffixLen).coerceAtLeast(prefixLen)
    val insertedEndInNew = (newStr.length - suffixLen).coerceAtLeast(prefixLen)

    val removedSlice = oldChars.subList(prefixLen, removedEndInOld)
    val insertedText = newStr.substring(prefixLen, insertedEndInNew)

    val protectedPartnerChars = removedSlice.filter { it.author != activeUser }
    val blockedPartner = protectedPartnerChars.isNotEmpty()

    val newAuthoredAdditions = insertedText.map { ch ->
        AuthoredChar(ch = ch, author = activeUser)
    }

    val rebuilt = ArrayList<AuthoredChar>(
        prefixLen + protectedPartnerChars.size + newAuthoredAdditions.size + suffixLen
    )
    rebuilt.addAll(oldChars.subList(0, prefixLen))

    if (blockedPartner && insertedText.isNotEmpty()) {
        // If an IME composing replacement inadvertently spanned a protected partner char,
        // keep the partner char in its original relative side and place the newly typed text
        // right where the user is typing.
        val leadingPartner = removedSlice.takeWhile { it.author != activeUser }
        val trailingPartner = removedSlice.drop(leadingPartner.size).filter { it.author != activeUser }
        rebuilt.addAll(leadingPartner)
        rebuilt.addAll(newAuthoredAdditions)
        rebuilt.addAll(trailingPartner)
    } else {
        rebuilt.addAll(protectedPartnerChars)
        rebuilt.addAll(newAuthoredAdditions)
    }

    rebuilt.addAll(oldChars.subList(removedEndInOld, oldChars.size))

    val targetCursor = if (blockedPartner && insertedText.isEmpty()) {
        prefixLen.coerceIn(0, rebuilt.size)
    } else if (blockedPartner && insertedText.isNotEmpty()) {
        val leadingPartnerCount = removedSlice.takeWhile { it.author != activeUser }.size
        (prefixLen + leadingPartnerCount + newAuthoredAdditions.size).coerceIn(0, rebuilt.size)
    } else {
        (prefixLen + newAuthoredAdditions.size).coerceIn(0, rebuilt.size)
    }

    return NonDestructiveEditResult(
        updatedChars = rebuilt,
        updatedSelection = TextRange(targetCursor),
        preserveComposition = !blockedPartner
    )
}

class MultiAuthorInkVisualTransformation(
    private val authoredChars: List<AuthoredChar>,
    private val user1Color: Color,
    private val user2Color: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val styled = buildAnnotatedString {
            append(raw)
            var idx = 0
            val limit = minOf(raw.length, authoredChars.size)
            while (idx < limit) {
                val currentAuthor = authoredChars[idx].author
                var runEnd = idx + 1
                while (runEnd < limit && authoredChars[runEnd].author == currentAuthor) {
                    runEnd++
                }
                val inkColor = if (currentAuthor == TestUserSlot.USER_1) user1Color else user2Color
                addStyle(
                    style = SpanStyle(color = inkColor),
                    start = idx,
                    end = runEnd
                )
                idx = runEnd
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}

/**
 * Generates a real multi-page PDF document containing all journal days and both users' tabs,
 * rendering each person's writing in their chosen ink color.
 */
object DuoJournalPdfExporter {
    fun buildPdfBytes(
        snapshot: JournalStateSnapshot,
        user1Name: String,
        user2Name: String
    ): ByteArray {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        val marginH = 44f
        val marginTop = 52f
        val marginBottom = 52f
        val contentWidth = pageWidth - (marginH * 2f)

        val user1Ink = (JOURNAL_INK_PALETTE.find { it.id == snapshot.user1InkId }
            ?: JOURNAL_INK_PALETTE[0]).lightColor.toArgb()
        val user2Ink = (JOURNAL_INK_PALETTE.find { it.id == snapshot.user2InkId }
            ?: JOURNAL_INK_PALETTE[1]).lightColor.toArgb()

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(20, 24, 33)
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(100, 110, 125)
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val dateHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(25, 30, 40)
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val tabHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(55, 65, 81)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(220, 224, 230)
            strokeWidth = 1f
        }

        var pageNumber = 1
        var currentPage = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        var canvas: AndroidCanvas = currentPage.canvas
        var y = marginTop

        fun startNewPage() {
            pdfDocument.finishPage(currentPage)
            pageNumber++
            currentPage = pdfDocument.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            canvas = currentPage.canvas
            y = marginTop
        }

        fun ensureSpace(neededHeight: Float) {
            if (y + neededHeight > pageHeight - marginBottom) {
                startNewPage()
            }
        }

        // Document Header
        canvas.drawText("DuoSpace Journal", marginH, y, titlePaint)
        y += 18f
        val exportedAt = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("$user1Name & $user2Name  •  Exported $exportedAt", marginH, y, subtitlePaint)
        y += 16f

        // Ink Legend
        bodyPaint.color = user1Ink
        canvas.drawText("● $user1Name's Ink", marginH, y, bodyPaint)
        bodyPaint.color = user2Ink
        canvas.drawText("● $user2Name's Ink", marginH + 130f, y, bodyPaint)
        y += 14f
        canvas.drawLine(marginH, y, pageWidth - marginH, y, dividerPaint)
        y += 22f

        // Sort logs chronologically (oldest to newest, or include today if all are empty)
        val nonEmptyDates = snapshot.logsByDate.values
            .filter { it.user1TabChars.isNotEmpty() || it.user2TabChars.isNotEmpty() }
            .sortedBy { it.dateIso }
            .ifEmpty {
                listOf(
                    DailyDuoJournalLog(
                        dateIso = DuoJournalPersistence.todayIso(),
                        user1TabChars = emptyList(),
                        user2TabChars = emptyList()
                    )
                )
            }

        fun drawAuthoredParagraphs(chars: List<AuthoredChar>) {
            if (chars.isEmpty()) {
                ensureSpace(18f)
                subtitlePaint.color = android.graphics.Color.rgb(150, 155, 165)
                canvas.drawText("(No entry written)", marginH + 10f, y, subtitlePaint)
                subtitlePaint.color = android.graphics.Color.rgb(100, 110, 125)
                y += 18f
                return
            }

            // Split by newline while preserving AuthoredChar
            val lines = mutableListOf<List<AuthoredChar>>()
            var currentLine = mutableListOf<AuthoredChar>()
            for (ch in chars) {
                if (ch.ch == '\n') {
                    lines.add(currentLine)
                    currentLine = mutableListOf()
                } else {
                    currentLine.add(ch)
                }
            }
            lines.add(currentLine)

            val lineHeight = 16f
            val startX = marginH + 10f
            val maxLineX = marginH + contentWidth

            for (paragraphLine in lines) {
                if (paragraphLine.isEmpty()) {
                    y += lineHeight * 0.7f
                    continue
                }
                ensureSpace(lineHeight)
                var cursorX = startX
                var i = 0
                while (i < paragraphLine.size) {
                    // Group next word or whitespace token so wrapping doesn't split mid-word
                    val isSpace = paragraphLine[i].ch.isWhitespace()
                    var tokenEnd = i + 1
                    while (
                        tokenEnd < paragraphLine.size &&
                        paragraphLine[tokenEnd].ch.isWhitespace() == isSpace
                    ) {
                        tokenEnd++
                    }
                    val tokenChars = paragraphLine.subList(i, tokenEnd)
                    val tokenString = tokenChars.asPlainString()
                    val tokenWidth = bodyPaint.measureText(tokenString)

                    if (cursorX + tokenWidth > maxLineX && cursorX > startX) {
                        y += lineHeight
                        ensureSpace(lineHeight)
                        cursorX = startX
                        if (isSpace) {
                            i = tokenEnd
                            continue
                        }
                    }

                    // Draw each character run inside the token with its author's ink color
                    var r = 0
                    while (r < tokenChars.size) {
                        val runAuthor = tokenChars[r].author
                        var rEnd = r + 1
                        while (rEnd < tokenChars.size && tokenChars[rEnd].author == runAuthor) {
                            rEnd++
                        }
                        val runText = tokenChars.subList(r, rEnd).asPlainString()
                        bodyPaint.color = if (runAuthor == TestUserSlot.USER_1) user1Ink else user2Ink
                        canvas.drawText(runText, cursorX, y, bodyPaint)
                        cursorX += bodyPaint.measureText(runText)
                        r = rEnd
                    }

                    i = tokenEnd
                }
                y += lineHeight
            }
        }

        for (dayLog in nonEmptyDates) {
            ensureSpace(64f)
            val readableDate = DuoJournalPersistence.formatReadableDate(dayLog.dateIso)
            canvas.drawText("$readableDate  (${dayLog.dateIso})", marginH, y, dateHeaderPaint)
            y += 18f

            // Person 1's Tab
            ensureSpace(28f)
            tabHeaderPaint.color = user1Ink
            canvas.drawText("$user1Name's Entry:", marginH + 4f, y, tabHeaderPaint)
            y += 16f
            drawAuthoredParagraphs(dayLog.user1TabChars)
            y += 8f

            // Person 2's Tab
            ensureSpace(28f)
            tabHeaderPaint.color = user2Ink
            canvas.drawText("$user2Name's Entry:", marginH + 4f, y, tabHeaderPaint)
            y += 16f
            drawAuthoredParagraphs(dayLog.user2TabChars)
            y += 12f

            canvas.drawLine(marginH, y, pageWidth - marginH, y, dividerPaint)
            y += 20f
        }

        pdfDocument.finishPage(currentPage)
        val out = ByteArrayOutputStream()
        pdfDocument.writeTo(out)
        pdfDocument.close()
        return out.toByteArray()
    }
}

object DuoJournalPersistence {
    private const val LEGACY_PREFS_NAME = "duo_space_journal_prefs_v2"
    private const val KEY_JSON = "journal_state_json"
    private const val SANDBOX_DIR = "sandboxed_journal_e2ee"
    private const val ENCRYPTED_STATE_FILE = "journal_state.venc"

    private fun journalSandboxFile(context: Context): File {
        val dir = File(context.noBackupFilesDir, SANDBOX_DIR)
        if (!dir.exists()) dir.mkdirs()
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) runCatching { noMedia.createNewFile() }
        return File(dir, ENCRYPTED_STATE_FILE)
    }

    fun todayIso(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun dateDaysAgoIso(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun formatReadableDate(dateIso: String): String {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso)
            if (parsed != null) {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(parsed)
            } else {
                dateIso
            }
        } catch (_: Exception) {
            dateIso
        }
    }

    fun loadState(context: Context, user1Name: String, user2Name: String): JournalStateSnapshot {
        val encFile = journalSandboxFile(context)
        var raw: String? = null

        if (encFile.exists()) {
            val decrypted = VaultCryptoEngine.decryptBytes(context, runCatching { encFile.readBytes() }.getOrNull() ?: ByteArray(0))
            if (decrypted != null) {
                raw = String(decrypted, Charsets.UTF_8)
            }
        }

        // Migrate any existing legacy SharedPreferences into AES-256-GCM sandboxed file and wipe plaintext prefs
        if (raw.isNullOrBlank()) {
            val prefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            val legacyRaw = prefs.getString(KEY_JSON, null)
            if (!legacyRaw.isNullOrBlank()) {
                raw = legacyRaw
                prefs.edit().clear().apply()
            }
        }

        if (raw.isNullOrBlank()) {
            val seeded = createInitialState()
            saveState(context, seeded)
            return seeded
        }
        return try {
            val root = JSONObject(raw)
            val user1Ink = root.optString("user1InkId", "cobalt_blue")
            val user2Ink = root.optString("user2InkId", "crimson_rose").let {
                if (it == user1Ink) "crimson_rose" else it
            }

            val logsMap = linkedMapOf<String, DailyDuoJournalLog>()
            val logsArray = root.optJSONArray("logs") ?: JSONArray()
            for (i in 0 until logsArray.length()) {
                val obj = logsArray.getJSONObject(i)
                val dateIso = obj.getString("dateIso")
                val u1Tab = parseSegmentsArray(obj.optJSONArray("user1Tab"))
                val u2Tab = parseSegmentsArray(obj.optJSONArray("user2Tab"))
                logsMap[dateIso] = DailyDuoJournalLog(
                    dateIso = dateIso,
                    user1TabChars = u1Tab,
                    user2TabChars = u2Tab
                )
            }

            val today = todayIso()
            if (!logsMap.containsKey(today)) {
                logsMap[today] = DailyDuoJournalLog(
                    dateIso = today,
                    user1TabChars = emptyList(),
                    user2TabChars = emptyList()
                )
            }

            JournalStateSnapshot(
                user1InkId = user1Ink,
                user2InkId = user2Ink,
                logsByDate = logsMap
            )
        } catch (_: Exception) {
            val seeded = createInitialState()
            saveState(context, seeded)
            seeded
        }
    }

    fun saveState(context: Context, snapshot: JournalStateSnapshot) {
        val root = JSONObject()
        root.put("user1InkId", snapshot.user1InkId)
        root.put("user2InkId", snapshot.user2InkId)

        val logsArray = JSONArray()
        for ((_, log) in snapshot.logsByDate) {
            val obj = JSONObject()
            obj.put("dateIso", log.dateIso)
            obj.put("user1Tab", serializeSegmentsArray(compressAuthoredChars(log.user1TabChars)))
            obj.put("user2Tab", serializeSegmentsArray(compressAuthoredChars(log.user2TabChars)))
            logsArray.put(obj)
        }
        root.put("logs", logsArray)
        val encrypted = VaultCryptoEngine.encryptBytes(context, root.toString().toByteArray(Charsets.UTF_8))
        runCatching {
            journalSandboxFile(context).writeBytes(encrypted)
        }
    }

    private fun parseSegmentsArray(arr: JSONArray?): List<AuthoredChar> {
        if (arr == null) return emptyList()
        val segments = mutableListOf<AuthoredSegment>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val author = if (item.optString("author") == TestUserSlot.USER_2.name) {
                TestUserSlot.USER_2
            } else {
                TestUserSlot.USER_1
            }
            val text = item.optString("text", "")
            segments.add(AuthoredSegment(author, text))
        }
        return expandAuthoredSegments(segments)
    }

    private fun serializeSegmentsArray(segments: List<AuthoredSegment>): JSONArray {
        val arr = JSONArray()
        for (seg in segments) {
            val obj = JSONObject()
            obj.put("author", seg.author.name)
            obj.put("text", seg.text)
            arr.put(obj)
        }
        return arr
    }

    private fun createInitialState(): JournalStateSnapshot {
        val today = todayIso()
        return JournalStateSnapshot(
            user1InkId = "cobalt_blue",
            user2InkId = "crimson_rose",
            logsByDate = linkedMapOf(
                today to DailyDuoJournalLog(
                    dateIso = today,
                    user1TabChars = emptyList(),
                    user2TabChars = emptyList()
                )
            )
        )
    }
}

@Composable
fun DuoJournalScreen(
    activeUser: TestUserSlot,
    user1Name: String,
    user2Name: String,
    onSwitchUser: (TestUserSlot) -> Unit,
    onBackToDashboard: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenWishlist: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    unreadChatCount: Int
) {
    BackHandler(onBack = onBackToDashboard)

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var journalSnapshot by remember {
        mutableStateOf(DuoJournalPersistence.loadState(context, user1Name, user2Name))
    }

    // Debounce persistence writes off the main typing path so fast typing is 100% instant
    LaunchedEffect(journalSnapshot) {
        delay(250L)
        withContext(Dispatchers.IO) {
            DuoJournalPersistence.saveState(context, journalSnapshot)
        }
    }

    var currentTodayIso by remember { mutableStateOf(DuoJournalPersistence.todayIso()) }
    var selectedDateIso by rememberSaveable { mutableStateOf(DuoJournalPersistence.todayIso()) }

    // Automatically refresh to the new day's notepad when midnight passes
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            val latestToday = DuoJournalPersistence.todayIso()
            if (latestToday != currentTodayIso) {
                val wasOnToday = (selectedDateIso == currentTodayIso)
                currentTodayIso = latestToday
                if (!journalSnapshot.logsByDate.containsKey(latestToday)) {
                    val updatedMap = LinkedHashMap<String, DailyDuoJournalLog>()
                    updatedMap[latestToday] = DailyDuoJournalLog(
                        dateIso = latestToday,
                        user1TabChars = emptyList(),
                        user2TabChars = emptyList()
                    )
                    updatedMap.putAll(journalSnapshot.logsByDate)
                    journalSnapshot = journalSnapshot.copy(logsByDate = updatedMap)
                }
                if (wasOnToday) {
                    selectedDateIso = latestToday
                }
            }
        }
    }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showInkPicker by rememberSaveable { mutableStateOf(false) }
    var showDateSearchPanel by rememberSaveable { mutableStateOf(false) }
    var dateSearchQuery by rememberSaveable { mutableStateOf("") }
    var showExportPdfDialog by rememberSaveable { mutableStateOf(false) }
    var lastExportSavedPath by remember { mutableStateOf<String?>(null) }

    val createPdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val pdfBytes = DuoJournalPdfExporter.buildPdfBytes(
                    snapshot = journalSnapshot,
                    user1Name = user1Name,
                    user2Name = user2Name
                )
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(pdfBytes)
                }
                Toast.makeText(context, "Journal exported as PDF", Toast.LENGTH_SHORT).show()
                showExportPdfDialog = false
            }.onFailure {
                Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val activeUserName = if (activeUser == TestUserSlot.USER_1) user1Name else user2Name

    val user1InkOption = remember(journalSnapshot.user1InkId) {
        JOURNAL_INK_PALETTE.find { it.id == journalSnapshot.user1InkId } ?: JOURNAL_INK_PALETTE[0]
    }
    val user2InkOption = remember(journalSnapshot.user2InkId) {
        JOURNAL_INK_PALETTE.find { it.id == journalSnapshot.user2InkId } ?: JOURNAL_INK_PALETTE[1]
    }

    val user1InkColor = user1InkOption.resolveColor(isDark)
    val user2InkColor = user2InkOption.resolveColor(isDark)
    val activeUserInkColor = if (activeUser == TestUserSlot.USER_1) user1InkColor else user2InkColor

    val currentDayLog = remember(journalSnapshot.logsByDate, selectedDateIso) {
        journalSnapshot.logsByDate[selectedDateIso] ?: DailyDuoJournalLog(
            dateIso = selectedDateIso,
            user1TabChars = emptyList(),
            user2TabChars = emptyList()
        )
    }

    val activeTabChars = if (selectedTabIndex == 0) {
        currentDayLog.user1TabChars
    } else {
        currentDayLog.user2TabChars
    }

    // Keyed only on (selectedDateIso, selectedTabIndex) so active typing is never interrupted
    var textFieldValue by remember(selectedDateIso, selectedTabIndex) {
        val initialText = activeTabChars.asPlainString()
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }

    fun updateActiveTabChars(newChars: List<AuthoredChar>) {
        val updatedLog = if (selectedTabIndex == 0) {
            currentDayLog.copy(user1TabChars = newChars)
        } else {
            currentDayLog.copy(user2TabChars = newChars)
        }
        val newMap = LinkedHashMap(journalSnapshot.logsByDate)
        newMap[selectedDateIso] = updatedLog
        journalSnapshot = journalSnapshot.copy(logsByDate = newMap)
    }

    val visualTransformation = remember(activeTabChars, user1InkColor, user2InkColor) {
        MultiAuthorInkVisualTransformation(
            authoredChars = activeTabChars,
            user1Color = user1InkColor,
            user2Color = user2InkColor
        )
    }

    if (showExportPdfDialog) {
        val loggedDaysCount = remember(journalSnapshot.logsByDate) {
            journalSnapshot.logsByDate.values.count {
                it.user1TabChars.isNotEmpty() || it.user2TabChars.isNotEmpty()
            }.coerceAtLeast(1)
        }

        AlertDialog(
            onDismissRequest = { showExportPdfDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "Export PDF",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Export Journal as PDF",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Exports all $loggedDaysCount day(s) written so far with both $user1Name's and $user2Name's entries rendered in your chosen ink colors.",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (lastExportSavedPath != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Saved to app files:\n$lastExportSavedPath",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileName = "DuoSpace_Journal_${DuoJournalPersistence.todayIso()}.pdf"
                        // Only export to phone when user explicitly confirms in the system Save dialog
                        createPdfDocumentLauncher.launch(fileName)
                    },
                    modifier = Modifier.testTag("journal_export_pdf_confirm_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save PDF to Phone")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExportPdfDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    // DuoSpace App Header matching DuoDashboardScreen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Journal",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (selectedDateIso == currentTodayIso) {
                                            "Today • ${DuoJournalPersistence.formatReadableDate(selectedDateIso)}"
                                        } else {
                                            DuoJournalPersistence.formatReadableDate(selectedDateIso)
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = "DuoSpace • $user1Name & $user2Name",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Search by Date Icon
                            IconButton(
                                onClick = {
                                    showDateSearchPanel = !showDateSearchPanel
                                    if (showDateSearchPanel) showInkPicker = false
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (showDateSearchPanel) {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .testTag("journal_search_date_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Search by Date",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Export as PDF Icon
                            IconButton(
                                onClick = { showExportPdfDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("journal_export_pdf_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Export PDF",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Ink Color Dot Trigger
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(activeUserInkColor)
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        showInkPicker = !showInkPicker
                                        if (showInkPicker) showDateSearchPanel = false
                                    }
                                    .testTag("journal_ink_palette_button")
                            )
                        }
                    }

                    // Expandable Ink Color Strip
                    AnimatedVisibility(visible = showInkPicker) {
                        val activeInkId = if (activeUser == TestUserSlot.USER_1) {
                            journalSnapshot.user1InkId
                        } else {
                            journalSnapshot.user2InkId
                        }
                        val partnerInkId = if (activeUser == TestUserSlot.USER_1) {
                            journalSnapshot.user2InkId
                        } else {
                            journalSnapshot.user1InkId
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                                .testTag("journal_ink_picker_bar"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(JOURNAL_INK_PALETTE, key = { it.id }) { inkOption ->
                                val resolved = inkOption.resolveColor(isDark)
                                val isSelected = inkOption.id == activeInkId
                                val isLockedByPartner = inkOption.id == partnerInkId

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isLockedByPartner) resolved.copy(alpha = 0.3f) else resolved
                                        )
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable(enabled = !isLockedByPartner) {
                                            journalSnapshot = if (activeUser == TestUserSlot.USER_1) {
                                                journalSnapshot.copy(user1InkId = inkOption.id)
                                            } else {
                                                journalSnapshot.copy(user2InkId = inkOption.id)
                                            }
                                            showInkPicker = false
                                        }
                                        .testTag("journal_ink_option_${inkOption.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        isSelected -> {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        isLockedByPartner -> {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Taken",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Expandable Search by Date Panel
                    AnimatedVisibility(visible = showDateSearchPanel) {
                        MinimalDateSearchPanel(
                            selectedDateIso = selectedDateIso,
                            currentTodayIso = currentTodayIso,
                            searchQuery = dateSearchQuery,
                            onSearchQueryChange = { dateSearchQuery = it },
                            logsByDate = journalSnapshot.logsByDate,
                            onSelectDate = { chosenIso ->
                                selectedDateIso = chosenIso
                                if (!journalSnapshot.logsByDate.containsKey(chosenIso)) {
                                    val newMap = LinkedHashMap(journalSnapshot.logsByDate)
                                    newMap[chosenIso] = DailyDuoJournalLog(
                                        dateIso = chosenIso,
                                        user1TabChars = emptyList(),
                                        user2TabChars = emptyList()
                                    )
                                    journalSnapshot = journalSnapshot.copy(logsByDate = newMap)
                                }
                                showDateSearchPanel = false
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            ModernBottomNavBar(
                currentDestination = DuoBottomNavDestination.JOURNAL,
                unreadChatCount = unreadChatCount,
                onClickDashboard = onBackToDashboard,
                onClickChat = onOpenChat,
                onClickJournal = {},
                onClickWishlist = onOpenWishlist,
                onClickVault = onOpenVault
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Framed Card inside the DuoSpace layout so it feels native to the app
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("journal_notepad_surface")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Clean Two-Tab Switcher at top of the Journal Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        JournalTabPill(
                            modifier = Modifier.weight(1f),
                            title = user1Name,
                            inkColor = user1InkColor,
                            isSelected = selectedTabIndex == 0,
                            testTag = "journal_tab_user_1",
                            onClick = { selectedTabIndex = 0 }
                        )
                        JournalTabPill(
                            modifier = Modifier.weight(1f),
                            title = user2Name,
                            inkColor = user2InkColor,
                            isSelected = selectedTabIndex == 1,
                            testTag = "journal_tab_user_2",
                            onClick = { selectedTabIndex = 1 }
                        )
                    }

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    )

                    val lineColor = MaterialTheme.colorScheme.outline.copy(
                        alpha = if (isDark) 0.18f else 0.25f
                    )
                    val scrollState = rememberScrollState()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                                .height(680.dp)
                        ) {
                            val step = 32.dp.toPx()
                            var y = step * 1.4f
                            while (y < size.height) {
                                drawLine(
                                    color = lineColor,
                                    start = Offset(18.dp.toPx(), y),
                                    end = Offset(size.width - 18.dp.toPx(), y),
                                    strokeWidth = 0.75.dp.toPx()
                                )
                                y += step
                            }
                        }

                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { incomingValue ->
                                val editResult = applyNonDestructiveEdit(
                                    oldChars = activeTabChars,
                                    previousSelection = textFieldValue.selection,
                                    previousComposition = textFieldValue.composition,
                                    newFieldValue = incomingValue,
                                    activeUser = activeUser
                                )
                                updateActiveTabChars(editResult.updatedChars)
                                textFieldValue = TextFieldValue(
                                    text = editResult.updatedChars.asPlainString(),
                                    selection = editResult.updatedSelection,
                                    composition = if (editResult.preserveComposition) {
                                        incomingValue.composition
                                    } else {
                                        null
                                    }
                                )
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 15.5.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.Normal,
                                color = activeUserInkColor
                            ),
                            cursorBrush = SolidColor(activeUserInkColor),
                            visualTransformation = visualTransformation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(680.dp)
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                                .testTag("journal_notepad_input"),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (textFieldValue.text.isEmpty()) {
                                        Text(
                                            text = "Write here...",
                                            style = TextStyle(
                                                fontSize = 15.5.sp,
                                                lineHeight = 32.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalTabPill(
    modifier: Modifier = Modifier,
    title: String,
    inkColor: Color,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            inkColor.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = if (isSelected) {
            BorderStroke(1.2.dp, inkColor.copy(alpha = 0.6f))
        } else {
            null
        },
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(inkColor)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MinimalDateSearchPanel(
    selectedDateIso: String,
    currentTodayIso: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    logsByDate: Map<String, DailyDuoJournalLog>,
    onSelectDate: (String) -> Unit
) {
    val allDates = remember(logsByDate, currentTodayIso) {
        val set = linkedSetOf<String>()
        for (daysBack in 0..7) {
            set.add(DuoJournalPersistence.dateDaysAgoIso(daysBack))
        }
        set.addAll(logsByDate.keys)
        set.toList().sortedDescending()
    }

    val filteredDates = remember(allDates, searchQuery, logsByDate) {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        if (q.isEmpty()) {
            allDates
        } else {
            allDates.filter { dateIso ->
                val readable = DuoJournalPersistence.formatReadableDate(dateIso).lowercase(Locale.getDefault())
                dateIso.contains(q) || readable.contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .testTag("journal_date_search_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(text = "Search date (YYYY-MM-DD or MMM d)...", fontSize = 12.5.sp)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("journal_date_search_input"),
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        )

        val matchesIso = remember(searchQuery) {
            Regex("""^\d{4}-\d{2}-\d{2}$""").matches(searchQuery.trim())
        }
        if (matchesIso && !filteredDates.contains(searchQuery.trim())) {
            val customIso = searchQuery.trim()
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectDate(customIso) }
            ) {
                Text(
                    text = customIso,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredDates, key = { it }) { dateIso ->
                val isSelected = dateIso == selectedDateIso
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectDate(dateIso) }
                        .testTag("journal_date_result_$dateIso")
                ) {
                    Text(
                        text = if (dateIso == currentTodayIso) {
                            "Today"
                        } else {
                            DuoJournalPersistence.formatReadableDate(dateIso)
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
