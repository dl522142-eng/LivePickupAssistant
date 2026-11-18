package com.livepickup.assistant.parser

import android.graphics.Rect
import com.livepickup.assistant.model.BrandCatalog
import kotlin.math.abs
import kotlin.math.hypot

data class SemanticNode(
    val text: String,
    val bounds: Rect
)

data class ParsedOrderResult(
    val brandName: String,
    val pickupCode: String
)

object PickupCodeParser {

    private val ANCHOR_KEYWORDS = listOf(
        "取餐码", "取茶码", "取餐号", "取单号", "取货码", "提货码",
        "排队号", "出餐号", "核销码", "取杯码", "凭码", "叫号", "餐号"
    )

    // Strict pickup code pattern: e.g. A088, A9821, T089, E-512, 8204, 058, 312, B026
    private val CODE_ONLY_REGEX = Regex("^[A-Za-z]{0,2}-?\d{2,5}$")

    // Inline pattern inside notifications or combined text nodes
    private val INLINE_PATTERNS = listOf(
        Regex("(?:取餐码|取茶码|取餐号|取单号|取货码|提货码|餐号|排队号|核销码)[:：\s]*([A-Za-z]{0,2}-?\d{2,5})"),
        Regex("请凭[:：\s]*([A-Za-z]{0,2}-?\d{2,5})\s*(?:前往|到|取餐|领取)"),
        Regex("【([^】]{2,12})】.*?(?:取餐码|取茶码|取餐号|取单号)[:：\s]*([A-Za-z]{0,2}-?\d{2,5})")
    )

    fun parseFromNotificationText(title: String, content: String, packageName: String): ParsedOrderResult? {
        val fullText = "$title $content"
        if (ANCHOR_KEYWORDS.none { fullText.contains(it) } && !fullText.contains("出餐") && !fullText.contains("取餐")) {
            return null
        }

        val detectedBrand = BrandCatalog.findBrandByNameOrText(fullText)?.name
            ?: inferBrandFromPackageOrTitle(packageName, title, fullText)

        for (regex in INLINE_PATTERNS) {
            val match = regex.find(fullText)
            if (match != null) {
                val codeCandidate = match.groupValues.last().trim()
                if (isValidPickupCode(codeCandidate)) {
                    return ParsedOrderResult(detectedBrand, codeCandidate.uppercase())
                }
            }
        }
        return null
    }

    /**
     * Cross-node semantic anchor + visual large-font OCR parser:
     * 1. Detects brand name across all nodes.
     * 2. Checks inline anchor+code matches first.
     * 3. Associates semantic anchor nodes ("取餐码", "取茶码", etc.) with spatially closest code nodes.
     * 4. Fallback for Visual Large-Font OCR: picks the largest height numeric/alphanumeric code node on screen.
     */
    fun parseFromNodes(nodes: List<SemanticNode>, allowLargeFontFallback: Boolean = false): ParsedOrderResult? {
        if (nodes.isEmpty()) return null

        val allTextJoined = nodes.joinToString(" ") { it.text }
        val detectedBrand = BrandCatalog.findBrandByNameOrText(allTextJoined)?.name ?: "严选商户"

        // Step 1: Check inline match in any single node
        for (node in nodes) {
            for (regex in INLINE_PATTERNS) {
                val m = regex.find(node.text)
                if (m != null) {
                    val code = m.groupValues.last().trim()
                    if (isValidPickupCode(code)) {
                        return ParsedOrderResult(detectedBrand, code.uppercase())
                    }
                }
            }
        }

        // Step 2: Cross-node semantic anchor association
        val anchorNodes = nodes.filter { node ->
            ANCHOR_KEYWORDS.any { kw -> node.text.contains(kw) }
        }
        val candidateCodeNodes = nodes.filter { node ->
            val cleaned = node.text.trim()
            isValidPickupCode(cleaned)
        }

        if (anchorNodes.isNotEmpty() && candidateCodeNodes.isNotEmpty()) {
            var bestPair: Pair<SemanticNode, Double>? = null
            for (anchor in anchorNodes) {
                for (cand in candidateCodeNodes) {
                    val dx = (cand.bounds.centerX() - anchor.bounds.centerX()).toDouble()
                    val dy = (cand.bounds.centerY() - anchor.bounds.centerY()).toDouble()
                    // Prefer candidates below or to the right of the anchor label
                    val penalty = if (dy < -120) 2.2 else 1.0
                    val dist = hypot(dx, dy) * penalty
                    if (bestPair == null || dist < bestPair.second) {
                        bestPair = cand to dist
                    }
                }
            }
            if (bestPair != null && bestPair.second < 950.0) {
                return ParsedOrderResult(detectedBrand, bestPair.first.text.trim().uppercase())
            }
        }

        // Step 3: Visual Large-Font OCR Fallback (e.g. Manual Tile Scan or Mini-Program big pickup number)
        if (allowLargeFontFallback && candidateCodeNodes.isNotEmpty()) {
            val largestNode = candidateCodeNodes.maxByOrNull { it.bounds.height() }
            if (largestNode != null && largestNode.bounds.height() >= 42) {
                return ParsedOrderResult(detectedBrand, largestNode.text.trim().uppercase())
            }
        }

        return null
    }

    private fun isValidPickupCode(text: String): Boolean {
        val cleaned = text.trim()
        if (!CODE_ONLY_REGEX.matches(cleaned)) return false
        // Exclude common years or 24h time like 2025, 2026
        if (cleaned in listOf("2024", "2025", "2026", "2027", "1000", "0000")) return false
        return true
    }

    private fun inferBrandFromPackageOrTitle(pkg: String, title: String, fullText: String): String {
        val bracketMatch = Regex("【([^】]{2,10})】").find(fullText)
        if (bracketMatch != null) {
            val inside = bracketMatch.groupValues[1]
            if (!inside.contains("微信") && !inside.contains("通知") && !inside.contains("提醒")) {
                return inside
            }
        }
        return when {
            pkg.contains("sankuai.meituan") -> "美团外卖"
            pkg.contains("taobao") || pkg.contains("ele") -> "淘宝闪购"
            title.isNotBlank() && title != "服务通知" && title.length <= 10 -> title
            else -> "门店取餐"
        }
    }
}
