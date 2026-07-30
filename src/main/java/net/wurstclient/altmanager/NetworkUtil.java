package net.wurstclient.altmanager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.wurstclient.C;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/utils/client/NetworkUtil.java#L33
public class NetworkUtil {
    public static final HttpClient CLIENT = HttpClient.newHttpClient();

    // the cookie login redirects to a link with a space in it.
    public static String fixURL(String url) {
        return url.replaceAll(" ", "%20");
    }

    public static HttpResponse<String> getServerResponse(HttpRequest req) throws IOException, InterruptedException {
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public static JsonObject getJSONObject(HttpRequest req) throws IOException, InterruptedException {
        HttpResponse<String> response = getServerResponse(req);
        if (response.body().isEmpty()) return null;
        return getJSONObject(response);
    }

    public static JsonObject getJSONObject(HttpResponse<String> request) throws IOException {
        return C.gson.fromJson(request.body(), JsonElement.class).getAsJsonObject();
    }
}