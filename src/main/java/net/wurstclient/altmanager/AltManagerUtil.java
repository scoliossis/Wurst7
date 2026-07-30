package net.wurstclient.altmanager;

import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.wurstclient.C;
import net.wurstclient.WurstClient;
import net.wurstclient.util.RenderUtils;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;

public class AltManagerUtil {
    public static final ArrayList<Alt> ALTS = new ArrayList<>();
    private static final File ALTS_FOLDER = new File(WurstClient.ALTS_PATH);

    public static Alt currentAlt;

    private enum oAuthIds {
        LOCALTS("00000000402b5328", "service::user.auth.xboxlive.com::MBI_SSL");

        private final String CLIENT_ID;
        private final String SCOPE;

        oAuthIds(String clientId, String scope) {
            CLIENT_ID = clientId;
            SCOPE = scope;
        }
    }

    // todo: add more file types, idk
    private enum File_Types {
        /* matches for
        Thanks for choosing Localts!
        AlahEater69:M.C525_BL2.0.U.MsaArtifacts.-CoqpLE42UWWrxY3v7ohpr... etc
         */
        LOCALTS_REFRESH(Pattern.compile("^Thanks for choosing Localts!\\n(?:\\w{3,16}:M\\.C\\d{3}_\\w{3}\\.0\\.U\\.MsaArtifacts\\.-[^\\n]+\\n?)+$")) {
            @Override
            void attemptLogin(String fileText) {
                String[] split = fileText.split("\n");
                for (int i = 1; i < split.length; i++) {
                    String name = split[i].split(":")[0];
                    String refreshToken = split[i].split(":")[1];
                    if (ALTS.stream().anyMatch(alt -> name.equals(alt.name) || refreshToken.equals(alt.loginData.refreshToken))) continue;

                    boolean successful = oAuthUtil.authWithRefreshToken(refreshToken, oAuthIds.LOCALTS.CLIENT_ID, oAuthIds.LOCALTS.SCOPE, oAuthUtil.DEFAULT_REDIRECT, false);
                    if (successful) AltManagerUtil.currentAlt.save();
                }
            }
        },
        // today years old when i found out about Pattern.MULTILINE and CASE_INSENSITIVE etc.
        // matches for https://everything.curl.dev/http/cookies/fileformat.html
        COOKIE(Pattern.compile("^([^\\t]+)\\t(TRUE|FALSE)\\t([^\\t]*)\\t(TRUE|FALSE)\\t(\\d+)\\t([^\\t]+)\\t(.*)$", Pattern.MULTILINE)) {
            @Override
            void attemptLogin(String fileText) {
                CookieAltUtil.authenticate(CookieAltUtil.formatCookies(fileText));
            }
        };

        private final Pattern REGEX;

        File_Types(Pattern regex) {
            REGEX = regex;
        }

        abstract void attemptLogin(String fileText);
    }

    public static void loadAlts() {
        currentAlt = Alt.getCurrent();
        ALTS.add(currentAlt);
        if (SessionUtil.queryPlayerProfile(C.mc.getUser().getAccessToken()) != null) currentAlt.save();

        if (!ALTS_FOLDER.exists() && !ALTS_FOLDER.mkdirs()) return;

        for (File file : ALTS_FOLDER.listFiles()) {
            if (!file.getName().endsWith(".json")) continue;

            try {
                Alt alt = C.gson.fromJson(FileUtils.readFileToString(file, Charset.defaultCharset()), Alt.class);
                if (ALTS.stream().anyMatch(a -> a.uuid.equals(alt.uuid))) continue;

                ALTS.add(alt);
                alt.loadSkin();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to load alt " + file.getName());
            }
        }
    }

    private static Alt getAlt(UUID uuid) {
        return getAlt(new File(WurstClient.ALTS_PATH, uuid + ".json"));
    }

