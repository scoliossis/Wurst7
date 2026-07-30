package net.wurstclient.altmanager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.User;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

// https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/utils/alts/microsoft/Cookies.java
// stole urls from novoline
public class CookieAltUtil {
    public static boolean authenticate(String cookie) {
        if (cookie.isEmpty()) return false;
        AltManagerUtil.addProgressReport("Logging in with cookies");

        AltManagerUtil.addProgressReport("Getting xbox redirect.");
        String redirectLocation = getRedirectLocation(cookie);
        if (redirectLocation == null) return false;

        String xboxUrl = getXboxUrl(redirectLocation, cookie);
        if (xboxUrl == null) return false;

        AltManagerUtil.addProgressReport("Getting XSTS token and user hashcode from xbox redirect");
        String XSTSTokenAndHashcode = getXSTSTokenAndHashcode(xboxUrl, cookie);
        if (XSTSTokenAndHashcode == null) return false;

        AltManagerUtil.addProgressReport("Getting minecraft access token");
        JsonArray tokensArray = new Gson().fromJson(new String(Base64.getDecoder().decode(XSTSTokenAndHashcode)), JsonArray.class);
        // "Item1": "rp://api.minecraftservices.com/", minecrafts response is the 3rd token in the array for me
        JsonObject minecraftResponse = tokensArray.get(2).getAsJsonObject().get("Item2").getAsJsonObject();
        String userHashcode = minecraftResponse.get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();
        String XSTSaccessToken = minecraftResponse.get("Token").getAsString();

        String minecraftAccessToken = oAuthUtil.getMinecraftAccessToken(userHashcode, XSTSaccessToken);
        if (minecraftAccessToken == null) {
            AltManagerUtil.setErrorMessage("Minecraft access token is not valid!");
            return false;
        }

        if (ownsMinecraft(minecraftAccessToken)) {
            User session = SessionUtil.queryPlayerProfile(minecraftAccessToken);
            if (!SessionUtil.login(session)) return false;

            AltManagerUtil.currentAlt.loginData.cookie = cookie;
            return true;
        }
        else AltManagerUtil.setErrorMessage("Cookie does NOT own minecraft!");

        return false;
    }

    private static boolean ownsMinecraft(String accessToken) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/entitlements/license?requestId=checker"))
                    .setHeader("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            JsonObject resJson = NetworkUtil.getJSONObject(req);

            for (JsonElement element : resJson.get("items").getAsJsonArray()) {
                JsonObject jsonObject = element.getAsJsonObject();
                String source = jsonObject.get("source").getAsString();
                if (source.equals("PURCHASE") || source.equals("MC_PURCHASE")) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        AltManagerUtil.setErrorMessage("No license found!");
        return false;
    }

    private static String getXSTSTokenAndHashcode(String xboxUrl, String cookie) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(xboxUrl))
                    .setHeader("Cookie", cookie)
                    .GET()
                    .build();
            HttpResponse<String> res = NetworkUtil.getServerResponse(req);

            String location = res.headers().firstValue("Location").orElse("");
            if (location.contains("https://www.minecraft.net/en-us/login#state=login&accessToken="))
                return location.split("accessToken=")[1];
        } catch (Exception e) {
            e.printStackTrace();
        }

        AltManagerUtil.setErrorMessage("Access token is not present!");
        return null;
    }

    private static String getRedirectLocation(String cookie) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://sisu.xboxlive.com/connect/XboxLive/?state=login&ru=https://www.minecraft.net/en-us/login"))
                    .setHeader("Cookie", cookie)
                    .GET()
                    .build();

            HttpResponse<String> res = NetworkUtil.getServerResponse(req);

            String location = res.headers().firstValue("Location").orElse("");
            if (res.statusCode() == 302 && location.contains("oauth20_authorize.srf"))
                return NetworkUtil.fixURL(location);
        } catch (Exception e) {
            AltManagerUtil.setErrorMessage("Invalid Cookie, cannot log in to xbox account: " + e.getMessage());
        }
        return null;
    }

    private static String getXboxUrl(@Nullable String location, String cookie) {
        if (location == null) return null;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(location))
                    .setHeader("Cookie", cookie)
                    .GET()
                    .build();
            HttpResponse<String> res = NetworkUtil.getServerResponse(req);

            String location2 = res.headers().firstValue("Location").orElse("");
            if (res.statusCode() == 302 && location2.contains("code="))
                return NetworkUtil.fixURL(location2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        AltManagerUtil.setErrorMessage("Invalid Cookie, cannot log in to xbox account.");
        return null;
    }

    // hi this comment is here from scalehack v4, i have NO idea why this is here or how i came to write this, thanks past me for not leaving comments.
    public static String formatCookies(String fileLines) {
        StringBuilder cook = new StringBuilder();
        for (String s : fileLines.split("\n")) {
            String[] strings = s.split("\t");

            if (strings.length <= 6) continue;

            cook.append(strings[5]).append("=").append(strings[6]).append(";");
        }

        if (cook.length() < 2) return "";
        return cook.substring(0, cook.length() - 2);
    }
}
