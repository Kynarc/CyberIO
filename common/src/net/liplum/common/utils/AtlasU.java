package net.liplum.common.utils;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.util.Log;
import org.jetbrains.annotations.NotNull;

public class AtlasU {
    public static TextureRegion[] subFrames(String identity, int start, int number) {
        TextureRegion[] fms = new TextureRegion[number];
        int end = number + start;
        for (int i = start; i < end; i++) {
            fms[i - start] = Core.atlas.find(identity + i);
        }
        return fms;
    }

    public static TextureRegion[] animation(String identity, boolean isHorizontal, int number) {
        TextureRegion tr = Core.atlas.find(identity);
        if (!Core.atlas.isFound(tr)) {
            String possibleName = identity.substring(0, identity.length() - 5);
            String possibility = Core.atlas.isFound(Core.atlas.find(possibleName)) ?
                    "Maybe it's " + possibleName + "?" : "";
            Log.warn("Can't find texture[" + identity + "]." + possibility);
        }
        return slice(tr, number, isHorizontal);
    }

    public static TextureRegion[] sheet(@NotNull String identity, int number, boolean isHorizontal) {
        TextureRegion tr = Core.atlas.find(identity);
        return slice(tr, number, isHorizontal);
    }

    public static TextureRegion[] slice(TextureRegion original, int count, boolean isHorizontal) {
        TextureRegion[] fms = new TextureRegion[count];
        int width = original.width;
        int height = original.height;
        if (isHorizontal) {
            TextureRegion[][] split = original.split(width / count, height);
            for (int i = 0; i < count; i++) {
                fms[i] = split[i][0];
            }
        } else {
            TextureRegion[][] split = original.split(width, height / count);
            System.arraycopy(split[0], 0, fms, 0, count);
        }
        return fms;
    }
}
