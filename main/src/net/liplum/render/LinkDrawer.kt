package net.liplum.render

import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Groups
import net.liplum.Settings
import net.liplum.annotations.Subscribe
import net.liplum.api.cyber.IDataSender
import net.liplum.api.cyber.IStreamHost
import net.liplum.api.cyber.drawDataNetGraph
import net.liplum.api.cyber.drawStreamGraph
import net.liplum.mdt.ClientOnly

object LinkDrawer {
    @JvmStatic
    @ClientOnly
    @Subscribe(EventType.Trigger.drawOver)
    fun draw() {
        if (!Settings.AlwaysShowLink) return
        val curTeam = Vars.player.team()
        Groups.build.each {
            if (it.team != curTeam) return@each
            if (it is IDataSender) {
                it.drawDataNetGraph(showCircle = Settings.ShowLinkCircle)
            } else if (it is IStreamHost) {
                it.drawStreamGraph(showCircle = Settings.ShowLinkCircle)
            }
        }
    }
}