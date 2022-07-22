package net.liplum.data

import arc.func.Prov
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.math.Mathf
import arc.math.geom.Point2
import arc.util.Time
import arc.util.io.Reads
import arc.util.io.Writes
import mindustry.gen.Building
import mindustry.graphics.Pal
import mindustry.logic.LAccess
import mindustry.type.Item
import mindustry.world.meta.BlockGroup
import net.liplum.DebugOnly
import net.liplum.R
import net.liplum.Var
import net.liplum.api.cyber.*
import net.liplum.blocks.AniedBlock
import net.liplum.common.utils.toFloat
import net.liplum.data.Sender.SenderBuild
import net.liplum.lib.Serialized
import net.liplum.lib.assets.TR
import net.liplum.lib.math.isZero
import net.liplum.mdt.CalledBySync
import net.liplum.mdt.ClientOnly
import net.liplum.mdt.SendDataPack
import net.liplum.mdt.animations.anims.Animation
import net.liplum.mdt.animations.anis.AniState
import net.liplum.mdt.animations.anis.config
import net.liplum.mdt.render.*
import net.liplum.mdt.ui.bars.AddBar
import net.liplum.mdt.utils.*
import net.liplum.utils.addReceiverInfo

private typealias AniStateS = AniState<Sender, SenderBuild>

open class Sender(name: String) : AniedBlock<Sender, SenderBuild>(name) {
    @ClientOnly lateinit var BaseTR: TR
    @ClientOnly lateinit var HighlightTR: TR
    @ClientOnly lateinit var UpArrowTR: TR
    @ClientOnly lateinit var CrossTR: TR
    @ClientOnly lateinit var NoPowerTR: TR
    @ClientOnly lateinit var UnconnectedTR: TR
    @ClientOnly lateinit var UploadAnim: Animation
    @JvmField var UploadAnimFrameNumber = 7
    @JvmField var UploadAnimDuration = 30f
    @JvmField val CheckConnectionTimer = timers++
    @JvmField val SpeedLimitTimer = timers++
    /**
     * The max range when trying to connect. -1f means no limit.
     */
    @JvmField var maxRange = -1f
    @ClientOnly @JvmField var maxSelectedCircleTime = Var.SelectedCircleTime
    @ClientOnly @JvmField var SendingTime = 60f

    init {
        buildType = Prov { SenderBuild() }
        solid = true
        update = true
        acceptsItems = true
        configurable = true
        group = BlockGroup.transportation
        canOverdrive = false
        schematicPriority = 20
        unloadable = false
        saveConfig = true
        callDefaultBlockDraw = false
        /**
         * For connect
         */
        config(Integer::class.java) { it: SenderBuild, receiver ->
            it.setReceiverFromRemote(receiver.toInt())
        }
        configClear<SenderBuild> {
            it.receiverPos = null
        }
        /**
         * For schematic
         */
        config(Point2::class.java) { it: SenderBuild, point ->
            it.resolveRelativePosFromRemote(point)
        }
    }

    override fun load() {
        super.load()
        BaseTR = this.sub("base")
        HighlightTR = this.sub("highlight")
        UpArrowTR = this.inMod("rs-up-arrow")
        CrossTR = this.inMod("rs-cross")
        UnconnectedTR = this.inMod("rs-unconnected")
        NoPowerTR = this.inMod("rs-no-power")
        UploadAnim = this.autoAnimInMod("rs-up-arrow", UploadAnimFrameNumber, UploadAnimDuration)
    }

    override fun drawPlace(x: Int, y: Int, rotation: Int, valid: Boolean) {
        super.drawPlace(x, y, rotation, valid)
        if (maxRange > 0f)
            G.dashCircleBreath(this, x, y, maxRange * smoothPlacing(maxSelectedCircleTime), R.C.Sender)
    }

    override fun setBars() {
        super.setBars()
        DebugOnly {
            addReceiverInfo<SenderBuild>()
            AddBar<SenderBuild>("last-sending",
                { "Last Send: ${lastSendingTime.toInt()}" },
                { Pal.bar },
                { lastSendingTime / SendingTime }
            )
            AddBar<SenderBuild>("queue",
                { "Queue: $queue" },
                { Pal.bar },
                { (queue != null).toFloat() }
            )
        }
    }

