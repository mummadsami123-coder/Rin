package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.actions.RinAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiBrain {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun analyzeCommand(
        userInput: String,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        preferredLanguage: String = "English"
    ): ParsedIntent = withContext(Dispatchers.IO) {
        // First check fast local patterns for instant phone action response
        val localIntent = LocalIntentParser.parse(userInput)
        if (localIntent != null) {
            return@withContext localIntent
        }

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Local fallback with natural answer in preferred language
            return@withContext fallbackResponse(userInput, preferredLanguage)
        }

        try {
            val systemInstruction = """
                You are Mimi, a friendly, intelligent, and conversational personal Android voice assistant (RIN AI).
                You can converse naturally, answer any general question, explain concepts, tell stories, give advice, or execute native Android phone controls.
                
                The user has selected the speaking language: $preferredLanguage.
                You MUST respond naturally in $preferredLanguage.
                
                Classify user input and output a strict JSON object with this structure:
                {
                   "type": "ACTION" or "ANSWER",
                   "action": "OPEN_APP" | "CONTROL_FLASHLIGHT" | "CONTROL_VOLUME" | "CONTROL_MEDIA" | "MAKE_CALL" | "PREPARE_WHATSAPP" | "SEND_SMS" | "OPEN_SETTINGS" | "SET_REMINDER" | "READ_NOTIFICATIONS" | "NAVIGATE_HOME" | "NAVIGATE_BACK" | "SEARCH_WEB" | "GENERAL_ANSWER",
                   "params": {
                      "appName": "string",
                      "state": "on"|"off"|"toggle",
                      "direction": "up"|"down",
                      "command": "play"|"pause"|"next"|"previous",
                      "contact": "string",
                      "message": "string",
                      "settingsType": "wifi"|"bluetooth"|"sound"|"display"|"accessibility"|"notifications"|"main",
                      "title": "string",
                      "minutes": 10,
                      "query": "string"
                   },
                   "reply": "Natural conversational response in Mimi's persona written in $preferredLanguage"
                }
                
                If the user asks questions, chats, seeks information, jokes, or discusses topics, set type to "ANSWER", action to "GENERAL_ANSWER", and provide a clear, helpful, conversational response in "reply".
                Do not include markdown code block wrappers (like ```json), just output the raw JSON object.
            """.trimIndent()

            val contentsArray = JSONArray()

            // Add previous history context for conversational continuity
            conversationHistory.takeLast(6).forEach { (user, assist) ->
                contentsArray.put(
                    JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", user)))
                )
                contentsArray.put(
                    JSONObject().put("role", "model").put("parts", JSONArray().put(JSONObject().put("text", assist)))
                )
            }

            contentsArray.put(
                JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", userInput)))
            )

            val payload = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("responseMimeType", "application/json")
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiBrain", "Gemini API error ${response.code}: $responseBody")
                return@withContext fallbackResponse(userInput, preferredLanguage)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidateText = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            parseModelJson(candidateText, userInput, preferredLanguage)
        } catch (e: Exception) {
            Log.e("GeminiBrain", "Gemini request failed", e)
            fallbackResponse(userInput, preferredLanguage)
        }
    }

    private fun parseModelJson(rawJson: String, userInput: String, lang: String): ParsedIntent {
        try {
            val clean = rawJson.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(clean)
            val actionName = obj.optString("action", "GENERAL_ANSWER")
            val params = obj.optJSONObject("params") ?: JSONObject()
            val reply = obj.optString("reply", "")

            val action: RinAction = when (actionName) {
                "OPEN_APP" -> RinAction.OpenApp(params.optString("appName", "App"))
                "CONTROL_FLASHLIGHT" -> RinAction.ControlFlashlight(params.optString("state", "toggle"))
                "CONTROL_VOLUME" -> RinAction.ControlVolume(params.optString("direction", "up"))
                "CONTROL_MEDIA" -> RinAction.ControlMedia(params.optString("command", "play"))
                "MAKE_CALL" -> RinAction.MakeCall(params.optString("contact", ""))
                "PREPARE_WHATSAPP" -> RinAction.PrepareWhatsApp(params.optString("contact", ""), params.optString("message", ""))
                "SEND_SMS" -> RinAction.SendSms(params.optString("contact", ""), params.optString("message", ""))
                "OPEN_SETTINGS" -> RinAction.OpenSettings(params.optString("settingsType", "main"))
                "SET_REMINDER" -> RinAction.SetReminder(params.optString("title", "Reminder"), params.optInt("minutes", 10))
                "READ_NOTIFICATIONS" -> RinAction.ReadNotifications
                "NAVIGATE_HOME" -> RinAction.NavigateHome
                "NAVIGATE_BACK" -> RinAction.NavigateBack
                "SEARCH_WEB" -> RinAction.SearchWeb(params.optString("query", userInput))
                else -> RinAction.GeneralAnswer(reply.ifBlank { "I am listening and here to help you." })
            }

            return ParsedIntent(
                action = action,
                spokenReply = reply.ifBlank { "I'm right on it." }
            )
        } catch (e: Exception) {
            Log.e("GeminiBrain", "Failed parsing Gemini JSON: $rawJson", e)
            return fallbackResponse(userInput, lang)
        }
    }

    private fun fallbackResponse(userInput: String, preferredLanguage: String): ParsedIntent {
        val lower = userInput.lowercase()
        val isUrdu = preferredLanguage.contains("urdu", ignoreCase = true)
        val isHindi = preferredLanguage.contains("hindi", ignoreCase = true)

        val reply = when {
            lower.contains("kya haal hai") || lower.contains("how are you") || lower.contains("kaise ho") -> {
                when {
                    isUrdu -> "Main theek hoon! Aap batayein, aaj main aapki kya madad kar sakti hoon?"
                    isHindi -> "Main badhiya hoon! Batayein, aaj main aapki kya madad kar sakti hoon?"
                    else -> "I'm doing wonderful! How can I assist you today?"
                }
            }
            lower.contains("story") || lower.contains("kahani") -> {
                when {
                    isUrdu -> "Ek dafa ka zikr hai, ek pur-sukoon shehar mein ek chota sa robot rehta tha jo sab ki madad karta tha. Ek din usne dekha ke sab se bari taaqat dosti aur mehnat hai!"
                    isHindi -> "Ek samay ki baat hai, ek khoobsurat shahar mein ek samajhdar robot rehta tha jo sabhi ki madad karta tha. Usne seekha ki sabse badi shakti dosti aur lagan hai!"
                    else -> "Once upon a time in a city illuminated with neon stars, a friendly voice named Mimi helped people achieve their greatest aspirations every single day."
                }
            }
            lower.contains("who are you") || lower.contains("tum kaun ho") || lower.contains("apna taruf") -> {
                when {
                    isUrdu -> "Main Mimi hoon, aapki personal Android voice assistant. Main aapke phone ke kaam aasan banati hoon."
                    isHindi -> "Main Mimi hoon, aapki personal Android voice assistant."
                    else -> "I am Mimi, your personal Android voice assistant powered by RIN AI."
                }
            }
            else -> {
                when {
                    isUrdu -> "Ji, main sun rahi hoon. Aapka hukam sar ankhon par!"
                    isHindi -> "Ji, main sun rahi hoon. Batayein main kya karoon?"
                    else -> "I hear you clearly! I am right here to help."
                }
            }
        }

        return ParsedIntent(
            action = RinAction.GeneralAnswer(reply),
            spokenReply = reply
        )
    }
}

