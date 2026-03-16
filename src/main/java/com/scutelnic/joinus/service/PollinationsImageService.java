package com.scutelnic.joinus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PollinationsImageService {

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PollinationsImageService(@Value("${pollinations.api-key:}") String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String buildImageUrl(String title, String description) {
        if (apiKey.isBlank()) {
            return buildLocalSvgDataUrl(title, description);
        }

        String prompt = buildPrompt(title, description);
        String negativePrompt = buildNegativePrompt(title, description);
        String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8).replace("+", "%20");
        String encodedNegative = URLEncoder.encode(negativePrompt, StandardCharsets.UTF_8).replace("+", "%20");
        int seed = ThreadLocalRandom.current().nextInt(1000, 999999);

        String base = "https://gen.pollinations.ai/image/"
                + encodedPrompt
                + "?width=1280&height=720&model=flux&seed="
                + seed
                + "&enhance=true&negative_prompt="
                + encodedNegative;
        return withApiKey(base);
    }

    public String getClientApiKey() {
        // Expose only publishable keys to client-side JavaScript.
        if (apiKey.startsWith("pk_")) {
            return apiKey;
        }
        return "";
    }

    public String normalizeLegacyUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return imageUrl;
        }

        try {
            URI uri = URI.create(imageUrl.trim());
            String host = uri.getHost();
            if (host == null) {
                return imageUrl;
            }
            if (!"image.pollinations.ai".equalsIgnoreCase(host)) {
                return imageUrl;
            }

            String path = uri.getPath();
            if (path == null || !path.startsWith("/prompt/")) {
                return imageUrl;
            }

            String promptPart = path.substring("/prompt/".length());
            String query = uri.getRawQuery();
            return "https://gen.pollinations.ai/image/" + promptPart + (query == null || query.isBlank() ? "" : "?" + query);
        } catch (IllegalArgumentException ex) {
            return imageUrl;
        }
    }

    public boolean isPollinationsUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(imageUrl.trim());
            String host = uri.getHost();
            return host != null && host.toLowerCase().endsWith("pollinations.ai");
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String withApiKey(String url) {
        if (apiKey.isBlank()) {
            return url;
        }
        String encoded = URLEncoder.encode(apiKey, StandardCharsets.UTF_8).replace("+", "%20");
        if (url.contains("?")) {
            return url + "&key=" + encoded;
        }
        return url + "?key=" + encoded;
    }

    private String buildLocalSvgDataUrl(String title, String description) {
        String safeTitle = clean(title, "Activitate JoinUs");
        String safeDescription = clean(description, "Comunitate si socializare");

        int hash = Math.abs((safeTitle + "|" + safeDescription).hashCode());
        String colorA = colorFromHash(hash);
        String colorB = colorFromHash(hash / 7 + 12345);
        String colorC = colorFromHash(hash / 13 + 241);
        String colorD = colorFromHash(hash / 17 + 991);

        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='1280' height='720' viewBox='0 0 1280 720'>"
                + "<defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>"
                + "<stop offset='0%' stop-color='" + colorA + "'/>"
                + "<stop offset='100%' stop-color='" + colorB + "'/>"
            + "</linearGradient><radialGradient id='r' cx='0.8' cy='0.2' r='0.9'>"
            + "<stop offset='0%' stop-color='" + colorC + "' stop-opacity='0.85'/>"
            + "<stop offset='100%' stop-color='" + colorD + "' stop-opacity='0.25'/></radialGradient></defs>"
                + "<rect width='1280' height='720' fill='url(#g)'/>"
            + "<rect width='1280' height='720' fill='url(#r)'/>"
            + "<g opacity='0.65'><circle cx='220' cy='150' r='180' fill='rgba(255,255,255,0.12)'/>"
            + "<circle cx='1080' cy='130' r='120' fill='rgba(255,255,255,0.14)'/>"
            + "<circle cx='980' cy='560' r='220' fill='rgba(255,255,255,0.08)'/></g>"
            + "<g opacity='0.22'>"
            + "<path d='M120 520 Q360 360 620 510 T1160 470 L1160 720 L120 720 Z' fill='rgba(255,255,255,0.55)'/>"
            + "<path d='M0 620 Q280 500 560 620 T1280 580 L1280 720 L0 720 Z' fill='rgba(255,255,255,0.42)'/>"
            + "</g>"
                + "</svg>";

        return "data:image/svg+xml;charset=UTF-8," + URLEncoder.encode(svg, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String colorFromHash(int hash) {
        int r = 60 + (hash % 140);
        int g = 60 + ((hash / 11) % 140);
        int b = 60 + ((hash / 23) % 140);
        return String.format(Locale.ROOT, "#%02x%02x%02x", r, g, b);
    }

    private String buildPrompt(String title, String description) {
        String safeTitle = clean(title, "community activity");
        String safeDescription = clean(description, "people gathering together");
        String criticalAnchors = buildCriticalAnchors(safeTitle + " " + safeDescription);
        String antiDrift = buildGlobalAntiDriftConstraints(safeTitle + " " + safeDescription);

        TranslationResult translated = translateToEnglish(safeTitle, safeDescription);
        if (translated != null) {
            String translatedPrompt = buildPromptFromTranslation(translated, criticalAnchors, antiDrift);
            return enforceSemanticAlignment(safeTitle, safeDescription, translatedPrompt);
        }

        PromptSpec promptSpec = buildPromptSpecViaTextModel(safeTitle, safeDescription);
        if (promptSpec != null) {
            String specPrompt = buildPromptFromSpec(promptSpec, criticalAnchors, antiDrift, safeTitle, safeDescription);
            return enforceSemanticAlignment(safeTitle, safeDescription, specPrompt);
        }

        String autoPrompt = buildPromptViaTextModel(safeTitle, safeDescription);
        if (autoPrompt != null && !autoPrompt.isBlank()) {
            String adjusted = autoPrompt;
            if (!criticalAnchors.isBlank()) {
                adjusted = adjusted + ". Critical constraints: " + criticalAnchors + ".";
            }
            if (!antiDrift.isBlank()) {
                adjusted = adjusted + " Anti-drift constraints: " + antiDrift + ".";
            }
            return enforceSemanticAlignment(safeTitle, safeDescription, adjusted);
        }

        String hints = buildSemanticHints(safeTitle + " " + safeDescription);

        String fallbackPrompt = "Photorealistic event photo, no text, no watermark. "
                + "Understand Romanian input exactly and keep the core activity. "
                + "Activity title: " + safeTitle + ". "
                + "Activity details: " + safeDescription + ". "
                + "Important visual elements: " + hints + ". "
                + (criticalAnchors.isBlank() ? "" : "Critical constraints: " + criticalAnchors + ". ")
                + (antiDrift.isBlank() ? "" : "Anti-drift constraints: " + antiDrift + ". ")
                + "Do not generate generic random outdoor crowd scene. "
                + "Natural light, detailed, vibrant, 16:9.";
        return enforceSemanticAlignment(safeTitle, safeDescription, fallbackPrompt);
    }

    private TranslationResult translateToEnglish(String title, String description) {
        if (apiKey.isBlank()) {
            return null;
        }

        String systemMessage = "You are a precise Romanian-to-English translator for activity descriptions. "
                + "Translate meaning faithfully, preserve concrete objects/actions, and avoid paraphrasing away key nouns. "
                + "Output STRICT JSON only: {\"title_en\":string,\"description_en\":string}.";
        String userMessage = "title_ro: " + title + "\n"
                + "description_ro: " + description;

        try {
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("model", "openai-large");

            ArrayNode messagesNode = objectMapper.createArrayNode();
            messagesNode.add(objectMapper.createObjectNode()
                    .put("role", "system")
                    .put("content", systemMessage));
            messagesNode.add(objectMapper.createObjectNode()
                    .put("role", "user")
                    .put("content", userMessage));

            requestNode.set("messages", messagesNode);
            requestNode.put("temperature", 0.0);
            requestNode.put("max_tokens", 220);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://gen.pollinations.ai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestNode.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                return null;
            }

            JsonNode json = parseLooseJsonObject(content);
            if (json == null || !json.isObject()) {
                return null;
            }

            String titleEn = textOrBlank(json, "title_en");
            String descriptionEn = textOrBlank(json, "description_en");
            if (titleEn.isBlank() && descriptionEn.isBlank()) {
                return null;
            }

            TranslationResult result = new TranslationResult();
            result.titleEn = titleEn.isBlank() ? title : titleEn;
            result.descriptionEn = descriptionEn.isBlank() ? description : descriptionEn;
            return result;
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildPromptFromTranslation(TranslationResult translated,
                                              String criticalAnchors,
                                              String antiDrift) {
        String titleEn = clean(translated.titleEn, "community activity");
        String descriptionEn = clean(translated.descriptionEn, "people gathering together");

        return "Photorealistic image. "
            + "Main activity: " + titleEn + ". "
            + "Context: " + descriptionEn + ". "
            + "Show the core action and objects clearly in foreground. "
            + "No text, no watermark, realistic people, natural lighting, 16:9.";
    }

    private String enforceSemanticAlignment(String titleRo, String descriptionRo, String promptEn) {
        if (apiKey.isBlank() || promptEn == null || promptEn.isBlank()) {
            return promptEn;
        }

        String systemMessage = "You are a strict semantic alignment checker. "
                + "Compare Romanian source text with an English image prompt. "
                + "If prompt changes key entities/actions, rewrite it to preserve exact activity intent. "
                + "Return STRICT JSON only: {\"aligned\":boolean,\"corrected_prompt\":string}. "
                + "Do not add markdown.";
        String userMessage = "source_title_ro: " + titleRo + "\n"
                + "source_description_ro: " + descriptionRo + "\n"
                + "candidate_prompt_en: " + promptEn;

        try {
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("model", "openai-fast");

            ArrayNode messagesNode = objectMapper.createArrayNode();
            messagesNode.add(objectMapper.createObjectNode()
                    .put("role", "system")
                    .put("content", systemMessage));
            messagesNode.add(objectMapper.createObjectNode()
                    .put("role", "user")
                    .put("content", userMessage));

            requestNode.set("messages", messagesNode);
            requestNode.put("temperature", 0.0);
            requestNode.put("max_tokens", 260);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://gen.pollinations.ai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestNode.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return promptEn;
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            JsonNode json = parseLooseJsonObject(content);
            if (json == null || !json.isObject()) {
                return promptEn;
            }

            boolean aligned = json.path("aligned").asBoolean(true);
            String corrected = textOrBlank(json, "corrected_prompt");
            if (aligned || corrected.isBlank()) {
                return promptEn;
            }
            return corrected;
        } catch (Exception ex) {
            return promptEn;
        }
    }

    private String normalizeRomanian(String text) {
        return (text == null ? "" : text)
                .toLowerCase(Locale.ROOT)
                .replace("ă", "a")
                .replace("â", "a")
                .replace("î", "i")
                .replace("ș", "s")
                .replace("ş", "s")
                .replace("ț", "t")
                .replace("ţ", "t");
    }

    private String buildNegativePrompt(String title, String description) {
        String normalized = normalizeRomanian(title + " " + description);

        List<String> negatives = new ArrayList<>();
        negatives.add("text, watermark, logo, banner, sign");
        negatives.add("blurry, low quality, distorted anatomy");

        boolean allowsMarketScene = containsAny(normalized,
                "targ", "piata", "market", "fair", "festival", "expoz", "expo", "workshop", "atelier", "vanz", "cumpar");
        if (!allowsMarketScene) {
            negatives.add("market stalls, fair booth, craft table, city square crowd, random shopping crowd");
            negatives.add("umbrellas in crowd, street bazaar");
        }

        return String.join(", ", negatives);
    }

    private String buildPromptFromSpec(PromptSpec spec,
                                       String criticalAnchors,
                                       String antiDrift,
                                       String safeTitle,
                                       String safeDescription) {
        String activity = nonBlankOrDefault(spec.activity, safeTitle);
        String mainSubject = nonBlankOrDefault(spec.mainSubject, "people doing the exact activity");
        String locationType = nonBlankOrDefault(spec.locationType, "outdoor community location");
        String environment = nonBlankOrDefault(spec.environment, "natural light");
        String style = nonBlankOrDefault(spec.style, "photorealistic documentary photo");

        String mustHave = spec.mustHaveObjects.isEmpty()
                ? mainSubject
                : String.join(", ", spec.mustHaveObjects);

        String mustAvoid = spec.mustAvoidObjects.isEmpty()
                ? "unrelated crowd scene, unrelated objects"
                : String.join(", ", spec.mustAvoidObjects);

        return "Photorealistic event image, no text, no watermark. "
                + "Activity: " + activity + ". "
                + "Main subject MUST be clearly visible: " + mainSubject + ". "
                + "Location type: " + locationType + ". "
                + "Environment: " + environment + ". "
                + "Must-have objects: " + mustHave + ". "
                + "Must-avoid objects: " + mustAvoid + ". "
                + (criticalAnchors.isBlank() ? "" : "Critical constraints: " + criticalAnchors + ". ")
                + (antiDrift.isBlank() ? "" : "Anti-drift constraints: " + antiDrift + ". ")
                + "Style: " + style + ". "
                + "User context: title=" + safeTitle + "; details=" + safeDescription + ". "
                + "Natural light, high detail, cinematic composition, 16:9.";
    }

    private PromptSpec buildPromptSpecViaTextModel(String title, String description) {
        if (apiKey.isBlank()) {
            return null;
        }

        String systemMessage = "You are an image prompt planner. "
                + "Read Romanian or English event text and output STRICT JSON only (no markdown). "
                + "Preserve exact intent and disambiguate nouns correctly. "
                + "For Romanian 'masina', use passenger car/automobile unless explicitly tractor/truck/bus/farm machine. "
            + "Do not convert hobbies into market/fair/workshop scenes unless user explicitly asks for market/fair/exhibition/workshop. "
                + "JSON schema: {"
                + "\"activity\":string,"
                + "\"main_subject\":string,"
                + "\"location_type\":string,"
                + "\"environment\":string,"
                + "\"must_have_objects\":string[],"
                + "\"must_avoid_objects\":string[],"
                + "\"style\":string"
                + "}.";

        String userMessage = "title: " + title + "\n"
                + "description: " + description;

        try {
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("model", "openai-fast");

            ArrayNode messagesNode = objectMapper.createArrayNode();
            messagesNode.add(objectMapper.createObjectNode()
                    .put("role", "system")
                    .put("content", systemMessage));
            messagesNode.add(objectMapper.createObjectNode()
                    .put("role", "user")
                    .put("content", userMessage));

            requestNode.set("messages", messagesNode);
            requestNode.put("temperature", 0.1);
            requestNode.put("max_tokens", 220);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://gen.pollinations.ai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestNode.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                return null;
            }

            JsonNode specNode = parseLooseJsonObject(content);
            if (specNode == null || !specNode.isObject()) {
                return null;
            }

            PromptSpec spec = new PromptSpec();
            spec.activity = textOrBlank(specNode, "activity");
            spec.mainSubject = textOrBlank(specNode, "main_subject");
            spec.locationType = textOrBlank(specNode, "location_type");
            spec.environment = textOrBlank(specNode, "environment");
            spec.style = textOrBlank(specNode, "style");
            spec.mustHaveObjects = stringList(specNode.get("must_have_objects"));
            spec.mustAvoidObjects = stringList(specNode.get("must_avoid_objects"));

            if (spec.activity.isBlank() && spec.mainSubject.isBlank()) {
                return null;
            }
            return spec;
        } catch (Exception ex) {
            return null;
        }
    }

    private JsonNode parseLooseJsonObject(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(text);
        } catch (Exception ignored) {
            // Try extracting first JSON object from wrapped content.
        }

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }

        String candidate = text.substring(start, end + 1);
        try {
            return objectMapper.readTree(candidate);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> stringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText("").trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private String textOrBlank(JsonNode node, String field) {
        if (node == null || field == null) {
            return "";
        }
        return node.path(field).asText("").trim();
    }

    private String nonBlankOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static class PromptSpec {
        private String activity = "";
        private String mainSubject = "";
        private String locationType = "";
        private String environment = "";
        private List<String> mustHaveObjects = Collections.emptyList();
        private List<String> mustAvoidObjects = Collections.emptyList();
        private String style = "";
    }

    private static class TranslationResult {
        private String titleEn = "";
        private String descriptionEn = "";
    }

    private String buildPromptViaTextModel(String title, String description) {
        if (apiKey.isBlank()) {
            return null;
        }

        String userMessage = "Title: " + title + "\nDescription: " + description;
        String systemMessage = "You are an expert image prompt engineer. "
                + "Convert the user's activity title and description into one short, precise, photorealistic image prompt in English. "
                + "Preserve exact activity intent and context (Romanian inputs included). "
            + "Object fidelity is mandatory: the main object/activity from input must be clearly visible in the scene. "
            + "Disambiguate Romanian terms correctly: 'masina' means passenger car/automobile unless user explicitly asks tractor, truck, bus, or farm machinery. "
            + "If user says car drive/walk with car/road trip, prefer road, forest road, mountain road with a normal car. "
                + "Avoid generic crowds if not requested. "
                + "Output ONLY the final prompt text, no markdown, no quotes.";

        try {
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("model", "openai-fast");

            ArrayNode messagesNode = objectMapper.createArrayNode();
            messagesNode.add(objectMapper.createObjectNode()
                .put("role", "system")
                .put("content", systemMessage));
            messagesNode.add(objectMapper.createObjectNode()
                .put("role", "user")
                .put("content", userMessage));

            requestNode.set("messages", messagesNode);
            requestNode.put("temperature", 0.2);
            requestNode.put("max_tokens", 180);

            String requestBody = requestNode.toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://gen.pollinations.ai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                return null;
            }

            String generated = content.asText().trim();
            if (generated.length() > 500) {
                return generated.substring(0, 500).trim();
            }
            return generated;
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildSemanticHints(String text) {
        String normalized = text.toLowerCase(Locale.ROOT)
                .replace("ă", "a")
                .replace("â", "a")
                .replace("î", "i")
                .replace("ș", "s")
                .replace("ş", "s")
                .replace("ț", "t")
                .replace("ţ", "t");

        List<String> hints = new ArrayList<>();

        if (containsAny(normalized, "sah", "chess")) {
            hints.add("chessboard, chess pieces, people playing chess outdoors");
            hints.add("focus on the chess game");
        }

        boolean hasCooking = containsAny(normalized,
                "gatit", "gati", "frigarui", "frigaru", "gratar", "grill", "bbq", "barbecue", "cooking");
        boolean hasOutdoor = containsAny(normalized,
                "natura", "afara", "parc", "padure", "forest", "outdoor", "camp", "campfire", "picnic");

        if (hasCooking && hasOutdoor) {
            hints.add("outdoor barbecue scene, skewers on grill, food cooking over fire");
            hints.add("people grilling in nature, picnic table, smoke from grill");
            hints.add("focus on cooking process and food, not random crowd");
            hints.add("no market fair, no street protest, no festival crowd");
        } else if (hasCooking) {
            hints.add("cooking activity, grilled food, barbecue tools, close-up on food and grill");
            hints.add("no unrelated crowd scene");
        }

        boolean hasCycling = containsAny(normalized, "cicl", "biciclet", "cycling", "bike", "mtb");
        boolean hasMountain = containsAny(normalized, "munte", "munti", "montan", "mountain", "trail", "traseu");

        if (hasCycling && hasMountain) {
            hints.add("mountain biking on alpine trail, forest mountain path, elevation scenery");
            hints.add("cyclists with MTB helmets on off-road route");
            hints.add("no city street race, no urban boulevard");
        } else if (hasCycling) {
            hints.add("cycling activity, bicycles in motion, outdoor ride");
        }
        if (containsAny(normalized, "fotbal", "football", "soccer")) {
            hints.add("soccer ball, football field, active play moment");
        }
        if (containsAny(normalized, "alerg", "running", "jog")) {
            hints.add("running movement, park path, athletic posture");
        }
        if (containsAny(normalized, "yoga", "medit")) {
            hints.add("yoga poses, calm posture, wellness atmosphere");
        }
        if (containsAny(normalized, "drumet", "hike", "traseu", "munte")) {
            hints.add("hiking trail, mountain path, trekking group");
        }
        if (containsAny(normalized, "dans", "dance")) {
            hints.add("dance movement, dynamic body posture");
        }
        if (containsAny(normalized, "pict", "paint", "arta", "art")) {
            hints.add("art materials, painting scene, creative workshop");
        }

        if (hints.isEmpty()) {
            return "people engaged in the exact described activity";
        }
        return String.join(", ", hints);
    }

    private String buildCriticalAnchors(String text) {
        String normalized = normalizeRomanian(text);

        List<String> anchors = new ArrayList<>();

        boolean hasCar = containsAny(normalized, "masina", "automobil", "car", "road trip", "drive");
        boolean hasFarmVehicle = containsAny(normalized, "tractor", "combine", "buldozer", "camion", "truck", "bus");
        if (hasCar && !hasFarmVehicle) {
            anchors.add("show a passenger car (automobile) as primary subject");
            anchors.add("do not show tractor, farm machinery, or construction vehicle");
        }

        if (containsAny(normalized, "padure", "forest", "natura", "outdoor") && hasCar) {
            anchors.add("place the car on a forest road or nature route");
        }

        boolean hasCycling = containsAny(normalized, "cicl", "biciclet", "cycling", "bike", "mtb");
        if (hasCycling) {
            anchors.add("show bicycles clearly as primary subject");
            anchors.add("avoid motor vehicles dominating the frame");
        }

        if (containsAny(normalized, "sah", "chess")) {
            anchors.add("chessboard and chess pieces must be visible");
        }

        boolean hasHorseRiding = containsAny(normalized, "calar", "calarit", "calarie", "cai", "cal", "horse", "horses", "riding");
        if (hasHorseRiding) {
            anchors.add("horses and riders must be the primary subject");
            anchors.add("outdoor riding scene in park or nature trail");
            anchors.add("do not show market stalls, workshop tables, or street fair");
        }

        boolean hasParachute = containsAny(normalized, "parasuta", "parasut", "parasutism", "skydiv", "jump");
        if (hasParachute) {
            anchors.add("parachute and skydiver must be clearly visible in air");
            anchors.add("no umbrellas, no rain accessories, no street crowd scene");
        }

        if (containsAny(normalized, "gatit", "frigarui", "gratar", "bbq", "barbecue")) {
            anchors.add("grill and food preparation must be visible");
            anchors.add("avoid market crowd scene unless explicitly requested");
        }

        return String.join(", ", anchors);
    }

    private String buildGlobalAntiDriftConstraints(String text) {
        String normalized = normalizeRomanian(text);

        boolean allowsMarketScene = containsAny(normalized,
                "targ", "piata", "market", "fair", "expoz", "expo", "workshop", "atelier", "vanz", "cumpar");

        if (!allowsMarketScene) {
            return "avoid market stalls, fair booths, exhibition tables, and unrelated commercial crowd scene";
        }
        return "";
    }

    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
