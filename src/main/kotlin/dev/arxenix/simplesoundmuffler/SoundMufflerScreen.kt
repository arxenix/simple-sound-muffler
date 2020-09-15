package dev.arxenix.simplesoundmuffler

import drawer.put
import io.github.cottonmc.cotton.gui.client.CottonClientScreen
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription
import io.github.cottonmc.cotton.gui.widget.*
import io.github.cottonmc.cotton.gui.widget.data.Axis
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.LiteralText
import net.minecraft.util.Identifier
import java.util.function.BiConsumer
import java.util.function.Consumer


class SoundMufflerScreen(val description: SoundMufflerGui) : CottonClientScreen(description) {
    override fun isPauseScreen(): Boolean {
        return false
    }
}

class SoundMufflerGui(val blockEntity: SoundMufflerBlockEntity): LightweightGuiDescription() {
    val observedSounds = mutableSetOf<Identifier>()
    val observedSoundList = mutableListOf<Identifier>()
    var listPanel: WListPanel<Identifier, ListItem>? = null

    init {
        for (se in blockEntity.data.soundEvents) {
            observedSounds.add(se)
            observedSoundList.add(se)
        }
    }

    fun observeSound(sound: Identifier) {
        if (observedSounds.add(sound)) {
            observedSoundList.add(sound)
            this.listPanel?.layout()
        }
    }

    fun sendUpdatePacket() {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(blockEntity.pos)
        val tag = CompoundTag()
        SoundMufflerData.serializer().put(blockEntity.data, tag)
        buf.writeCompoundTag(tag)
        ClientSidePacketRegistry.INSTANCE.sendToServer(SimpleSoundMuffler.UPDATE_SOUND_MUFFLER_PACKET, buf)
    }

    init {
        val root = WGridPanel()
        setRootPanel(root)
        root.setSize(252, 200)

        listPanel = WListPanel<Identifier, ListItem>(
            observedSoundList,
            {
                ListItem(BiConsumer { listItem, toggle ->
                    listItem.soundEvent?.let { soundEvent ->
                        assert(toggle == !blockEntity.data.soundEvents.contains(soundEvent))
                        if (toggle) {
                            blockEntity.data.soundEvents.add(soundEvent)
                        } else {
                            blockEntity.data.soundEvents.remove(soundEvent)
                        }

                        sendUpdatePacket()
                    }
                })
            },
            { s: Identifier, destination: ListItem ->
                destination.soundEvent = s
                destination.button.toggle = blockEntity.data.soundEvents.contains(s)
                destination.button.label = LiteralText(s.toString())
            }
        ).setListItemHeight(20)
        root.add(listPanel, 0, 0, 14, 9)
        val modeButton = WButton(LiteralText("Mode: ${blockEntity.data.mode.name}"))
        modeButton.onClick = Runnable {
            blockEntity.data.mode = SoundMufflerMode.values()[(blockEntity.data.mode.ordinal + 1) % SoundMufflerMode.values().size]
            modeButton.label = LiteralText("Mode: ${blockEntity.data.mode.name}")

            sendUpdatePacket()
        }
        root.add(modeButton, 0, 10, 6, 1)

        val muffleSlider = WSlider(0, 100, Axis.HORIZONTAL)
        muffleSlider.value = blockEntity.data.muffleAmount
        muffleSlider.setDraggingFinishedListener {
            blockEntity.data.muffleAmount = it

            sendUpdatePacket()
        }

        root.add(WText(LiteralText("Muffle Amount")), 7, 10, 7, 1)
        root.add(muffleSlider, 7, 11, 7, 1)
        root.validate(this)
    }
}

class ListItem(onToggle: BiConsumer<ListItem, Boolean>, enabled: Boolean = true) : WPlainPanel() {
    var button = WToggleButton(LiteralText("placeholder"))
    var soundEvent: Identifier? = null

    init {
        button.onToggle = Consumer { toggle ->
            onToggle.accept(this, toggle)
        }
        this.add(button, 0, 0, 13 * 18, 20)
        setSize(13 * 18, 20)
    }
}