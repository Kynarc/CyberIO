package net.liplum.utils;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import mindustry.ctype.MappableContent;
import net.liplum.Meta;

public class AtlasUtil {
    public static TextureRegion sub(MappableContent content, String subName) {
        return Core.atlas.find(content.name + '-' + subName);
    }

    public static TextureRegion cio(String name) {
        return Core.atlas.find(Meta.ModID + '-' + name);
    }

    public static TextureRegion[] subFrames(MappableContent content, String subName, int number) {
        return subFrames(content, subName, 0, number);
    }

    public static TextureRegion[] subFrames(MappableContent content, String subName, int start, int number) {
        TextureRegion[] fms = new TextureRegion[number];
        int end = number + start;
        for (int i = start; i < end; i++) {
            fms[i - start] = Core.atlas.find(content.name + "-" + subName + "-" + i);
        }
        return fms;
    }

    public static TextureRegion[] animation(MappableContent content, String subName, int number) {
        return animation(content, subName, true, number);
    }

    public static TextureRegion[] animation(MappableContent content, String subName, boolean isHorizontal, int number) {
        String identity = content.name + '-' + subName + "-anim";
        TextureRegion tr = Core.atlas.find(identity);
        return slice(tr, isHorizontal, number);
    }

    public static TextureRegion[] animationCio(String name, int number) {
        return animationCio(name, true, number);
    }

    public static TextureRegion[] animationCio(String name, boolean isHorizontal, int number) {
        String identity = Meta.ModID + '-' + name + "-anim";
        TextureRegion tr = Core.atlas.find(identity);
        return slice(tr, isHorizontal, number);
    }

    public static TextureRegion[] slice(TextureRegion original, boolean isHorizontal, int count) {
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
            for (int i = 0; i < count; i++) {
                fms[i] = split[0][i];
            }
        }
        return fms;
    }
}