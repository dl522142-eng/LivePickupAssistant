package com.livepickup.assistant.model

import android.content.Context
import android.graphics.*
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

data class BrandInfo(
    val id: String,
    val name: String,
    val defaultCode: String,
    val primaryColor: Int,
    val secondaryColor: Int,
    val badgeText: String,
    val keywords: List<String>
)

object BrandCatalog {
    private const val PREFS_CUSTOM_LOGOS = "custom_brand_logos"

    val ALL_BRANDS: List<BrandInfo> = listOf(
        BrandInfo("luckin", "瑞幸咖啡", "A9821", Color.parseColor("#19398A"), Color.parseColor("#FFFFFF"), "瑞", listOf("瑞幸", "luckin")),
        BrandInfo("chagee", "霸王茶姬", "T089", Color.parseColor("#D9232E"), Color.parseColor("#FFFFFF"), "茶", listOf("霸王茶姬", "chagee")),
        BrandInfo("heytea", "喜茶", "8204", Color.parseColor("#1E1E1E"), Color.parseColor("#FFFFFF"), "喜", listOf("喜茶", "heytea")),
        BrandInfo("mcdonalds", "麦当劳", "312", Color.parseColor("#DB0007"), Color.parseColor("#FFBC0D"), "M", listOf("麦当劳", "金拱门", "mcdonald")),
        BrandInfo("meituan", "美团外卖", "7892", Color.parseColor("#FFD000"), Color.parseColor("#222222"), "美", listOf("美团", "美团外卖")),
        BrandInfo("taobao_flash", "淘宝闪购", "E-512", Color.parseColor("#FF5500"), Color.parseColor("#FFFFFF"), "淘", listOf("淘宝闪购", "淘宝", "饿了么")),
        BrandInfo("guming", "古茗", "058", Color.parseColor("#222222"), Color.parseColor("#FFFFFF"), "古", listOf("古茗", "guming")),
        BrandInfo("starbucks", "星巴克", "B026", Color.parseColor("#00704A"), Color.parseColor("#FFFFFF"), "星", listOf("星巴克", "starbucks", "专星送", "啡快")),
        BrandInfo("molinaibai", "茉莉奶白", "068", Color.parseColor("#5B8C5A"), Color.parseColor("#FFFFFF"), "茉", listOf("茉莉奶白")),
        BrandInfo("mixue", "蜜雪冰城", "108", Color.parseColor("#E60012"), Color.parseColor("#FFFFFF"), "蜜", listOf("蜜雪冰城")),
        BrandInfo("chabaidao", "茶百道", "C215", Color.parseColor("#0052D9"), Color.parseColor("#FFFFFF"), "茶", listOf("茶百道")),
        BrandInfo("cotti", "库迪咖啡", "K082", Color.parseColor("#9B1B30"), Color.parseColor("#FFFFFF"), "库", listOf("库迪", "cotti")),
        BrandInfo("kfc", "肯德基", "K109", Color.parseColor("#E4002B"), Color.parseColor("#FFFFFF"), "KFC", listOf("肯德基", "kfc")),
        BrandInfo("nayuki", "奈雪的茶", "N066", Color.parseColor("#78B833"), Color.parseColor("#FFFFFF"), "奈", listOf("奈雪")),
        BrandInfo("aysh", "沪上阿姨", "H301", Color.parseColor("#F36C21"), Color.parseColor("#FFFFFF"), "沪", listOf("沪上阿姨")),
        BrandInfo("burgerking", "汉堡王", "B812", Color.parseColor("#D62300"), Color.parseColor("#F5EBDC"), "汉", listOf("汉堡王")),
        BrandInfo("manner", "Manner咖啡", "M204", Color.parseColor("#C8102E"), Color.parseColor("#FFFFFF"), "M", listOf("manner", "Manner")),
        BrandInfo("yidiandian", "1点点", "A116", Color.parseColor("#006837"), Color.parseColor("#FFFFFF"), "1点", listOf("1点点", "一点点")),
        BrandInfo("coco", "CoCo都可", "D092", Color.parseColor("#FF6600"), Color.parseColor("#FFFFFF"), "Co", listOf("CoCo", "都可")),
        BrandInfo("shuyi", "书亦烧仙草", "S045", Color.parseColor("#C41230"), Color.parseColor("#FFFFFF"), "书", listOf("书亦")),
        BrandInfo("sexytea", "茶颜悦色", "Y520", Color.parseColor("#8B261D"), Color.parseColor("#FFFFFF"), "颜", listOf("茶颜悦色")),
        BrandInfo("tims", "Tims天好咖啡", "T401", Color.parseColor("#C8102E"), Color.parseColor("#FFFFFF"), "T", listOf("Tims", "天好咖啡")),
        BrandInfo("mstand", "M Stand", "MS18", Color.parseColor("#111111"), Color.parseColor("#FFFFFF"), "MS", listOf("M Stand", "mstand")),
        BrandInfo("lavazza", "Lavazza", "L033", Color.parseColor("#0A2240"), Color.parseColor("#FFFFFF"), "L", listOf("Lavazza", "拉瓦萨")),
        BrandInfo("linlee", "LINLEE林里", "L088", Color.parseColor("#0047BA"), Color.parseColor("#FFDE00"), "林", listOf("林里", "LINLEE")),
        BrandInfo("益禾堂", "益禾堂", "Y077", Color.parseColor("#009944"), Color.parseColor("#FFFFFF"), "益", listOf("益禾堂")),
        BrandInfo("tianlala", "甜啦啦", "T168", Color.parseColor("#F5A623"), Color.parseColor("#FFFFFF"), "甜", listOf("甜啦啦")),
        BrandInfo("7fen", "7分甜", "7021", Color.parseColor("#FFD100"), Color.parseColor("#222222"), "7", listOf("7分甜", "七分甜")),
        BrandInfo("leke", "乐乐茶", "L602", Color.parseColor("#E60012"), Color.parseColor("#FFFFFF"), "乐", listOf("乐乐茶")),
        BrandInfo("Wallace", "华莱士", "W118", Color.parseColor("#D9232E"), Color.parseColor("#FFFFFF"), "华", listOf("华莱士")),
        BrandInfo("tastien", "塔斯汀", "TS09", Color.parseColor("#E60012"), Color.parseColor("#FFFFFF"), "塔", listOf("塔斯汀")),
        BrandInfo("saizeriya", "萨莉亚", "SZ12", Color.parseColor("#007A33"), Color.parseColor("#FFFFFF"), "萨", listOf("萨莉亚")),
        BrandInfo("yungeng", "爷爷不泡茶", "YY06", Color.parseColor("#2D5A27"), Color.parseColor("#FFFFFF"), "茶", listOf("爷爷不泡茶"))
    )

