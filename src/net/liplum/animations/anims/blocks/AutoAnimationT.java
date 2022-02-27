package net.liplum.animations.anims.blocks;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.gen.Building;
import net.liplum.animations.anims.IAnimatedBlockT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoAnimationT extends AutoAnimation implements IAnimatedBlockT<AutoAnimationT.Obj> {

    /**
     * @param totalDuration how long can this animation be played
     * @param allFrames     every frame which has the same duration
     */
    public AutoAnimationT(float totalDuration, TextureRegion... allFrames) {
        super(totalDuration, allFrames);
    }

    @Override
    public Obj gen() {
        return new Obj(this);
    }

    @Nullable
    public TextureRegion getCurTR(@Nullable Building tileEntity, @NotNull Obj obj) {
        int length = allFrames.length;
        if (length == 0) {
            return null;
        }
        int index = 0;
        if (indexer != null) {
            index = indexer.getCurIndex(length, tileEntity);
        }
        if (index < 0) {
            return null;
        }
        if (obj.reversed) {
            index = length - 1 - index;
        }
        return allFrames[index];
    }

    @Override
    public void draw(@NotNull Obj obj, float x, float y, Building tileEntity) {
        TextureRegion curTR = getCurTR(tileEntity, obj);
        if (curTR != null) {
            Draw.rect(curTR, x, y);
        }
    }

    @Override
    public void draw(@NotNull Obj obj, Color color, float x, float y, Building tileEntity) {
        TextureRegion curTR = getCurTR(tileEntity, obj);
        if (curTR != null) {
            Draw.color(color);
            Draw.rect(curTR, x, y);
            Draw.color();
        }
    }

    public static class Obj {
        public AutoAnimationT prototype;
        public boolean reversed;

        public Obj(AutoAnimationT prototype) {
            this.prototype = prototype;
        }
    }
}