package com.divyasrikarri.migrainejournal.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.divyasrikarri.migrainejournal.data.repository.InsightsCalculator
import com.divyasrikarri.migrainejournal.data.repository.MigraineRepository
import com.divyasrikarri.migrainejournal.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** Result of an export: the files written plus a ready-to-launch share intent. */
data class ExportResult(val intent: Intent, val fileNames: List<String>)

/**
 * Writes exports into app-specific external storage and hands back a share intent. Nothing
 * leaves the device unless the user picks a target in the system share sheet.
 */
class DataExporter(
    private val context: Context,
    private val repository: MigraineRepository
) {

    suspend fun exportCsv(): ExportResult = withContext(Dispatchers.IO) {
        val stamp = DateUtils.todayKey()
        val files = listOf(
            write("migraines_$stamp.csv", CsvExporter.migrainesCsv(repository.allMigrainesOnce())),
            write("daily_logs_$stamp.csv", CsvExporter.dailyLogsCsv(repository.allDailyLogsOnce())),
            write("food_entries_$stamp.csv", CsvExporter.foodEntriesCsv(repository.allFoodOnce()))
        )
        val uris = ArrayList(files.map(::uriFor))
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/csv"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, "Migraine Journal data export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ExportResult(intent, files.map { it.name })
    }

    /**
     * Doctor summary PDF. [days] bounds the reporting period; the default covers a year,
     * which is the span a neurologist appointment usually asks about.
     */
    suspend fun exportPdf(days: Int = 365): ExportResult = withContext(Dispatchers.IO) {
        val to = DateUtils.today()
        val from = to.minusDays((days - 1).toLong())
        val migraines = repository.getMigrainesBetween(from, to)
        val dailyLogs = repository.allDailyLogsOnce().filter { inRange(it.date, from, to) }
        val foods = repository.allFoodOnce().filter { inRange(it.date, from, to) }

        val input = PdfExporter.Input(
            from = from,
            toInclusive = to,
            migraines = migraines,
            dailyLogs = dailyLogs,
            correlations = InsightsCalculator.correlations(migraines, dailyLogs, foods)
        )

        val file = fileIn("migraine_summary_${DateUtils.todayKey()}.pdf")
        file.outputStream().use { PdfExporter().write(input, it) }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uriFor(file))
            putExtra(Intent.EXTRA_SUBJECT, "Migraine Journal summary")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ExportResult(intent, listOf(file.name))
    }

    private fun inRange(dateKey: String, from: LocalDate, toInclusive: LocalDate): Boolean {
        val date = DateUtils.parseKeyOrNull(dateKey) ?: return false
        return !date.isBefore(from) && !date.isAfter(toInclusive)
    }

    private fun write(name: String, content: String): File =
        fileIn(name).apply { writeText(content) }

    private fun fileIn(name: String): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "exports")
        dir.mkdirs()
        return File(dir, name)
    }

    private fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
