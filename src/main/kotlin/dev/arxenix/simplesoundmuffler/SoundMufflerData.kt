@file:UseSerializers(ForIdentifier::class)
package dev.arxenix.simplesoundmuffler

import drawer.ForIdentifier
import kotlinx.serialization.*
import net.minecraft.util.Identifier

enum class SoundMufflerMode {
    ALLOW,
    DENY
}

@Serializable
data class SoundMufflerData(
    var mode: SoundMufflerMode = SoundMufflerMode.ALLOW,
    var soundEvents: MutableSet<Identifier> = mutableSetOf(),
    var muffleAmount: Int = 100,
    var radius: Int = 3
)
