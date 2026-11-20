package com.example.gesturerecognition

enum class CoreGestureType(
    val titleAr: String,
    val titleEn: String,
    val descAr: String,
    val descEn: String,
    val defaultAction: GestureActionType,
    val badgeSymbol: String
) {
    THUMBS_UP(
        titleAr = "رفع الإبهام",
        titleEn = "THUMBS UP",
        descAr = "رفع الإبهام لأعلى مع ثني باقي الأصابع لتشغيل الفلاش.",
        descEn = "Raise thumb upward with fingers curled to turn flashlight on.",
        defaultAction = GestureActionType.FLASH_ON,
        badgeSymbol = "👍"
    ),
    OPEN_PALM(
        titleAr = "فتح كف اليد",
        titleEn = "OPEN PALM",
        descAr = "فرد جميع الأصابع الخمسة لإيقاف الفلاش فوراً.",
        descEn = "Open all five fingers to turn flashlight off.",
        defaultAction = GestureActionType.FLASH_OFF,
        badgeSymbol = "✋"
    ),
    PEACE_SIGN(
        titleAr = "علامة النصر",
        titleEn = "PEACE SIGN",
        descAr = "رفع السبابة والوسطى لتشغيل أو إيقاف الوسائط.",
        descEn = "Raise index and middle fingers to toggle media playback.",
        defaultAction = GestureActionType.MEDIA_PLAY_PAUSE,
        badgeSymbol = "✌️"
    ),
    FIST(
        titleAr = "قبض اليد",
        titleEn = "FIST",
        descAr = "إغلاق جميع الأصابع لإيقاف التعرّف مؤقتاً.",
        descEn = "Close all fingers into a fist to pause recognition.",
        defaultAction = GestureActionType.PAUSE_RECOGNITION,
        badgeSymbol = "✊"
    ),
    ONE_FINGER(
        titleAr = "رفع إصبع واحد",
        titleEn = "ONE FINGER",
        descAr = "رفع السبابة لأعلى لزيادة مستوى الصوت.",
        descEn = "Point index finger upward to increase media volume.",
        defaultAction = GestureActionType.VOLUME_UP,
        badgeSymbol = "☝️"
    ),
    DOWN_GESTURE(
        titleAr = "إشارة لأسفل",
        titleEn = "DOWN GESTURE",
        descAr = "توجيه السبابة لأسفل لخفض مستوى الصوت.",
        descEn = "Point index finger downward to lower media volume.",
        defaultAction = GestureActionType.VOLUME_DOWN,
        badgeSymbol = "👇"
    ),
    SWIPE_RIGHT(
        titleAr = "سحب لليمين",
        titleEn = "SWIPE RIGHT",
        descAr = "تحريك اليد بسرعة لليمين للانتقال للعنصر التالي.",
        descEn = "Sweep hand right to jump to the next media or slide.",
        defaultAction = GestureActionType.MEDIA_NEXT,
        badgeSymbol = "👉"
    ),
    SWIPE_LEFT(
        titleAr = "سحب لليسار",
        titleEn = "SWIPE LEFT",
        descAr = "تحريك اليد بسرعة لليسار للرجوع للعنصر السابق.",
        descEn = "Sweep hand left to return to the previous media or slide.",
        defaultAction = GestureActionType.MEDIA_PREV,
        badgeSymbol = "👈"
    ),
    HORNS(
        titleAr = "علامة القرون",
        titleEn = "HORNS",
        descAr = "رفع السبابة والخنصر لفتح تطبيق يختاره المستخدم.",
        descEn = "Raise index and pinky fingers to launch your chosen app.",
        defaultAction = GestureActionType.OPEN_APP,
        badgeSymbol = "🤘"
    );

    companion object {
        fun fromKey(key: String): CoreGestureType? =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
    }
}

enum class GestureActionType(
    val labelAr: String,
    val labelEn: String,
    val categoryAr: String,
    val categoryEn: String
) {
    FLASH_ON("تشغيل الفلاش", "Flashlight On", "العتاد", "Hardware"),
    FLASH_OFF("إيقاف الفلاش", "Flashlight Off", "العتاد", "Hardware"),
    TOGGLE_FLASH("تبديل الفلاش", "Toggle Flashlight", "العتاد", "Hardware"),
    MEDIA_PLAY_PAUSE("تشغيل / إيقاف الموسيقى", "Play / Pause Media", "الوسائط", "Media"),
    MEDIA_NEXT("التالي (وسائط / عرض)", "Next Item / Track", "الوسائط", "Media"),
    MEDIA_PREV("السابق (وسائط / عرض)", "Previous Item / Track", "الوسائط", "Media"),
    VOLUME_UP("رفع الصوت", "Volume Up", "الصوت", "Audio"),
    VOLUME_DOWN("خفض الصوت", "Volume Down", "الصوت", "Audio"),
    OPEN_APP("فتح تطبيق مخصص", "Launch Selected App", "التطبيقات", "Apps"),
    PAUSE_RECOGNITION("إيقاف التعرّف مؤقتاً", "Pause Recognition", "النظام", "System"),
    OPEN_MEDIA_STAGE("فتح منصة العرض اللالمسية", "Open Media Stage", "داخل التطبيق", "In-App"),
    OPEN_HISTORY("فتح سجل النشاطات", "Open History Screen", "داخل التطبيق", "In-App"),
    OPEN_SETTINGS("فتح الإعدادات", "Open Settings Screen", "داخل التطبيق", "In-App"),
    GLOBAL_BACK("رجوع عالمي (إمكانية الوصول)", "Global Back (Accessibility)", "إمكانية الوصول", "Accessibility"),
    GLOBAL_HOME("الشاشة الرئيسية (إمكانية الوصول)", "Global Home (Accessibility)", "إمكانية الوصول", "Accessibility"),
    NONE("بدون أمر", "No Action", "عام", "General");

    companion object {
        fun fromKey(key: String): GestureActionType =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: NONE
    }
}

data class GestureSeedItem(
    val gestureKey: String,
    val displayNameAr: String,
    val displayNameEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val actionKey: String
)

object DefaultGestureSeed {
    fun defaultCoreGestures(): List<GestureSeedItem> = CoreGestureType.entries.map {
        GestureSeedItem(
            gestureKey = it.name,
            displayNameAr = it.titleAr,
            displayNameEn = it.titleEn,
            descriptionAr = it.descAr,
            descriptionEn = it.descEn,
            actionKey = it.defaultAction.name
        )
    }
}
