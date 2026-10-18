package com.civicconnect.backend.it;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Api {

    record Response(int status, String body, java.net.http.HttpHeaders headers) {

        String text(String key) {
            return Api.text(body, key);
        }

        long number(String key) {
            return Api.number(body, key);
        }

        boolean has(String fragment) {
            return body.contains(fragment);
        }
    }

    static final String PASSWORD = "Test@1234";

    private final HttpClient http = HttpClient.newHttpClient();
    private final String base;

    Api(int port) {
        this.base = "http://localhost:" + port;
    }

    Response get(String path, String token) {
        return send(builder(path, token).GET().build());
    }

    Response post(String path, String token, String json) {
        return send(builder(path, token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build());
    }

    String login(String email) {
        Response r = post("/api/auth/login", null, "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        if (r.status() != 200) {
            throw new AssertionError("Login failed for " + email + ": " + r.status() + " " + r.body());
        }
        return r.text("token");
    }

    Response createRequest(String token, String categoryId, String description) {
        return post("/api/requests", token, "{\"categoryId\":\"" + categoryId + "\",\"description\":\""
                + description + "\",\"priority\":\"MEDIUM\"}");
    }

    Response assign(String token, String id, long version) {
        return post("/api/requests/" + id + "/assign", token, "{\"version\":" + version + "}");
    }

    Response status(String token, String id, String target, String comment, long version) {
        String note = comment == null ? "null" : "\"" + comment + "\"";
        return post("/api/requests/" + id + "/status", token,
                "{\"targetStatus\":\"" + target + "\",\"comment\":" + note + ",\"version\":" + version + "}");
    }

    Response comment(String token, String id, String text, long version) {
        return post("/api/requests/" + id + "/comments", token,
                "{\"comment\":\"" + text + "\",\"version\":" + version + "}");
    }

    static String text(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        if (!m.find()) {
            throw new AssertionError("No string \"" + key + "\" in " + json);
        }
        return m.group(1);
    }

    static long number(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!m.find()) {
            throw new AssertionError("No number \"" + key + "\" in " + json);
        }
        return Long.parseLong(m.group(1));
    }

    private HttpRequest.Builder builder(String path, String token) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return b;
    }

    private Response send(HttpRequest request) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body(), response.headers());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
