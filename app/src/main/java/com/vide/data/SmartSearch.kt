package com.vide.data

import com.vide.model.AppInfo
import java.text.Normalizer

object SmartSearch {

    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    fun search(query: String, apps: List<AppInfo>): List<AppInfo> {
        if (query.isBlank()) return apps

        val q = normalize(query.trim())
        val expandedTerms = expandKeywords(q)

        val scored = apps.map { app ->
            val label = normalize(app.label)
            val pkg = app.packageName.lowercase()

            var score = 0

            // Direct label match
            if (label.contains(q)) score += 100

            // Label starts with query
            if (label.startsWith(q)) score += 50

            // Package name match
            if (pkg.contains(q)) score += 40

            // Keyword expansion matches
            for (term in expandedTerms) {
                if (label.contains(term)) score += 30
                if (pkg.contains(term)) score += 20
            }

            // Fuzzy: query chars appear in order in label
            if (score == 0 && isSubsequence(q, label)) score += 10

            app to score
        }

        return scored
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun expandKeywords(query: String): Set<String> {
        val terms = mutableSetOf<String>()
        for ((keyword, expansions) in keywords) {
            if (keyword.contains(query) || query.contains(keyword)) {
                terms.addAll(expansions)
            }
        }
        return terms
    }

    private fun isSubsequence(query: String, target: String): Boolean {
        var qi = 0
        for (c in target) {
            if (qi < query.length && c == query[qi]) qi++
        }
        return qi == query.length
    }

    private val keywords = mapOf(
        // Authentication / Security
        "otp" to setOf("authenticator", "2fa", "otp", "authy", "aegis", "totp", "duo"),
        "2fa" to setOf("authenticator", "2fa", "otp", "authy", "aegis"),
        "mdp" to setOf("password", "bitwarden", "1password", "dashlane", "keepass", "lastpass"),
        "password" to setOf("password", "bitwarden", "1password", "dashlane", "keepass", "lastpass"),

        // Documents
        "pdf" to setOf("pdf", "reader", "adobe", "acrobat", "document", "viewer", "xodo"),
        "doc" to setOf("document", "word", "docs", "office", "writer", "google"),
        "scan" to setOf("scanner", "scan", "camscanner", "adobe", "genius"),

        // Media
        "musique" to setOf("spotify", "deezer", "music", "soundcloud", "tidal", "apple", "youtube"),
        "music" to setOf("spotify", "deezer", "music", "soundcloud", "tidal", "apple"),
        "podcast" to setOf("podcast", "spotify", "castbox", "pocket casts", "overcast"),
        "video" to setOf("youtube", "vlc", "video", "netflix", "twitch", "prime"),
        "film" to setOf("netflix", "prime", "disney", "canal", "video", "cinema", "ocs"),
        "serie" to setOf("netflix", "prime", "disney", "canal", "ocs", "hbo"),
        "stream" to setOf("twitch", "youtube", "netflix", "disney", "spotify"),

        // Photos
        "photo" to setOf("camera", "gallery", "photos", "instagram", "snapseed", "lightroom", "appareil"),
        "camera" to setOf("camera", "appareil", "photo"),
        "image" to setOf("gallery", "photos", "image", "galerie"),

        // Communication
        "mail" to setOf("gmail", "mail", "outlook", "email", "courrier", "proton"),
        "email" to setOf("gmail", "mail", "outlook", "email", "proton"),
        "message" to setOf("messages", "whatsapp", "telegram", "signal", "messenger", "sms"),
        "chat" to setOf("whatsapp", "telegram", "signal", "messenger", "discord"),
        "appel" to setOf("phone", "téléphone", "dialer", "appel"),
        "tel" to setOf("phone", "téléphone", "dialer", "appel"),

        // Navigation
        "carte" to setOf("maps", "waze", "navigation", "map", "plan"),
        "map" to setOf("maps", "waze", "navigation", "carte"),
        "gps" to setOf("maps", "waze", "navigation", "gps"),
        "itineraire" to setOf("maps", "waze", "citymapper", "navigation"),

        // Finance
        "banque" to setOf("bank", "banque", "boursorama", "revolut", "n26", "crédit", "bnp", "société", "caisse"),
        "bank" to setOf("bank", "banque", "boursorama", "revolut", "n26"),
        "paiement" to setOf("pay", "paypal", "lydia", "paiement", "wallet", "sumup"),
        "argent" to setOf("bank", "banque", "pay", "revolut", "lydia", "budget"),
        "crypto" to setOf("binance", "coinbase", "crypto", "bitcoin", "ledger"),

        // Social
        "social" to setOf("instagram", "facebook", "twitter", "tiktok", "snapchat", "linkedin", "threads"),
        "reseau" to setOf("instagram", "facebook", "twitter", "tiktok", "snapchat", "linkedin"),

        // Productivity
        "note" to setOf("notes", "keep", "notion", "evernote", "memo", "obsidian"),
        "todo" to setOf("todoist", "ticktick", "tasks", "rappel", "reminder"),
        "tache" to setOf("todoist", "ticktick", "tasks", "rappel"),
        "agenda" to setOf("calendar", "calendrier", "agenda", "google"),
        "calendrier" to setOf("calendar", "calendrier", "agenda"),

        // Utilities
        "calcul" to setOf("calculatrice", "calculator", "calc"),
        "meteo" to setOf("météo", "weather", "meteo", "climat"),
        "weather" to setOf("météo", "weather", "meteo"),
        "horloge" to setOf("clock", "horloge", "alarme", "timer", "chrono"),
        "alarme" to setOf("clock", "horloge", "alarme", "alarm", "réveil"),
        "reveil" to setOf("clock", "horloge", "alarme", "alarm", "réveil"),
        "fichier" to setOf("files", "fichier", "file", "manager", "gestionnaire"),
        "file" to setOf("files", "fichier", "file", "manager"),
        "param" to setOf("paramètres", "settings", "réglages"),
        "setting" to setOf("paramètres", "settings", "réglages"),
        "vpn" to setOf("vpn", "nordvpn", "expressvpn", "proton", "mullvad"),
        "qr" to setOf("qr", "scanner", "barcode", "code"),
        "lampe" to setOf("flashlight", "lampe", "torche"),
        "torch" to setOf("flashlight", "lampe", "torche"),

        // Shopping
        "shop" to setOf("amazon", "ebay", "leboncoin", "vinted", "aliexpress", "cdiscount"),
        "achat" to setOf("amazon", "ebay", "leboncoin", "vinted", "aliexpress"),
        "occasion" to setOf("leboncoin", "vinted", "ebay", "occasion"),

        // Transport
        "transport" to setOf("uber", "bolt", "blablacar", "sncf", "ratp", "citymapper", "metro"),
        "taxi" to setOf("uber", "bolt", "taxi", "freenow"),
        "train" to setOf("sncf", "trainline", "train", "oui"),
        "metro" to setOf("ratp", "metro", "citymapper", "transport"),
        "avion" to setOf("flight", "ryanair", "easyjet", "airfrance", "skyscanner", "booking"),
        "vol" to setOf("flight", "ryanair", "easyjet", "airfrance", "skyscanner"),
        "voyage" to setOf("booking", "airbnb", "tripadvisor", "skyscanner", "maps"),

        // Food
        "food" to setOf("ubereats", "deliveroo", "justeat", "uber eats"),
        "livraison" to setOf("ubereats", "deliveroo", "justeat", "uber eats", "stuart"),
        "resto" to setOf("ubereats", "deliveroo", "thefork", "restaurant", "tripadvisor"),
        "recette" to setOf("marmiton", "cuisine", "recipe", "recette"),

        // Health
        "sante" to setOf("santé", "doctolib", "ameli", "health", "médecin"),
        "docteur" to setOf("doctolib", "doctor", "médecin", "santé"),
        "sport" to setOf("strava", "nike", "adidas", "fitness", "running", "sport"),
        "fitness" to setOf("strava", "nike", "fitness", "gym", "musculation"),

        // Games
        "jeu" to setOf("game", "play", "jeu", "gaming"),
        "game" to setOf("game", "play", "jeu", "gaming"),
    )
}
