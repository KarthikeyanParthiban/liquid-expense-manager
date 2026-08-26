package com.expensemanager.app.ml

import android.content.Context
import com.expensemanager.app.core.model.Category
import org.json.JSONObject
import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * High-performance, privacy-first on-device Machine Learning Merchant & Transaction Classifier.
 * Uses a precomputed Log-Linear Softmax model with character subword n-grams and word tokens
 * to classify long-tail and unseen Indian merchants into 13 expense categories.
 */
object OnDeviceMerchantClassifier {

    data class PredictionResult(
        val category: Category,
        val confidence: Float,
        val probabilities: Map<Category, Float>
    )

    private var isInitialized = false
    private lateinit var categories: List<Category>
    private lateinit var wordVocab: Map<String, Int>
    private lateinit var wordIdf: FloatArray
    private lateinit var charVocab: Map<String, Int>
    private lateinit var charIdf: FloatArray
    private lateinit var weights: Array<FloatArray>
    private lateinit var intercept: FloatArray
    private var totalFeatures = 0

    @Synchronized
    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            val assetStream = context.assets.open("models/merchant_classifier_weights.json")
            loadFromStream(assetStream)
        } catch (e: Exception) {
            // Log and gracefully handle initialization failure
            e.printStackTrace()
        }
    }

    @Synchronized
    fun loadFromStream(inputStream: InputStream) {
        val jsonString = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val json = JSONObject(jsonString)

        val catArray = json.getJSONArray("categories")
        val catList = mutableListOf<Category>()
        for (i in 0 until catArray.length()) {
            val name = catArray.getString(i)
            val cat = try {
                Category.valueOf(name)
            } catch (e: Exception) {
                Category.OTHERS
            }
            catList.add(cat)
        }
        categories = catList

        val wVocabJson = json.getJSONObject("word_vocab")
        val wVocab = HashMap<String, Int>(wVocabJson.length())
        val wKeys = wVocabJson.keys()
        while (wKeys.hasNext()) {
            val key = wKeys.next()
            wVocab[key] = wVocabJson.getInt(key)
        }
        wordVocab = wVocab

        val wIdfArray = json.getJSONArray("word_idf")
        wordIdf = FloatArray(wIdfArray.length()) { i -> wIdfArray.getDouble(i).toFloat() }

        val cVocabJson = json.getJSONObject("char_vocab")
        val cVocab = HashMap<String, Int>(cVocabJson.length())
        val cKeys = cVocabJson.keys()
        while (cKeys.hasNext()) {
            val key = cKeys.next()
            cVocab[key] = cVocabJson.getInt(key)
        }
        charVocab = cVocab

        val cIdfArray = json.getJSONArray("char_idf")
        charIdf = FloatArray(cIdfArray.length()) { i -> cIdfArray.getDouble(i).toFloat() }

        totalFeatures = wordVocab.size + charVocab.size

        val weightsArray = json.getJSONArray("weights")
        weights = Array(weightsArray.length()) { i ->
            val row = weightsArray.getJSONArray(i)
            FloatArray(row.length()) { j -> row.getDouble(j).toFloat() }
        }

        val interceptArray = json.getJSONArray("intercept")
        intercept = FloatArray(interceptArray.length()) { i -> interceptArray.getDouble(i).toFloat() }

        isInitialized = true
    }

    /**
     * Performs instant (<1ms) inference on the input merchant name or description.
     */
    fun predict(text: String?): PredictionResult? {
        if (!isInitialized || text.isNullOrBlank()) return null

        val clean = cleanText(text)
        if (clean.isBlank()) return null

        val featureVector = extractFeatures(clean)
        if (featureVector.isEmpty()) return null

        val nClasses = minOf(categories.size, minOf(intercept.size, weights.size))
        if (nClasses == 0) return null
        val logits = FloatArray(nClasses)

        var maxLogit = Float.NEGATIVE_INFINITY
        for (c in 0 until nClasses) {
            var dot = intercept[c]
            val classWeights = weights[c]
            for ((featIdx, featVal) in featureVector) {
                if (featIdx < classWeights.size) {
                    dot += classWeights[featIdx] * featVal
                }
            }
            logits[c] = dot
            if (dot > maxLogit) {
                maxLogit = dot
            }
        }

        // Softmax with numerical stability
        var sumExp = 0.0f
        val expLogits = FloatArray(nClasses)
        for (c in 0 until nClasses) {
            val e = exp(logits[c] - maxLogit)
            expLogits[c] = e
            sumExp += e
        }

        val probabilities = mutableMapOf<Category, Float>()
        var bestIdx = 0
        var bestProb = -1.0f

        for (c in 0 until nClasses) {
            val prob = if (sumExp > 0.0f) expLogits[c] / sumExp else 0.0f
            val cat = categories[c]
            probabilities[cat] = prob
            if (prob > bestProb) {
                bestProb = prob
                bestIdx = c
            }
        }

        return PredictionResult(
            category = categories[bestIdx],
            confidence = bestProb,
            probabilities = probabilities
        )
    }

    private fun cleanText(raw: String): String {
        return raw.lowercase()
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun extractFeatures(cleaned: String): List<Pair<Int, Float>> {
        val wordOffset = 0
        val charOffset = wordVocab.size

        val termCounts = HashMap<Int, Int>()

        // 1. Word n-grams (1-2)
        val tokens = cleaned.split(" ").filter { it.isNotBlank() }
        for (i in tokens.indices) {
            // Unigram
            val unigram = tokens[i]
            wordVocab[unigram]?.let { idx ->
                val featId = wordOffset + idx
                termCounts[featId] = (termCounts[featId] ?: 0) + 1
            }

            // Bigram
            if (i + 1 < tokens.size) {
                val bigram = "${tokens[i]} ${tokens[i + 1]}"
                wordVocab[bigram]?.let { idx ->
                    val featId = wordOffset + idx
                    termCounts[featId] = (termCounts[featId] ?: 0) + 1
                }
            }
        }

        // 2. Character n-grams (3-4) with word boundary padding
        for (token in tokens) {
            val padded = " $token "
            val len = padded.length
            // 3-grams
            for (i in 0..len - 3) {
                val gram3 = padded.substring(i, i + 3)
                charVocab[gram3]?.let { idx ->
                    val featId = charOffset + idx
                    termCounts[featId] = (termCounts[featId] ?: 0) + 1
                }
            }
            // 4-grams
            for (i in 0..len - 4) {
                val gram4 = padded.substring(i, i + 4)
                charVocab[gram4]?.let { idx ->
                    val featId = charOffset + idx
                    termCounts[featId] = (termCounts[featId] ?: 0) + 1
                }
            }
        }

        if (termCounts.isEmpty()) return emptyList()

        // Apply sublinear TF-IDF: (1 + ln(tf)) * idf
        var sumSquares = 0.0f
        val rawFeatures = ArrayList<Pair<Int, Float>>(termCounts.size)

        for ((featId, count) in termCounts) {
            val idf = if (featId < charOffset) {
                wordIdf[featId]
            } else {
                charIdf[featId - charOffset]
            }
            val tf = 1.0f + ln(count.toFloat())
            val tfidf = tf * idf
            rawFeatures.add(Pair(featId, tfidf))
            sumSquares += tfidf * tfidf
        }

        // L2 Normalization
        val norm = max(sqrt(sumSquares), 1e-6f)
        return rawFeatures.map { Pair(it.first, it.second / norm) }
    }
}