    override fun setStats() {
        super.setStats()
        addLinkRangeStats(maxRange)
        addMaxClientStats(1)
    }

    open inner class SenderBuild : AniedBuild(), IDataSender {
        override val maxRange = this@Sender.maxRange
        @ClientOnly var lastSendingTime = 0f
            set(value) {
                field = value.coerceAtLeast(0f)
            }
        @ClientOnly
        open val isBlocked: Boolean
            get() = lastSendingTime > SendingTime
        @set:CalledBySync
        @Serialized
        var receiverPos: Point2? = null
            set(value) {
                var curBuild = receiverPos.dr()
                curBuild?.disconnect(this)
                field = value
                curBuild = receiverPos.dr()
                curBuild?.connect(this)
            }
        val receiver: IDataReceiver?
            get() = receiverPos.dr()
        @CalledBySync
        fun setReceiverFromRemote(pos: PackedPos) {
            val unpacked = Point2.unpack(pos)
            receiverPos = if (unpacked.dr().exists) unpacked else null
        }
        @CalledBySync
        fun resolveRelativePosFromRemote(relative: Point2) {
            val abs = resolveRelativePos(relative)
            queue = abs
            if (abs.dr().exists) {
                receiverPos = abs
                queue = null
            } else {
                receiverPos = null
            }
        }
        /**
         * @param relative the relative position
         * @return
         */
        fun resolveRelativePos(relative: Point2): Point2 {
            val res = relative.cpy()
            res.x += this.tile.x
            res.y += this.tile.y
            return res // now it's absolute position
        }
        /**
         * When this sender was restored by schematic, it should check whether the receiver was built.
         *
         * It's an absolute point
         */
        @CalledBySync
        var queue: Point2? = null
        /**
         * Consider this block as (0,0)
         */
        fun genRelativePos(): Point2? {
            val abs = receiverPos?.cpy() ?: return null
            abs.x -= this.tile.x
            abs.y -= this.tile.y
            val relative = abs// now it's relative
            return relative
        }

        open fun checkReceiverPos() {
            if (receiverPos == null) return
            if (!receiverPos.dr().exists) {
                receiverPos = null
            }
        }

        override fun updateTile() {
            val waiting = queue
            if (waiting != null) {
                val dr = waiting.dr()
                if (dr != null) {
                    connectSync(dr)
                    queue = null
                }
            }
            // Check connection every second
            if (timer(CheckConnectionTimer, 60f)) {
                checkReceiverPos()
            }
            ClientOnly {
                lastSendingTime += Time.delta
            }
        }

        override fun toString() =
            "Sender#$id(->$receiverPos)"

        override val connectedReceiver: Int?
            get() = receiverPos?.pack()

        override fun handleItem(source: Building, item: Item) {
            if (!canConsume()) {
                return
            }
            val reb = receiver
            if (reb != null) {
                sendData(reb, item, 1)
                ClientOnly {
                    lastSendingTime = 0f
                }
            }
        }
        @ClientOnly
        override val senderColor: Color
            get() = receiver?.let {
                if (it.isDefaultColor) super.senderColor
                else it.receiverColor
            } ?: super.senderColor
        @ClientOnly
        override fun drawConfigure() {
            super.drawConfigure()
            this.drawDataNetGraphic()
            drawMaxRange()
        }
        @ClientOnly
        override fun drawSelect() {
            this.drawDataNetGraphic()
            drawMaxRange()
        }
        @ClientOnly
        @SendDataPack
        override fun onConfigureBuildTapped(other: Building): Boolean {
            if (this == other) {
                deselect()
                configure(null)
                return false
            }
            if (other.tileEquals(receiverPos)) {
                configure(null)
                return false
            }
            if (other is IDataReceiver) {
                if (maxRange > 0f && other.dst(this) >= maxRange) {
                    postOverRangeOn(other)
                    return false
                } else {
                    if (!canMultipleConnect) {
                        deselect()
                    }
                    if (canHaveMoreReceiverConnection()) {
                        if (other.acceptConnection(this)) {
                            connectSync(other)
                        } else {
                            postFullSenderOn(other)
                        }
                    } else {
                        postFullSenderOn(other)
                    }
                }
                return false
            }
            return true
        }
        @Synchronized
        override fun config(): Any? {
            return genRelativePos()
        }

        override fun acceptItem(source: Building, item: Item): Boolean {
            if (!canConsume()) {
                return false
            }
            val reb = receiver
            return reb?.acceptedAmount(this, item)?.isAccepted() ?: false
        }

        override fun write(write: Writes) {
            super.write(write)
            write.i(receiverPos?.pack() ?: -1)
        }

        override fun read(read: Reads, revision: Byte) {
            super.read(read, revision)
            val packPos = read.i()
            receiverPos = if (packPos != -1) packPos.unpack() else null
        }
        @SendDataPack
        override fun connectSync(receiver: IDataReceiver) {
            val target = receiver.building
            if (!target.tileEquals(receiverPos)) {
                configure(target.pos())
            }
        }
        @SendDataPack
        override fun disconnectSync(receiver: IDataReceiver) {
            if (receiver.building.tileEquals(receiverPos)) {
                configure(null)
            }
        }

        override fun control(type: LAccess, p1: Any?, p2: Double, p3: Double, p4: Double) {
            when (type) {
                LAccess.shoot ->
                    if (!p2.isZero && p1 is IDataReceiver) connectSync(p1)
                else -> super.control(type, p1, p2, p3, p4)
            }
        }

        override fun control(type: LAccess, p1: Double, p2: Double, p3: Double, p4: Double) {
            when (type) {
                LAccess.shoot -> {
                    val receiver = buildAt(p1, p2)
                    if (!p3.isZero && receiver is IDataReceiver) connectSync(receiver)
                }
                else -> super.control(type, p1, p2, p3, p4)
            }
        }

        override fun sense(sensor: LAccess): Double {
            return when (sensor) {
                LAccess.shootX -> receiver.tileXd
                LAccess.shootY -> receiver.tileYd
                else -> super.sense(sensor)
            }
        }
        @ClientOnly @JvmField
        var highlightAlpha = 1f
        override fun fixedDraw() {
            BaseTR.DrawOn(this)
            if (aniStateM.curState == IdleAni) {
                highlightAlpha = Mathf.approach(highlightAlpha, 1f, 0.01f)
                Draw.alpha(highlightAlpha)
                HighlightTR.DrawOn(this)
                Draw.color()
            } else {
                highlightAlpha = Mathf.approach(highlightAlpha, Var.rsSlightHighlightAlpha, 0.01f)
                Draw.alpha(highlightAlpha)
                HighlightTR.DrawOn(this)
                Draw.color()
            }
        }
    }

