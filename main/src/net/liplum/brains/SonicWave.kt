package net.liplum.brains

import arc.util.io.Reads
import arc.util.io.Writes
import net.liplum.lib.entity.PosRadiation
import net.liplum.lib.entity.Queue

class SonicWave(
    range: Float = 0f,
    x: Float = 0f,
    y: Float = 0f,
    var damage: Float = 0f,
) : PosRadiation(range, x, y) {
    override fun read(reader: Reads) {
        super.read(reader)
        damage = reader.f()
    }

    override fun write(writer: Writes) {
        super.write(writer)
        writer.f(damage)
    }
}
fun SonicWaveQueue(size: Int = 1) =
    Queue(size, ::SonicWave)