    fun findBrandByNameOrText(text: String): BrandInfo? {
        val normalized = text.lowercase()
        return ALL_BRANDS.firstOrNull { brand ->
            brand.name.lowercase() == normalized ||
                brand.keywords.any { normalized.contains(it.lowercase()) }
        }
    }

    fun saveCustomLogoBase64(context: Context, brandId: String, dataUrlOrBase64: String) {
        val cleanBase64 = dataUrlOrBase64.substringAfter("base64,", dataUrlOrBase64)
        context.getSharedPreferences(PREFS_CUSTOM_LOGOS, Context.MODE_PRIVATE)
            .edit()
            .putString(brandId, cleanBase64)
            .apply()
    }

    fun removeCustomLogo(context: Context, brandId: String) {
        context.getSharedPreferences(PREFS_CUSTOM_LOGOS, Context.MODE_PRIVATE)
            .edit()
            .remove(brandId)
            .apply()
    }

    fun getCustomLogosJson(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_CUSTOM_LOGOS, Context.MODE_PRIVATE)
        val obj = JSONObject()
        for ((k, v) in prefs.all) {
            if (v is String && v.isNotEmpty()) {
                obj.put(k, "data:image/png;base64,$v")
            }
        }
        return obj.toString()
    }

    fun renderBrandBitmap(context: Context, brandName: String): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val matchedBrand = findBrandByNameOrText(brandName)
        val prefs = context.getSharedPreferences(PREFS_CUSTOM_LOGOS, Context.MODE_PRIVATE)
        val customBase64 = matchedBrand?.let { prefs.getString(it.id, null) }

        if (!customBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(customBase64, Base64.DEFAULT)
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (decoded != null) {
                    val shader = BitmapShader(
                        Bitmap.createScaledBitmap(decoded, size, size, true),
                        Shader.TileMode.CLAMP,
                        Shader.TileMode.CLAMP
                    )
                    paint.shader = shader
                    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
                    return bitmap
                }
            } catch (_: Exception) {
            }
        }

        // Default or "测试商户" circular icon rendering (matches 1000041154.jpg exactly)
        val bgColor = matchedBrand?.primaryColor ?: Color.parseColor("#0D6EFD")
        val fgColor = matchedBrand?.secondaryColor ?: Color.WHITE
        val label = when {
            brandName.contains("测试") -> "测"
            matchedBrand != null -> matchedBrand.badgeText
            brandName.isNotEmpty() -> brandName.take(1)
            else -> "取"
        }

        // Draw outer circle
        paint.style = Paint.Style.FILL
        paint.color = bgColor
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Draw subtle inner white ring like in 1000041154.jpg
        paint.style = Paint.Style.STROKE
        paint.color = fgColor
        paint.strokeWidth = 7f
        canvas.drawCircle(size / 2f, size / 2f, size * 0.30f, paint)

        // Draw center badge character
        paint.style = Paint.Style.FILL
        paint.color = fgColor
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = if (label.length > 1) 40f else 48f
        val fontMetrics = paint.fontMetrics
        val baseline = size / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(label, size / 2f, baseline, paint)

        return bitmap
    }
}
