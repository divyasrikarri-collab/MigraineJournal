package com.divyasrikarri.migrainejournal.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.divyasrikarri.migrainejournal.data.local.DailyLog
import com.divyasrikarri.migrainejournal.data.local.MigraineEntry
import com.divyasrikarri.migrainejournal.data.model.Correlation
import com.divyasrikarri.migrainejournal.util.DateUtils
import java.io.OutputStream
import java.time.LocalDate
import java.util.Locale

// A4 at 72dpi, which is what PdfDocument's canvas units are.
private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 36f

private val TABLE_COLUMNS = listOf("Date", "Start", "Duration", "Pain", "Symptoms", "Medication")
private val TABLE_WEIGHTS = listOf(0.14f, 0.09f, 0.12f, 0.07f, 0.34f, 0.24f)

/**
 * Renders a doctor-ready summary with [android.graphics.pdf.PdfDocument]. Plain platform
 * drawing, no third-party PDF library — the layout is a header, a stats block, the
 * correlation callouts and a paginated migraine table.
 */
class PdfExporter {

    data class Input(
        val from: LocalDate,
        val toInclusive: LocalDate,
        val migraines: List<MigraineEntry>,
        val dailyLogs: List<DailyLog>,
        val correlations: List<Correlation>
    )

    fun write(input: Input, out: OutputStream) {
        val document = PdfDocument()
        val renderer = PageRenderer(document)

        renderer.startPage()
        renderer.title("Migraine Journal — Summary")
        renderer.subtitle(
            "${DateUtils.formatDate(input.from)} to ${DateUtils.formatDate(input.toInclusive)}"
        )
        renderer.gap(6f)

        renderer.sectionHeading("At a glance")
        summaryLines(input).forEach { renderer.bodyLine(it) }
        renderer.gap(8f)

        if (input.correlations.isNotEmpty()) {
            renderer.sectionHeading("Patterns in the logged data")
            input.correlations.forEach { correlation ->
                renderer.bodyLine("• ${correlation.headline}")
                renderer.mutedLine("   ${correlation.detail}")
            }
            renderer.mutedLine(
                "These are descriptive counts over self-reported entries, not clinical findings."
            )
            renderer.gap(8f)
        }

        renderer.sectionHeading("Migraine log")
        if (input.migraines.isEmpty()) {
            renderer.bodyLine("No migraines logged in this period.")
        } else {
            renderer.tableHeader(TABLE_COLUMNS, TABLE_WEIGHTS)
            input.migraines.sortedBy { it.startDateTime }.forEach { entry ->
                renderer.tableRow(rowFor(entry), TABLE_WEIGHTS)
            }
        }

        renderer.finishPage()
        document.writeTo(out)
        document.close()
    }

    private fun summaryLines(input: Input): List<String> {
        val migraines = input.migraines
        val durations = migraines.mapNotNull { it.durationMinutes }
        val daysInRange = (input.toInclusive.toEpochDay() - input.from.toEpochDay() + 1)
            .coerceAtLeast(1)
        val medicated = migraines.count { !it.medicationName.isNullOrBlank() }

        val lines = mutableListOf(
            "Migraines recorded: ${migraines.size} over $daysInRange days",
            "Days with a migraine: ${migraines.map { DateUtils.keyOf(it.startDateTime) }.distinct().size}"
        )
        if (migraines.isNotEmpty()) {
            lines += "Average pain level: %.1f / 10".format(
                Locale.US,
                migraines.map { it.painLevel }.average()
            )
            lines += "Highest pain level: ${migraines.maxOf { it.painLevel }} / 10"
            lines += "Attacks with aura: ${migraines.count { it.hasAura }}"
            lines += "Attacks where medication was taken: $medicated"
        }
        if (durations.isNotEmpty()) {
            lines += "Average duration: ${DateUtils.formatDuration(durations.average().toLong())}"
        }
        val sleep = input.dailyLogs.mapNotNull { it.sleepHours }
        if (sleep.isNotEmpty()) {
            lines += "Average sleep across %d logged nights: %.1f h".format(
                Locale.US, sleep.size, sleep.average()
            )
        }
        return lines
    }

    private fun rowFor(entry: MigraineEntry): List<String> = listOf(
        DateUtils.keyOf(entry.startDateTime),
        DateUtils.formatTime(entry.startDateTime),
        entry.durationMinutes?.let { DateUtils.formatDuration(it) } ?: "ongoing",
        entry.painLevel.toString(),
        entry.symptoms.joinToString(", ").ifEmpty { "—" },
        buildString {
            append(entry.medicationName.orEmpty().ifEmpty { "—" })
            entry.medicationEffectiveness?.let { append(" (rated $it/5)") }
        }
    )

