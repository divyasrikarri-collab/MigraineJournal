package com.divyasrikarri.migrainejournal.data.repository

import com.divyasrikarri.migrainejournal.data.local.DailyLog
import com.divyasrikarri.migrainejournal.data.local.DailyLogDao
import com.divyasrikarri.migrainejournal.data.local.FoodEntry
import com.divyasrikarri.migrainejournal.data.local.FoodEntryDao
import com.divyasrikarri.migrainejournal.data.local.MigraineDao
import com.divyasrikarri.migrainejournal.data.local.MigraineEntry
import com.divyasrikarri.migrainejournal.data.model.HomeStats
import com.divyasrikarri.migrainejournal.data.model.TriggerFoods
import com.divyasrikarri.migrainejournal.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Single entry point to the local database for the whole app. */
class MigraineRepository(
    private val migraineDao: MigraineDao,
    private val dailyLogDao: DailyLogDao,
    private val foodEntryDao: FoodEntryDao
) {

    // --- Migraines -------------------------------------------------------------------

    fun observeAllMigraines(): Flow<List<MigraineEntry>> = migraineDao.observeAll()

    fun observeRecentMigraines(limit: Int = 3): Flow<List<MigraineEntry>> =
        migraineDao.observeRecent(limit)

    fun observeMigrainesOnDay(date: LocalDate): Flow<List<MigraineEntry>> =
        migraineDao.observeInRange(DateUtils.startOfDay(date), DateUtils.endOfDay(date))

    fun observeMigrainesBetween(from: LocalDate, toInclusive: LocalDate): Flow<List<MigraineEntry>> =
        migraineDao.observeInRange(DateUtils.startOfDay(from), DateUtils.endOfDay(toInclusive))

    suspend fun getMigrainesBetween(from: LocalDate, toInclusive: LocalDate): List<MigraineEntry> =
        migraineDao.getInRange(DateUtils.startOfDay(from), DateUtils.endOfDay(toInclusive))

    suspend fun getMigraine(id: Long): MigraineEntry? = migraineDao.getById(id)

    suspend fun saveMigraine(entry: MigraineEntry): Long =
        if (entry.id == 0L) migraineDao.insert(entry) else {
            migraineDao.update(entry)
            entry.id
        }

    suspend fun deleteMigraine(entry: MigraineEntry) = migraineDao.delete(entry)

    fun observeRecentMedications(limit: Int = 6): Flow<List<String>> =
        migraineDao.observeRecentMedications(limit)

    // --- Daily logs ------------------------------------------------------------------

    fun observeDailyLog(date: LocalDate): Flow<DailyLog?> =
        dailyLogDao.observeByDate(DateUtils.toKey(date))

    suspend fun getDailyLog(date: LocalDate): DailyLog? =
        dailyLogDao.getByDate(DateUtils.toKey(date))

    fun observeDailyLogsBetween(from: LocalDate, toInclusive: LocalDate): Flow<List<DailyLog>> =
        dailyLogDao.observeInRange(DateUtils.toKey(from), DateUtils.toKey(toInclusive))

    suspend fun saveDailyLog(log: DailyLog) = dailyLogDao.upsert(log)

    // --- Food ------------------------------------------------------------------------

    fun observeFoodForDay(date: LocalDate): Flow<List<FoodEntry>> =
        foodEntryDao.observeByDate(DateUtils.toKey(date))

    suspend fun getFoodForDay(date: LocalDate): List<FoodEntry> =
        foodEntryDao.getByDate(DateUtils.toKey(date))

    fun observeKnownFoods(limit: Int = 60): Flow<List<String>> =
        foodEntryDao.observeKnownFoods(limit)

    /** Adds a food, auto-tagging it against the static trigger seed list. */
    suspend fun addFood(date: LocalDate, foodName: String, mealTime: Long): Long {
        val category = TriggerFoods.categorize(foodName)
        return foodEntryDao.insert(
            FoodEntry(
                date = DateUtils.toKey(date),
                foodName = foodName.trim(),
                mealTime = mealTime,
                isCommonTrigger = category != null,
                triggerCategory = category
            )
        )
    }

    suspend fun deleteFood(entry: FoodEntry) = foodEntryDao.delete(entry)

    // --- Dashboard -------------------------------------------------------------------

    fun observeHomeStats(): Flow<HomeStats> {
        val today = DateUtils.today()
        val monthStart = today.withDayOfMonth(1)
        return combine(
            migraineDao.observeInRange(
                DateUtils.startOfDay(monthStart),
                DateUtils.endOfDay(today)
            ),
            migraineDao.observeLatestStart()
        ) { thisMonth, latestStart ->
            HomeStats(
                migrainesThisMonth = thisMonth.size,
                averagePainLevel = thisMonth.takeIf { it.isNotEmpty() }
                    ?.map { it.painLevel }
                    ?.average()
                    ?.toFloat(),
                currentStreakDays = latestStart?.let {
                    ChronoUnit.DAYS.between(DateUtils.localDateOf(it), DateUtils.today())
                        .toInt()
                        .coerceAtLeast(0)
                }
            )
        }
    }

    /** Days in the given month that have at least one migraine, mapped to their peak pain. */
    fun observeMonthPainByDay(month: LocalDate): Flow<Map<LocalDate, Int>> {
        val first = month.withDayOfMonth(1)
        val last = first.plusMonths(1).minusDays(1)
        return migraineDao.observeInRange(DateUtils.startOfDay(first), DateUtils.endOfDay(last))
            .map { entries ->
                entries.groupBy { DateUtils.localDateOf(it.startDateTime) }
                    .mapValues { (_, dayEntries) -> dayEntries.maxOf { it.painLevel } }
            }
    }

    /** Days in the given month that have a daily check-in with any content. */
    fun observeMonthCheckedInDays(month: LocalDate): Flow<Set<LocalDate>> {
        val first = month.withDayOfMonth(1)
        val last = first.plusMonths(1).minusDays(1)
        return dailyLogDao.observeInRange(DateUtils.toKey(first), DateUtils.toKey(last))
            .map { logs ->
                logs.filter { it.hasContent }
                    .mapNotNull { DateUtils.parseKeyOrNull(it.date) }
                    .toSet()
            }
    }

    // --- Export / data management ----------------------------------------------------

    suspend fun allMigrainesOnce(): List<MigraineEntry> = migraineDao.getAllOnce()

    suspend fun allDailyLogsOnce(): List<DailyLog> = dailyLogDao.getAllOnce()

    suspend fun allFoodOnce(): List<FoodEntry> = foodEntryDao.getAllOnce()

    suspend fun clearAllData() {
        foodEntryDao.deleteAll()
        dailyLogDao.deleteAll()
        migraineDao.deleteAll()
    }
}
