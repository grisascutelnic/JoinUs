package com.scutelnic.joinus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PollinationsImageService {

    private static final Logger log = LoggerFactory.getLogger(PollinationsImageService.class);

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
        int seed = buildStableSeed(title, description);

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

        TranslationResult translated = translateToEnglish(safeTitle, safeDescription);
        String titleEn = translated == null ? safeTitle : clean(translated.titleEn, safeTitle);
        String descriptionEn = translated == null ? safeDescription : clean(translated.descriptionEn, safeDescription);

        PromptPlan plan = composeImagePrompt(titleEn, descriptionEn);
        if (plan != null && plan.prompt != null && !plan.prompt.isBlank()) {
            String finalPrompt = plan.prompt;
            if (!plan.keyObjects.isEmpty()) {
                finalPrompt = finalPrompt + " Mandatory visible elements: " + String.join(", ", plan.keyObjects) + ".";
            }
            return finalPrompt;
        }

        return "Photorealistic event photo, documentary style. "
                + "Main activity: " + titleEn + ". "
                + "Scene details: " + descriptionEn + ". "
                + "Realistic people, natural lighting, sharp focus, high detail, cinematic composition, 16:9. "
                + "No text, no logo, no watermark.";
    }

    private PromptPlan composeImagePrompt(String titleEn, String descriptionEn) {
        if (apiKey.isBlank()) {
            return null;
        }

        String systemMessage = "You are an expert prompt writer for photorealistic image generation. "
                + "Create one concise English prompt that is faithful to the user activity. "
                + "Do not change the activity type, subject, or key objects. "
                + "Do not invent unrelated events. "
                + "Extract key visual objects explicitly mentioned by user and include them in key_objects. "
                + "Output STRICT JSON only: {\"prompt\":string,\"key_objects\":string[]}.";
        String userMessage = "title_en: " + titleEn + "\n"
                + "description_en: " + descriptionEn + "\n"
                + "requirements: photorealistic, natural lighting, realistic people, no text, no watermark, 16:9.";

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

            JsonNode json = parseLooseJsonObject(content);
            if (json == null || !json.isObject()) {
                return null;
            }

            String prompt = textOrBlank(json, "prompt");
            if (prompt.isBlank()) {
                return null;
            }

            PromptPlan plan = new PromptPlan();
            plan.prompt = prompt.length() > 500 ? prompt.substring(0, 500).trim() : prompt;
            plan.keyObjects = parseStringArray(json.path("key_objects"));

            if (plan.keyObjects.isEmpty()) {
                plan.keyObjects = parseFallbackKeyObjects(titleEn, descriptionEn);
            }

            return plan;
        } catch (Exception ex) {
            return null;
        }
    }

    private int buildStableSeed(String title, String description) {
        String base = clean(title, "") + "|" + clean(description, "");
        int hash = Math.abs(base.toLowerCase(Locale.ROOT).hashCode());
        int seed = (hash % 999000) + 1000;
        if (seed <= 0) {
            return ThreadLocalRandom.current().nextInt(1000, 999999);
        }
        return seed;
    }

    private TranslationResult translateToEnglish(String title, String description) {
        if (apiKey.isBlank()) {
            return null;
        }

        String systemMessage = "You are a precise Romanian-to-English translator for activity content. "
            + "Translate faithfully and naturally, preserving original meaning and context. "
            + "Output STRICT JSON only: {\"title_en\":string,\"description_en\":string}.";
        String userMessage = "title_ro: " + title + "\n"
                + "description_ro: " + description;

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
            requestNode.put("max_tokens", 180);

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
            log.debug("AI image translation: roTitle='{}' -> enTitle='{}'", title, result.titleEn);
            return result;
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildNegativePrompt(String title, String description) {
        String normalized = (clean(title, "") + " " + clean(description, "")).toLowerCase(Locale.ROOT);
        String base = "text, watermark, logo, signature, banner, blurry, low quality, distorted anatomy, deformed face, extra limbs";
        if (!containsAny(normalized, "beer", "alcohol", "drink", "party", "cocktail", "bottle", "pub", "bar")) {
            return base + ", beer, alcohol bottles, party table";
        }
        return base;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private List<String> parseStringArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return values;
        }
        for (JsonNode item : node) {
            String value = item.asText("").trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private List<String> parseFallbackKeyObjects(String titleEn, String descriptionEn) {
        List<String> fallback = new ArrayList<>();
        String main = clean(titleEn, "");
        String context = clean(descriptionEn, "");
        if (!main.isBlank()) {
            fallback.add(main);
        }
        if (!context.isBlank()) {
            String firstClause = context.split("[\\.;,]")[0].trim();
            if (!firstClause.isBlank() && !firstClause.equalsIgnoreCase(main)) {
                fallback.add(firstClause);
            }
        }
        return fallback;
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

    private String textOrBlank(JsonNode node, String field) {
        if (node == null || field == null) {
            return "";
        }
        return node.path(field).asText("").trim();
    }

    private static class TranslationResult {
        private String titleEn = "";
        private String descriptionEn = "";
    }

    private static class PromptPlan {
        private String prompt = "";
        private List<String> keyObjects = new ArrayList<>();
    }


    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