    @ClientOnly lateinit var IdleAni: AniStateS
    @ClientOnly lateinit var UploadAni: AniStateS
    @ClientOnly lateinit var BlockedAni: AniStateS
    @ClientOnly lateinit var NoPowerAni: AniStateS
    @ClientOnly
    override fun genAniState() {
        IdleAni = addAniState("Idle")
        UploadAni = addAniState("Upload") {
            UploadAnim.draw(Color.green, x, y)
        }
        BlockedAni = addAniState("Blocked") {
            SetColor(R.C.Stop)
            UpArrowTR.Draw(x, y)
        }
        NoPowerAni = addAniState("NoPower") {
            NoPowerTR.Draw(x, y)
        }
    }
    @ClientOnly
    override fun genAniConfig() {
        config {
            // Idle
            From(IdleAni) To UploadAni When {
                val reb = receiver
                reb != null
            } To NoPowerAni When {
                !canConsume()
            }
            // Upload
            From(UploadAni) To IdleAni When {
                receiverPos == null
            } To BlockedAni When {
                val reb = receiver
                reb != null && isBlocked
            } To NoPowerAni When {
                !canConsume()
            }
            // Blocked
            From(BlockedAni) To IdleAni When {
                receiverPos == null
            } To UploadAni When {
                val reb = receiver
                reb != null && !isBlocked
            } To NoPowerAni When {
                !canConsume()
            }
            // NoPower
            From(NoPowerAni) To IdleAni When {
                canConsume()
            }
        }
    }
}