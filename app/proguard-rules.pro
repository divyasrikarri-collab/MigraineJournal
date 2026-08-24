# Room generates code reflected over at runtime by the generated database impl.
-keep class com.divyasrikarri.migrainejournal.data.local.** { *; }

# WorkManager instantiates workers by class name.
-keep class com.divyasrikarri.migrainejournal.notification.ReminderWorker { *; }