    /** Draws sequential blocks onto A4 pages, starting a new page when the cursor runs out. */
    private class PageRenderer(private val document: PdfDocument) {

        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var cursorY = 0f
        private var pageNumber = 0

        private val titlePaint = paint(20f, bold = true)
        private val subtitlePaint = paint(12f, color = Color.DKGRAY)
        private val headingPaint = paint(13f, bold = true)
        private val bodyPaint = paint(10.5f)
        private val mutedPaint = paint(9f, color = Color.DKGRAY)
        private val tableHeaderPaint = paint(9.5f, bold = true)
        private val rulePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.6f
        }

        fun startPage() {
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = document.startPage(info)
            canvas = page?.canvas
            cursorY = MARGIN
            if (pageNumber > 1) {
                draw("Migraine Journal — continued", MARGIN, mutedPaint)
                cursorY += 14f
            }
        }

        fun finishPage() {
            page?.let { current ->
                canvas?.drawText(
                    "Page $pageNumber",
                    PAGE_WIDTH - MARGIN - 40f,
                    PAGE_HEIGHT - MARGIN / 2,
                    mutedPaint
                )
                document.finishPage(current)
            }
            page = null
            canvas = null
        }

        fun title(text: String) = block(text, titlePaint, 26f)

        fun subtitle(text: String) = block(text, subtitlePaint, 16f)

        fun sectionHeading(text: String) {
            ensureSpace(30f)
            cursorY += 6f
            draw(text, MARGIN, headingPaint)
            cursorY += 6f
            canvas?.drawLine(MARGIN, cursorY, PAGE_WIDTH - MARGIN, cursorY, rulePaint)
            cursorY += 12f
        }

        fun bodyLine(text: String) = wrapped(text, bodyPaint, 14f)

        fun mutedLine(text: String) = wrapped(text, mutedPaint, 12f)

        fun gap(height: Float) {
            cursorY += height
        }

        fun tableHeader(columns: List<String>, weights: List<Float>) {
            ensureSpace(24f)
            drawColumns(columns, weights, tableHeaderPaint)
            cursorY += 4f
            canvas?.drawLine(MARGIN, cursorY, PAGE_WIDTH - MARGIN, cursorY, rulePaint)
            cursorY += 10f
        }

        fun tableRow(cells: List<String>, weights: List<Float>) {
            ensureSpace(20f) { tableHeader(TABLE_COLUMNS, TABLE_WEIGHTS) }
            drawColumns(cells, weights, bodyPaint)
            cursorY += 14f
        }

        private fun drawColumns(cells: List<String>, weights: List<Float>, paint: Paint) {
            val available = PAGE_WIDTH - 2 * MARGIN
            var x = MARGIN
            cells.forEachIndexed { index, cell ->
                val width = available * weights[index]
                draw(ellipsize(cell, width - 6f, paint), x, paint)
                x += width
            }
        }

        private fun block(text: String, paint: Paint, advance: Float) {
            ensureSpace(advance)
            draw(text, MARGIN, paint)
            cursorY += advance
        }

        /** Draws [text] across as many lines as it needs, wrapping at the page width. */
        private fun wrapped(text: String, paint: Paint, advance: Float) {
            val maxWidth = PAGE_WIDTH - 2 * MARGIN
            var remaining = text
            while (remaining.isNotEmpty()) {
                val count = paint.breakText(remaining, true, maxWidth, null)
                    .coerceAtLeast(1)
                var take = count
                if (take < remaining.length) {
                    val lastSpace = remaining.lastIndexOf(' ', take)
                    if (lastSpace > 0) take = lastSpace
                }
                ensureSpace(advance)
                draw(remaining.substring(0, take).trimEnd(), MARGIN, paint)
                cursorY += advance
                remaining = remaining.substring(take).trimStart()
            }
        }

        private fun ellipsize(text: String, maxWidth: Float, paint: Paint): String {
            if (paint.measureText(text) <= maxWidth) return text
            var cut = paint.breakText(text, true, maxWidth - paint.measureText("…"), null)
            cut = cut.coerceAtLeast(1)
            return text.substring(0, cut).trimEnd() + "…"
        }

        private fun draw(text: String, x: Float, paint: Paint) {
            canvas?.drawText(text, x, cursorY, paint)
        }

        private inline fun ensureSpace(needed: Float, onNewPage: () -> Unit = {}) {
            if (cursorY + needed <= PAGE_HEIGHT - MARGIN) return
            finishPage()
            startPage()
            onNewPage()
        }

        private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = Paint().apply {
            isAntiAlias = true
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
    }
}
