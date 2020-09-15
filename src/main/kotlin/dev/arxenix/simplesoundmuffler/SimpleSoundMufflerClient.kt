package dev.arxenix.simplesoundmuffler

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(EnvType.CLIENT)
class SimpleSoundMufflerClient: ClientModInitializer {
    override fun onInitializeClient() {
        println("Initialize SimpleSoundMuffler Client")
    }
}