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


        // TITLE
        root.add(WText(LiteralText("Sound Muffler")), 2, 0, 6, 1)


        // SOUND LIST
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
        root.add(listPanel, 0, 1, 14, 8)


        // MODE SWITCH
        val modeButton = WButton(LiteralText("Mode: ${blockEntity.data.mode.name}"))
        modeButton.onClick = Runnable {
            blockEntity.data.mode = SoundMufflerMode.values()[(blockEntity.data.mode.ordinal + 1) % SoundMufflerMode.values().size]
            modeButton.label = LiteralText("Mode: ${blockEntity.data.mode.name}")

            sendUpdatePacket()
        }
        root.add(modeButton, 7, 0, 6, 1)


        // MUFFLE SLIDER
        val muffleSlider = WSlider(0, 100, Axis.HORIZONTAL)
        muffleSlider.value = blockEntity.data.muffleAmount
        val muffleText = WText(LiteralText("Muffle: ${blockEntity.data.muffleAmount}%"))
        muffleSlider.setValueChangeListener {
            blockEntity.data.muffleAmount = it
            muffleText.text = LiteralText("Muffle: ${blockEntity.data.muffleAmount}%")
        }
        // only send packet on dragging finished
        muffleSlider.setDraggingFinishedListener {
            blockEntity.data.muffleAmount = it
            sendUpdatePacket()
        }
        root.add(muffleText, 0, 10, 6, 1)
        root.add(muffleSlider, 0, 11, 6, 1)


        // RADIUS SLIDER
        val radiusSlider = WSlider(1, 16, Axis.HORIZONTAL)
        radiusSlider.value = blockEntity.data.radius
        val radiusText = WText(LiteralText("Radius: ${blockEntity.data.radius}"))
        radiusSlider.setValueChangeListener {
            blockEntity.data.radius = it
            radiusText.text = LiteralText("Radius: ${blockEntity.data.radius}")
        }
        radiusSlider.setDraggingFinishedListener {
            blockEntity.data.radius = it
            sendUpdatePacket()
        }
        root.add(radiusText, 8, 10, 6, 1)
        root.add(radiusSlider, 8, 11, 6, 1)


        root.validate(this)
    }
}

class ListItem(onToggle: BiConsumer<ListItem, Boolean>) : WPlainPanel() {
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