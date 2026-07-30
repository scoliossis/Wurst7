package net.wurstclient.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

// "com.google.gson.JsonIOException: Failed making field 'java.awt.image.BufferedImage#imageType' accessible; either increase its visibility or write a custom TypeAdapter for its declaring type."
public class BufferedImageTypeAdapter extends TypeAdapter<BufferedImage> {
    @Override
    public void write(JsonWriter out, BufferedImage image) throws IOException {
        if (image == null) {
            out.nullValue();
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        out.value(Base64.getEncoder().encodeToString(baos.toByteArray()));
    }

    @Override
    public BufferedImage read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(in.nextString())));
    }
}
