package net.wurstclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.wurstclient.util.BufferedImageTypeAdapter;

import java.awt.image.BufferedImage;
import java.net.http.HttpClient;
import java.util.concurrent.ForkJoinPool;

// todo: use this, i dont think typing WurstClient.MC is very fast :pensive:
// C is supposed to stand for "constants" but it kinda stands for "client variables" now.
public class C {
    // todo: remake event bus and modules aswell </3
    //public static final Reflections REFLECTIONS = new Reflections(Main.class.getPackage().getName());
    public static final Minecraft mc = Minecraft.getInstance();
    public static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            // sigh. i miss java 8.
            .registerTypeAdapter(BufferedImage.class, new BufferedImageTypeAdapter())
            .create();

    public static final ForkJoinPool ASYNC = ForkJoinPool.commonPool();
    public static final HttpClient CLIENT = HttpClient.newHttpClient();


    // C.p
    public static LocalPlayer p() {
        return mc.player;
    }

    public static ClientLevel w() {
        return mc.level;
    }

    public static MultiPlayerGameMode im() {
        return mc.gameMode;
    }

    public static Window res() {
        return mc.getWindow();
    }

    public static boolean isInGame() {
        return w() != null && p() != null;
    }

    public static int tick = 0;
}
