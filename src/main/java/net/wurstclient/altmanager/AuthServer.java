package net.wurstclient.altmanager;

import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// stolen from mushroom client (which batman said is stolen from ias)
// https://login.microsoftonline.com/consumers/oauth2/v2.0/logout
public class AuthServer {
    // ias client id
    private static final String CLIENT_ID = "54fd49e4-2103-4044-9603-2b028c814ec3";
    private static final int PORT = 59125;
    private static final String REDIRECT_URL = "http://localhost:"+PORT+"/";
    private static final String SCOPE = "XboxLive.signin XboxLive.offline_access";

    public static final String URL = "https://login.live.com/oauth20_authorize.srf" +
            "?client_id=" + CLIENT_ID +
            "&response_type=code" +
            "&scope=XboxLive.signin%20XboxLive.offline_access" +
            "&prompt=select_account" +
            "&redirect_uri="+REDIRECT_URL;

    private final Pattern codePattern = Pattern.compile("\\?code=(.+?)$", 0);

    public AuthServer() {
        new Thread(this::start).start();
    }

    private void start() {
        try {
            HttpServer s = HttpServer.create(
                    new InetSocketAddress("127.0.0.1", PORT),
                    0
            );
            s.createContext("/", exchange -> {
                OutputStream out = exchange.getResponseBody();
                exchange.sendResponseHeaders(200, "".getBytes().length);
                out.write("".getBytes());
                out.flush();
                out.close();

                Matcher m = codePattern.matcher(
                        exchange.getRequestURI().toString()
                );
                if (m.find()) {
                    String code = m.group(1);
                    try {
                        AltManagerUtil.addProgressReport("Logging into Microsoft account...");
                        oAuthUtil.authWithRefreshToken(code, CLIENT_ID, SCOPE, REDIRECT_URL, true);
                        AltManagerUtil.currentAlt.save();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}