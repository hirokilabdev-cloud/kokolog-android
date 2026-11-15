package com.kokolog.app

/** 「その後のココロ」は回復・改善を意味しない。入力された変化だけを時系列で返す。 */
data class AfterHeartStory(
    val original: EmotionLog,
    val events: List<EmotionChangeLog>
)

object MonthlyAfterHeartPresentation {
    /** ReportAnalyzer v1.1 と集計範囲を揃える。元の記録が当月、変化も当月中のみ。 */
    fun stories(
        logs: List<EmotionLog>,
        changeLogs: List<EmotionChangeLog>,
        period: ReportPeriod
    ): List<AfterHeartStory> {
        val originals = logs.filter { it.id > 0 && it.createdAt >= period.startMillis && it.createdAt < period.endExclusiveMillis }
            .associateBy { it.id }
        return changeLogs.asSequence()
            .filter { change ->
                val original = originals[change.originalLogId]
                original != null && change.createdAt >= original.createdAt && change.createdAt < period.endExclusiveMillis
            }
            .groupBy { it.originalLogId }
            .mapNotNull { (id, events) ->
                originals[id]?.let { original ->
                    AfterHeartStory(original, events.sortedWith(compareBy<EmotionChangeLog> { it.createdAt }.thenBy { it.id }))
                }
            }
            .sortedByDescending { it.original.createdAt }
    }

    fun ignoredLogs(logs: List<EmotionLog>, period: ReportPeriod): List<EmotionLog> =
        logs.filter { it.id > 0 && it.isIgnored && it.createdAt >= period.startMillis && it.createdAt < period.endExclusiveMillis }
            .sortedByDescending { it.createdAt }
}
