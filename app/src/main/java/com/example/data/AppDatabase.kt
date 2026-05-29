package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Project::class, Donation::class, UserWallet::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bonyanDao(): BonyanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bonyan_database"
                )
                .addCallback(BonyanDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class BonyanDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.bonyanDao()
                    // Pre-populate Wallets
                    dao.insertWallet(UserWallet("donor", 150000.0)) // 150,000 SAR
                    dao.insertWallet(UserWallet("contractor", 250000.0)) // 250,000 SAR

                    // Pre-populate sample issues/projects
                    dao.insertProject(
                        Project(
                            title = "تعبيد وصيانة طريق ريادي حيوي",
                            description = "صيانة وتعبيد الحفر العميقة في طريق الملك فهد الفرعي الذي يربط بين الأحياء السكنية والمنطقة الزراعية بطول 2 كم لحماية المركبات.",
                            category = "طرق",
                            latitude = 24.7136, // Riyadh
                            longitude = 46.6753,
                            requiredAmount = 40000.0,
                            collectedAmount = 15000.0,
                            status = "قيد_التمويل",
                            reporterName = "صالح السعيد",
                            imageKey = "road_pothole"
                        )
                    )

                    dao.insertProject(
                        Project(
                            title = "تجهيز قسم الطوارئ في مستشفى الخير",
                            description = "تأمين وتجهيز أجهزة قياس الضغط، أكسجين، وأسرة عناية متوسطة لقسم الطوارئ لخدمة أهالي القرى المحيطة.",
                            category = "مستشفيات",
                            latitude = 21.4858, // Jeddah
                            longitude = 39.1925,
                            requiredAmount = 90000.0,
                            collectedAmount = 90000.0,
                            status = "مكتمل_التمويل", // Ready for claims
                            reporterName = "د. أسماء الحربي",
                            imageKey = "hospital_emergency"
                        )
                    )

                    dao.insertProject(
                        Project(
                            title = "تركيب مضخات ري تعمل بالطاقة الشمسية",
                            description = "مبادرة لإمداد 10 مزارع صغيرة بأجهزة مضخات ري شمسية لتذليل عقبات شح الوقود واستدامة الإنتاج الزراعي المحلي.",
                            category = "زراعة",
                            latitude = 26.2172,
                            longitude = 50.1971,
                            requiredAmount = 60000.0,
                            collectedAmount = 25000.0,
                            status = "قيد_التمويل",
                            reporterName = "أبو فهد الأحسائي",
                            imageKey = "agri_solar"
                        )
                    )

                    dao.insertProject(
                        Project(
                            title = "ترميم وتوسعة الفصول المدرسية المتهالكة",
                            description = "ترميم وتجهيز مدرستين تعليميتين للمرحلة الأساسية، صيانة دورات المياه والأسلاك الكهربائية وتوفير طاولات دراسية جديدة لـ 60 طالباً.",
                            category = "تعليم",
                            latitude = 24.4672, // Madinah
                            longitude = 39.6111,
                            requiredAmount = 50000.0,
                            collectedAmount = 50000.0,
                            status = "مكتمل_التمويل", // Ready for claims
                            reporterName = "أ. منيرة المطلق",
                            imageKey = "class_repair"
                        )
                    )
                }
            }
        }
    }
}
