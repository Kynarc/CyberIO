package net.liplum.mdt.render

import arc.Core
import arc.graphics.Blending
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.math.Mathf
import mindustry.gen.Building
import mindustry.graphics.Layer
import mindustry.world.Block
import mindustry.world.blocks.heat.HeatBlock
import mindustry.world.draw.DrawBlock
import net.liplum.lib.assets.TR

data class HeatMeta(
    var heatColor: Color = Color(1f, 0.22f, 0.22f, 0.8f),
    var heatPulse: Float = 0.3f,
    var heatPulseScl: Float = 10f,
    var glowMultiplier: Float = 1.2f,
)

fun HeatMeta.drawHeat(b: Building, tr: TR, heatFrac: Float) {
    if (heatFrac > 0f) {
        Draw.z(Layer.blockAdditive)
        Draw.blend(Blending.additive)
        Draw.color(
            heatColor,
            heatFrac * (heatColor.a * (1f - heatPulse + Mathf.absin(heatPulseScl, heatPulse)))
        )
        Draw.rect(tr, b.x, b.y)
        Draw.blend()
        Draw.color()
        Draw.z(Layer.block)
    }
}

fun HeatMeta.drawHeatAt(x: Float, y: Float, tr: TR, heatFrac: Float) {
    if (heatFrac > 0f) {
        Draw.z(Layer.blockAdditive)
        Draw.blend(Blending.additive)
        Draw.color(
            heatColor,
            heatFrac * (heatColor.a * (1f - heatPulse + Mathf.absin(heatPulseScl, heatPulse)))
        )
        Draw.rect(tr, x, y)
        Draw.blend()
        Draw.color()
        Draw.z(Layer.block)
    }
}

inline fun HeatMeta.drawHeat(heatFrac: Float, draw: () -> Unit) {
    if (heatFrac > 0f) {
        Draw.blend(Blending.additive)
        Draw.color(
            heatColor,
            heatFrac * (heatColor.a * (1f - heatPulse + Mathf.absin(heatPulseScl, heatPulse)))
        )
        draw()
        Draw.blend()
        Draw.color()
    }
}

fun HeatMeta.drawHeat(b: Building, tr: TR) {
    if (b is HeatBlock) {
        val heatFrac = b.heatFrac()
        if (heatFrac > 0f) {
            Draw.z(Layer.blockAdditive)
            Draw.blend(Blending.additive)
            Draw.color(
                heatColor,
                heatFrac * (heatColor.a * (1f - heatPulse + Mathf.absin(heatPulseScl, heatPulse)))
            )
            Draw.rect(tr, b.x, b.y)
            Draw.blend()
            Draw.color()
            Draw.z(Layer.block)
        }
    }
}

class DrawHeat(
    var suffix: String = "-heat",
) : DrawBlock() {
    lateinit var heatTR: TR
    var heatColor = Color(1f, 0.22f, 0.22f, 0.8f)
    var heatPulse = 0.3f
    var heatPulseScl: Float = 10f
    var glowMult: Float = 1.2f
    override fun draw(b: Building) = b.run {
        if (!heatTR.found()) return@run
        if (this is HeatBlock) {
            val frac = heatFrac()
            if (frac > 0f) {
                Draw.z(Layer.blockAdditive)
                Draw.z(Layer.blockAdditive)
                Draw.blend(Blending.additive)
                Draw.color(
                    heatColor,
                    frac * (heatColor.a * (1f - heatPulse + Mathf.absin(heatPulseScl, heatPulse)))
                )
                Draw.rect(heatTR, b.x, b.y)
                Draw.blend()
                Draw.color()
                Draw.z(Layer.block)
            }
        }
    }

    override fun load(block: Block) {
        heatTR = Core.atlas.find(block.name + suffix)
    }
}