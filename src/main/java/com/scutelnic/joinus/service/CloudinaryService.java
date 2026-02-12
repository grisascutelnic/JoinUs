package com.scutelnic.joinus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService {

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String uploadPreset;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CloudinaryService() {
        Map<String, String> env = loadEnv();
        this.cloudName = env.get("CLOUDINARY_CLOUD_NAME");
        this.apiKey = env.get("CLOUDINARY_API_KEY");
        this.apiSecret = env.get("CLOUDINARY_API_SECRET");
        this.uploadPreset = env.get("CLOUDINARY_UPLOAD_PRESET");
    }

    public boolean isConfigured() {
        return notBlank(cloudName) && notBlank(apiKey) && notBlank(apiSecret);
    }

    public String getCloudName() {
        return cloudName;
    }

    public String getUploadPreset() {
        return uploadPreset;
    }

    public String uploadImage(MultipartFile file) throws IOException {
        if (!isConfigured()) {
            return null;
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = "upload-" + UUID.randomUUID();
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        long timestamp = Instant.now().getEpochSecond();
        String signature = sha1("timestamp=" + timestamp + apiSecret);

        String boundary = "----JoinUsBoundary" + UUID.randomUUID();
        byte[] body = buildMultipart(boundary, file.getBytes(), filename, contentType, timestamp, signature);
        return uploadMultipart(body, boundary);
    }

    public String uploadImageFromUrl(String imageUrl) throws IOException {
        if (!isConfigured() || !notBlank(imageUrl)) {
            return null;
        }

        long timestamp = Instant.now().getEpochSecond();
        String signature = sha1("timestamp=" + timestamp + apiSecret);

        String boundary = "----JoinUsBoundary" + UUID.randomUUID();
        byte[] body = buildMultipartForRemoteUrl(boundary, imageUrl.trim(), timestamp, signature);
        return uploadMultipart(body, boundary);
    }

    private String uploadMultipart(byte[] body, String boundary) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Cloudinary upload interrupted", ex);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Cloudinary upload failed with status " + response.statusCode());
        }
        JsonNode jsonNode = objectMapper.readTree(response.body());
        JsonNode secureUrlNode = jsonNode.get("secure_url");
        if (secureUrlNode == null || secureUrlNode.asText().isBlank()) {
            throw new IOException("Cloudinary upload response missing secure_url");
        }

        return secureUrlNode.asText();
    }

    private byte[] buildMultipart(String boundary,
                                  byte[] fileBytes,
                                  String filename,
                                  String contentType,
                                  long timestamp,
                                  String signature) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String lineBreak = "\r\n";

        writePart(output, boundary, "api_key", apiKey);
        writePart(output, boundary, "timestamp", String.valueOf(timestamp));
        writePart(output, boundary, "signature", signature);

        output.write(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"" + lineBreak)
                .getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + contentType + lineBreak + lineBreak).getBytes(StandardCharsets.UTF_8));
        output.write(fileBytes);
        output.write(lineBreak.getBytes(StandardCharsets.UTF_8));

        output.write(("--" + boundary + "--" + lineBreak).getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private byte[] buildMultipartForRemoteUrl(String boundary,
                                              String imageUrl,
                                              long timestamp,
                                              String signature) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String lineBreak = "\r\n";

        writePart(output, boundary, "api_key", apiKey);
        writePart(output, boundary, "timestamp", String.valueOf(timestamp));
        writePart(output, boundary, "signature", signature);
        writePart(output, boundary, "file", imageUrl);

        output.write(("--" + boundary + "--" + lineBreak).getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private void writePart(ByteArrayOutputStream output, String boundary, String name, String value) throws IOException {
        String lineBreak = "\r\n";
        output.write(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"" + lineBreak + lineBreak)
                .getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write(lineBreak.getBytes(StandardCharsets.UTF_8));
    }

    private String sha1(String input) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IOException("Failed to compute SHA-1 signature", ex);
        }
    }

    private Map<String, String> loadEnv() {
        Map<String, String> values = new HashMap<>(System.getenv());
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return values;
        }

        try {
            for (String line : Files.readAllLines(envPath)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                String key = trimmed.substring(0, idx).trim();
                String val = trimmed.substring(idx + 1).trim();
                if (!key.isEmpty() && !val.isEmpty()) {
                    values.putIfAbsent(key, val);
                }
            }
        } catch (IOException ignored) {
            return values;
        }
        return values;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
