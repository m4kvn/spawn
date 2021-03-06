package com.m4kvn.spawn.command

import com.m4kvn.spawn.Main
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.GenericArguments
import org.spongepowered.api.command.spec.CommandExecutor
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors
import java.util.concurrent.TimeUnit

class SpawnCommand : CommandExecutor, KoinComponent {
    private val plugin: Main by inject()

    companion object {
        private const val PERMISSION_MAIN = "spawn.command.spawn.main"
        private const val PERMISSION_SET = "spawn.command.spawn.set"
        private const val DELAY = 3

        private fun newSpec() =
            CommandSpec.builder()
                .permission(PERMISSION_MAIN)
                .executor(SpawnCommand())
                .arguments(
                    GenericArguments.flags()
                        .permissionFlag(PERMISSION_SET, "s")
                        .buildWith(GenericArguments.none())
                )
                .build()

        fun register(plugin: Any) {
            Sponge.getCommandManager()
                .register(plugin, newSpec(), "spawn", "s")
        }
    }

    override fun execute(src: CommandSource, args: CommandContext): CommandResult {
        if (src !is Player) return CommandResult.empty()

        if (args.hasAny("s")) {
            src.world.properties.spawnPosition =
                    src.transform.position.toInt()
            val text = Text.builder("Set your location to the spawn location.")
                .color(TextColors.AQUA)
                .build()
            src.sendMessage(text)
            return CommandResult.success()
        }

        val executer = Sponge.getScheduler().createSyncExecutor(plugin)
        val disposable = CompositeDisposable()

        Observable.interval(0, 1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.from(executer))
            .observeOn(Schedulers.from(executer))
            .subscribeBy {
                val num = DELAY - it
                if (num > 0) {
                    val text = Text.builder("You warp to spawn in ")
                        .append(Text.builder("$num").color(TextColors.AQUA).build())
                        .append(Text.of(" second."))
                        .build()
                    src.sendMessage(text)
                } else {
                    val worldName = Sponge.getServer().defaultWorldName
                    val world = Sponge.getServer().getWorld(worldName).get()
                    src.location = world.spawnLocation.asHighestLocation()
                    disposable.clear()
                }
            }.addTo(disposable)

        return CommandResult.success()
    }
}