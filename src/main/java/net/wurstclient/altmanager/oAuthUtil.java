package net.wurstclient.altmanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.User;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/utils/alts/microsoft/MSAuth.java
public class oAuthUtil {
    public static String getMinecraftAccessToken(String userHashcode, String XSTSaccessToken) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
                    .setHeader("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{ \"identityToken\": \"XBL3.0 x="+userHashcode+";"+XSTSaccessToken+"\" }"))
                    .build();

            HttpResponse<String> res = NetworkUtil.getServerResponse(req);
            JsonObject resJson = NetworkUtil.getJSONObject(res);

            // https://http.cat/status/429
            if (res.statusCode() == 429) {
                AltManagerUtil.setErrorMessage("Rate limited, please wait.");
                return null;
            }

            if (resJson.has("access_token")) {
                AltManagerUtil.addProgressReport("Access token found!");
                return resJson.get("access_token").getAsString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        AltManagerUtil.setErrorMessage("No microsoft access token present!");
        return null;
    }

    // we only use the different redirect for the AuthServer because it is awaiting a response from localthost specifically
    public static final String DEFAULT_REDIRECT = "https://login.live.com/oauth20_desktop.srf";

    private static AccessRefreshToken refreshToken(String key, String clientId, String scope, String redirect, boolean code) throws Exception {
        // shrug, works, previously this used EVIL http3 stuff
        String postContent =
                "client_id=" + clientId +
                        (code ? "&code=" : "&refresh_token=") + key +
                        "&grant_type=" + (code ? "authorization_code" : "refresh_token") +
                        "&redirect_uri=" + redirect +
                        "&scope=" + scope;

        HttpRequest req = HttpRequest.newBuilder(URI.create("https://login.live.com/oauth20_token.srf"))
                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(postContent))
                .build();

        JsonObject res = NetworkUtil.getJSONObject(req);
        if (res.has("error_description")) {
            AltManagerUtil.setErrorMessage(res.get("error_description").getAsString());
            return null;
        }
        String accessToken = res.get("access_token").getAsString();
        String newRefreshToken = res.get("refresh_token").getAsString();

        AltManagerUtil.addProgressReport("Successfully refreshed refresh token!");
        return new AccessRefreshToken(accessToken, newRefreshToken);
    }

    // "formal" seems to be random lwk, localts doesnt like the d=, but i do personally
    private static String authXBL(String authToken, boolean formal) throws Exception {
        JsonObject payload = new JsonObject();
        JsonObject payloadProps = new JsonObject();
        payloadProps.addProperty("AuthMethod", "RPS");
        payloadProps.addProperty("SiteName", "user.auth.xboxlive.com");
        payloadProps.addProperty("RpsTicket", (formal ? "d=" : "") + authToken);
        payload.add("Properties", payloadProps);
        payload.addProperty("RelyingParty", "http://auth.xboxlive.com");
        payload.addProperty("TokenType", "JWT");

        HttpRequest req = HttpRequest.newBuilder(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject res = NetworkUtil.getJSONObject(req);
        // try again without the d=
        if (res == null && formal) {
            return authXBL(authToken, false);
        }
        if (res.has("Token")) {
            AltManagerUtil.addProgressReport("Successfully authed xbl!");
            return NetworkUtil.getJSONObject(req).get("Token").getAsString();
        }

        if (res.has("error_description")) {
            AltManagerUtil.setErrorMessage(res.get("error_description").getAsString());
            return null;
        }
        return null;
    }

    // XBLToken from authXBL()
    private static XBLTokenUhs authXSTS(String XBLToken) throws Exception {
        JsonObject payload = new JsonObject();
        JsonObject payloadProps = new JsonObject();
        JsonArray userTokens = new JsonArray();
        userTokens.add(new JsonPrimitive(XBLToken));
        payloadProps.add("UserTokens", userTokens);
        payloadProps.addProperty("SandboxId", "RETAIL");
        payload.add("Properties", payloadProps);
        payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        payload.addProperty("TokenType", "JWT");

        HttpRequest req = HttpRequest.newBuilder(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject res = NetworkUtil.getJSONObject(req);
        if (res.has("error_description")) {
            AltManagerUtil.setErrorMessage(res.get("error_description").getAsString());
            return null;
        }

        String token = res.getAsJsonObject().get("Token").getAsString();
        String uhs = res
                .getAsJsonObject()
                .get("DisplayClaims")
                .getAsJsonObject()
                .get("xui")
                .getAsJsonArray()
                .get(0)
                .getAsJsonObject()
                .get("uhs")
                .getAsString();

        AltManagerUtil.addProgressReport("Successfully authed xsts!");
        return new XBLTokenUhs(token, uhs);
    }

    public static User authWithAccessRefreshToken(AccessRefreshToken accessRefreshToken) throws Exception {
        String XBLToken = authXBL(accessRefreshToken.accessToken, true);
        XBLTokenUhs xblTokenUhs = authXSTS(XBLToken);
        String accessToken = getMinecraftAccessToken(xblTokenUhs.uhs, xblTokenUhs.XBLToken);

        return SessionUtil.queryPlayerProfile(accessToken);
    }

    public static boolean authWithRefreshToken(String key, String clientId, String scope, String redirect, boolean code) {
        try {
            AccessRefreshToken accessRefreshToken = refreshToken(key, clientId, scope, redirect, code);
            if (accessRefreshToken == null) return false;

            User session = authWithAccessRefreshToken(accessRefreshToken);
            if (session == null) return false;

            if (SessionUtil.login(session)) {
                AltManagerUtil.currentAlt.loginData.refreshToken = accessRefreshToken.refreshToken;
                AltManagerUtil.currentAlt.loginData.clientId = clientId;
                AltManagerUtil.currentAlt.loginData.scope = scope;

                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static class AccessRefreshToken {

        public final String accessToken;
        public final String refreshToken;

        public AccessRefreshToken(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    private static class XBLTokenUhs {

        public final String XBLToken;
        public final String uhs;

        public XBLTokenUhs(String XBLToken, String uhs) {
            this.XBLToken = XBLToken;
            this.uhs = uhs;
        }
    }
}