    private static Alt getAlt(File file) {
        if (!file.exists()) return null;

        try {
            return C.gson.fromJson(FileUtils.readFileToString(file, Charset.defaultCharset()), Alt.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // todo: add skin, ban data, name change data
    public static class Alt {
        public String name;

        public Alt(String name, UUID uuid, LoginData loginData, long lastLogin, BufferedImage skinImage, Identifier skin) {
            this.name = name;
            this.uuid = uuid;
            this.loginData = loginData;
            this.lastLogin = lastLogin;
            this.skinImage = skinImage;
            this.skin = skin;
        }

        public UUID uuid;
        public LoginData loginData;
        public long lastLogin;

        public BufferedImage skinImage;
        public Identifier skin;

        public static Alt getCurrent() {
            Alt alt = getAlt(C.mc.getUser().getProfileId());
            if (alt != null) return alt;

            return new Alt(
                    C.mc.getUser().getName(),
                    C.mc.getUser().getProfileId(),
                    new LoginData(C.mc.getUser().getAccessToken(), null, null, null, null),
                    System.currentTimeMillis(),
                    null,
                    DefaultPlayerSkin.getDefaultTexture()
            );
        }

        public void login() {
            if (loginData.attemptLogin()) this.save();
        }

        /// dont call too often, because it requires a request to update skin,
        public void save() {
            if (!ALTS_FOLDER.exists() && !ALTS_FOLDER.mkdirs()) return;

            C.mc.execute(() -> {
                try {
                    currentAlt = this;
                    this.lastLogin = System.currentTimeMillis();
                    System.out.println("Saved alt " + this.name + " : " + this.lastLogin);
                    this.loginData.ssid = C.mc.getUser().getAccessToken();
                    updateSkin();
                    Files.write(new File(ALTS_FOLDER, this.uuid.toString() + ".json").toPath(), this.toString().getBytes());

                    // refresh alts that arent really real
                    ALTS.removeIf(alt -> !alt.getFile().exists());

                    // in case u resaving an alt
                    AltManagerUtil.ALTS.removeIf(alt -> alt.uuid.equals(AltManagerUtil.currentAlt.uuid));
                    AltManagerUtil.ALTS.add(AltManagerUtil.currentAlt);
                } catch (IOException e) {
                    e.printStackTrace();
                    setErrorMessage("Failed to save alt: " + e.getMessage());
                }
            });
        }


        public void delete() {
            ALTS.remove(this);
            try {
                Files.delete(getFile().toPath());
            } catch (IOException e) {
                setErrorMessage("Failed to delete alt: " + e.getMessage());
            }
        }

        public void updateSkin() {
            try {
                this.skinImage = ImageIO.read(new URL(SessionUtil.getSkin(this.uuid.toString())));
                this.skin = getSkinIdentifier();
                RenderUtils.createTexture(this.skin, this.skinImage);
            } catch (IOException e) {
                System.err.println("Failed to update alt skin: " + e.getMessage());
            }
        }

        public void loadSkin() {
            if (this.skinImage == null) return;

            C.mc.execute(() -> {
                RenderUtils.createTexture(getSkinIdentifier(), this.skinImage);
            });
        }

        private Identifier getSkinIdentifier() {
            return Identifier.parse("skincache/" + this.uuid.toString());
        }

        public File getFile() {
            return new File(ALTS_FOLDER, this.uuid.toString() + ".json");
        }

        @Override
        public String toString() {
            return C.gson.toJson(this);
        }
    }

    public static class LoginData {
        String ssid, cookie, refreshToken, clientId, scope;

        public LoginData(String ssid, String cookie, String refreshToken, String clientId, String scope) {
            this.ssid = ssid;
            this.cookie = cookie;
            this.refreshToken = refreshToken;
            this.clientId = clientId;
            this.scope = scope;
        }

        public boolean attemptLogin() {
            if (ssid != null && SessionUtil.login(SessionUtil.queryPlayerProfile(ssid))) return true;
            if (refreshToken != null && clientId != null && oAuthUtil.authWithRefreshToken(refreshToken, clientId, scope, oAuthUtil.DEFAULT_REDIRECT, false)) return true;
            return cookie != null && CookieAltUtil.authenticate(cookie);
        }
    }

    public static void addProgressReport(String message) {
        System.out.println("PROGRESS PEOPLE! " + message);
    }
    public static void setErrorMessage(String message) {
        System.out.println("its all gone wrong: " + message);
    }

    public static void loginWithFile(Path path) {
        try {
            // todo: unzip if its a zip, ik localts used to sell zipped cookies for wtv reason
            String fileText = String.join("\n", Files.readAllLines(path));
            if (fileText.isEmpty()) return;

            for (File_Types type : File_Types.values()) {
                if (type.REGEX.matcher(fileText).find()) {
                    type.attemptLogin(fileText);
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
