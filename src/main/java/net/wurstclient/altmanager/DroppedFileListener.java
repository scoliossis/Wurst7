package net.wurstclient.altmanager;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DroppedFileListener {
    // only has the argument so i can pass it with DroppedFileListener::init
    public static void init(Minecraft client) {
        GLFW.glfwSetDropCallback(client.getWindow().handle(), (window, count, names) -> {
            // multiple files can be dropped, how spooky
            for (int i = 0; i < count; i++) {
                // why did they decide to HIDE the names like this.
                String filePath = GLFWDropCallback.getName(names, i);
                System.out.println(filePath);

                Path path = Paths.get(filePath);
                AltManagerUtil.loginWithFile(path);
            }
        });
    }
}