data class ParsedIntent(
    val action: RinAction,
    val spokenReply: String
)

object LocalIntentParser {
    fun parse(input: String): ParsedIntent? {
        val text = input.trim().lowercase()

        // Flashlight
        if (text.contains("flashlight on") || text.contains("torch on") || text.contains("batti jalao") || text.contains("light on")) {
            return ParsedIntent(RinAction.ControlFlashlight("on"), "Flashlight turned on.")
        }
        if (text.contains("flashlight off") || text.contains("torch off") || text.contains("batti bujhao") || text.contains("light off")) {
            return ParsedIntent(RinAction.ControlFlashlight("off"), "Flashlight turned off.")
        }

        // Volume
        if (text.contains("volume barhao") || text.contains("volume up") || text.contains("awaaz barhao") || text.contains("awaaz tez") || text.contains("increase volume")) {
            return ParsedIntent(RinAction.ControlVolume("up"), "Volume increased.")
        }
        if (text.contains("volume kam") || text.contains("volume down") || text.contains("awaaz dheemi") || text.contains("decrease volume")) {
            return ParsedIntent(RinAction.ControlVolume("down"), "Volume decreased.")
        }

        // Media
        if (text.contains("music pause") || text.contains("gana roko") || text.contains("pause music") || text.contains("stop music")) {
            return ParsedIntent(RinAction.ControlMedia("pause"), "Media paused.")
        }
        if (text.contains("music play") || text.contains("gana bajao") || text.contains("play music") || text.contains("chalao")) {
            return ParsedIntent(RinAction.ControlMedia("play"), "Playing media.")
        }

        // Navigation
        if (text.contains("home screen") || text.contains("home par jao") || text.contains("go home")) {
            return ParsedIntent(RinAction.NavigateHome, "Navigating to home screen.")
        }
        if (text.contains("back jao") || text.contains("go back") || text == "back") {
            return ParsedIntent(RinAction.NavigateBack, "Navigating back.")
        }

        // App Launch: "kholo" or "open"
        val openAppMatch = Regex("(?:kholo|open|launch)\\s+(.+)").find(text)
            ?: Regex("(.+)\\s+(?:kholo|open karo)").find(text)

        if (openAppMatch != null) {
            val app = openAppMatch.groupValues[1].trim()
                .removePrefix("hello mimi ")
                .removePrefix("hey mimi ")
                .removePrefix("mimi ")
                .removePrefix("hey rin ")
                .removeSuffix("app")
                .trim()
            if (app.isNotEmpty() && app != "notifications" && app != "messages" && app != "settings") {
                return ParsedIntent(RinAction.OpenApp(app), "Opening $app.")
            }
        }

        // Settings
        if (text.contains("wifi settings") || text.contains("wi-fi settings")) {
            return ParsedIntent(RinAction.OpenSettings("wifi"), "Opening Wi-Fi settings.")
        }
        if (text.contains("bluetooth settings")) {
            return ParsedIntent(RinAction.OpenSettings("bluetooth"), "Opening Bluetooth settings.")
        }
        if (text.contains("phone settings") || text.contains("settings kholo")) {
            return ParsedIntent(RinAction.OpenSettings("main"), "Opening settings.")
        }

        // Notifications
        if (text.contains("notification parho") || text.contains("notifications parho") || text.contains("read notifications") || text.contains("mere messages parho")) {
            return ParsedIntent(RinAction.ReadNotifications, "Reading your latest notifications.")
        }

        // Call
        val callMatch = Regex("(?:call|phone)\\s+(?:karo|lagao)\\s+(.+)").find(text)
            ?: Regex("(.+)\\s+(?:ko call karo|ko phone lagao|ko call lagao)").find(text)

        if (callMatch != null) {
            val contact = callMatch.groupValues[1].trim()
                .removePrefix("hello mimi ")
                .removePrefix("hey mimi ")
                .removePrefix("mimi ")
                .trim()
            if (contact.isNotEmpty()) {
                return ParsedIntent(RinAction.MakeCall(contact), "Calling $contact.")
            }
        }

        // Web Search
        if (text.contains("search karo") || text.contains("google par search") || text.contains("search on google") || text.startsWith("search for ")) {
            val query = text.replace("search karo", "").replace("google par search", "").replace("search on google", "").replace("search for ", "").trim()
            return ParsedIntent(RinAction.SearchWeb(query.ifBlank { "Latest News" }), "Searching the web.")
        }

        return null
    }
}
