package com.projecturanus.betterp2p.client.gui

import appeng.client.gui.widgets.MEGuiTextField
import com.projecturanus.betterp2p.BetterP2P
import com.projecturanus.betterp2p.MODID
import com.projecturanus.betterp2p.capability.MemoryInfo
import com.projecturanus.betterp2p.capability.P2PTunnelInfo
import com.projecturanus.betterp2p.client.ClientCache
import com.projecturanus.betterp2p.client.TextureBound
import com.projecturanus.betterp2p.client.gui.widget.IGuiTextField
import com.projecturanus.betterp2p.client.gui.widget.WidgetP2PDevice
import com.projecturanus.betterp2p.client.gui.widget.WidgetScrollBar
import com.projecturanus.betterp2p.item.BetterMemoryCardModes
import com.projecturanus.betterp2p.network.*
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.resources.I18n
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.GL_QUADS
import java.util.*

class GuiAdvancedMemoryCard(msg: S2CListP2P) : GuiScreen(), TextureBound {
    private val xSize = 288
    private val ySize = 242
    private val guiLeft: Int by lazy { (width - this.xSize) / 2 }
    private val guiTop: Int by lazy { (height - this.ySize) / 2 }

    private val tableX = 9
    private val tableY = 19

    private lateinit var scrollBar: WidgetScrollBar
    private lateinit var searchBar: MEGuiTextField
    private lateinit var renameBar: IGuiTextField

    private val infos = InfoList(msg.infos.map(::InfoWrapper), ::searchText)

    private val searchText: String
        get() = searchBar.text

    private val widgetDevices: List<WidgetP2PDevice>
//    private var infoOnScreen: List<InfoWrapper>

    private val descriptionLines: MutableList<String> = mutableListOf()

    private var mode = msg.memoryInfo.mode
    private var modeString = getModeString()
    private val modeButton by lazy { GuiButton(0, guiLeft + 8, guiTop + 190, 256, 20, modeString) }
    private val sortRules: List<String> by lazy {
        listOf(
            "§b§n" + I18n.format("gui.advanced_memory_card.sortinfo1"),
            "§9@in§7 - " + I18n.format("gui.advanced_memory_card.sortinfo2"),
            "§6@out§7 - " + I18n.format("gui.advanced_memory_card.sortinfo3"),
            "§a@b§7 - " + I18n.format("gui.advanced_memory_card.sortinfo4"),
            "§c@u§7 - " + I18n.format("gui.advanced_memory_card.sortinfo5"),
            "§7" + I18n.format("gui.advanced_memory_card.sortinfo6")
        )
    }

    private val selectedInfo: InfoWrapper?
        get() = infos.selectedInfo

    init {
        infos.select(msg.memoryInfo.selectedEntry)
        val list = mutableListOf<WidgetP2PDevice>()
        for (i in 0..3) {
            list += WidgetP2PDevice(::selectedInfo, ::mode, { infos.filtered.getOrNull(i + scrollBar.currentScroll) }, 0, 0)
        }
        widgetDevices = list.toList()
    }

    override fun initGui() {
        super.initGui()
        checkInfo()
        renameBar = IGuiTextField(fontRenderer, 0, 0)
        renameBar.maxStringLength = 50

        searchBar = MEGuiTextField(fontRenderer, guiLeft + 198, guiTop + 5, 65, 10)
        searchBar.maxStringLength = 25
        searchBar.setTextColor(0xFFFFFF)
        searchBar.visible = true
        searchBar.enableBackgroundDrawing = false

        scrollBar = WidgetScrollBar()
        scrollBar.displayX = guiLeft + 268
        scrollBar.displayY = guiTop + 19
        scrollBar.height = 150
        scrollBar.setRange(0, infos.size.coerceIn(0..(infos.size - 4).coerceAtLeast(0)), 23)

        for (i in 0..3) {
            widgetDevices[i].x = guiLeft + tableX
            widgetDevices[i].y = guiTop + tableY + 42 * i
        }
        searchBar.text = ClientCache.searchText
        infos.refresh()
        checkInfo()
        refreshOverlay()
    }

    private fun checkInfo() {
        infos.filtered.forEach { it.error = false }
        infos.filtered.groupBy { it.frequency }.filter { it.value.none { x -> !x.output } }.forEach { it.value.forEach { info ->
            info.error = true
        } }
    }

    fun refreshInfo(infos: List<P2PInfo>) {
        this.infos.rebuild(infos.map(::InfoWrapper))
        checkInfo()
        refreshOverlay()
    }

    private fun syncMemoryInfo() {
        ModNetwork.channel.sendToServer(C2SUpdateInfo(MemoryInfo(infos.selectedEntry, selectedInfo?.frequency ?: 0, mode)))
    }

    private fun drawInformation() {
        val x = 8
        var y = 214
        for (line in descriptionLines) {
            fontRenderer.drawString(line, guiLeft + x, guiTop + y, 0)
            y += fontRenderer.FONT_HEIGHT
        }
    }

    override fun onGuiClosed() {
        ClientCache.searchText = searchBar.text
        saveP2PChannel()
        syncMemoryInfo()
        ModNetwork.channel.sendToServer(C2SCloseGui())
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        drawBackground()
        scrollBar.draw(this)
        searchBar.drawTextBox()

        for (widget in widgetDevices) {
            widget.render(this, mouseX, mouseY, partialTicks)
        }
        modeButton.drawButton(mc, mouseX, mouseY, partialTicks)

        if (modeButton.isMouseOver) {
            descriptionLines.clear()
            descriptionLines += I18n.format("gui.advanced_memory_card.desc.mode", I18n.format("gui.advanced_memory_card.mode.${mode.next().name.toLowerCase(Locale.getDefault())}"))
        } else {
            descriptionLines.clear()
        }
        drawInformation()

        if (renameBar.visible && !renameBar.isFocused) {
            renameBar.isFocused = true
        }
        if (searchBar.isMouseIn(mouseX, mouseY)) {
            drawHoveringText(sortRules, guiLeft, guiTop + ySize - 40, fontRenderer)
        }

        renameBar.drawTextBox()

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun switchMode() {
        // Switch mode
        mode = mode.next()
        modeString = getModeString()
        modeButton.displayString = modeString

        syncMemoryInfo()
    }

    private fun getModeString(): String {
        return I18n.format("gui.advanced_memory_card.mode.${mode.name.toLowerCase(Locale.getDefault())}")
    }

    private fun findInput(frequency: Short?) =
        infos.filtered.find { it.frequency == frequency && !it.output }

    private fun selectInfo(hash: Long) {
        infos.select(hash)
        syncMemoryInfo()
        refreshOverlay()
    }

    private fun refreshOverlay() {
        if (selectedInfo == null) {
            ClientCache.selectedPosition = null
            ClientCache.selectedFacing = null
        } else {
            ClientCache.selectedPosition = selectedInfo?.pos
            ClientCache.selectedFacing = selectedInfo?.facing
        }
        ClientCache.positions.clear()
        ClientCache.positions.addAll(infos.sorted.filter { it.frequency == selectedInfo?.frequency && it != selectedInfo }.map { it.pos to it.facing })
    }

    private fun onSelectButtonClicked(info: InfoWrapper) {
        selectInfo(info.code)
    }

    private fun onRenameButtonClicked(info: InfoWrapper, index: Int) {
        /*
        if (isShiftKeyDown()) {
            return transportPlayer(info)
        }
         */

        renameBar.visible = true
        renameBar.y = (this.guiTop + this.tableY) + index * 42 + 19
        renameBar.x = this.guiLeft + 60
        renameBar.width = 120
        renameBar.height = 12
        renameBar.text = info.name
        renameBar.isFocused = true
        renameBar.cursorPosition = 0
        renameBar.info = info
    }

    private fun saveP2PChannel() {
        for (widget in widgetDevices) {
            widget.renderNameTextfield = true
        }

        if (renameBar.info != null && renameBar.info.name != renameBar.text) {
            val info: InfoWrapper = renameBar.info
            renameBar.text = renameBar.text.trim()
            ModNetwork.channel.sendToServer(C2SP2PTunnelInfo(P2PTunnelInfo(info.pos, info.dim, info.facing, renameBar.text)))
        }

        renameBar.visible = false
        renameBar.text = ""
        renameBar.isFocused = false
        renameBar.info = null
    }

    private fun onBindButtonClicked(info: InfoWrapper) {
        if (infos.selectedEntry == NONE) return
        when (mode) {
            BetterMemoryCardModes.INPUT -> {
                BetterP2P.logger.debug("Bind ${info.code} as input")
                ModNetwork.channel.sendToServer(C2SLinkP2P(info.code, infos.selectedEntry))
            }
            BetterMemoryCardModes.OUTPUT -> {
                BetterP2P.logger.debug("Bind ${info.code} as output")
                ModNetwork.channel.sendToServer(C2SLinkP2P(infos.selectedEntry, info.code))
            }
            BetterMemoryCardModes.COPY -> {
                val input = findInput(selectedInfo?.frequency)
                if (input != null)
                    ModNetwork.channel.sendToServer(C2SLinkP2P(input.code, info.code))
            }
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        var clickRenameButton = false
        for ((index, widget) in widgetDevices.withIndex()) {
            val info = widget.infoSupplier()
            if (info?.selectButton?.mousePressed(mc, mouseX, mouseY) == true)
                onSelectButtonClicked(widget.infoSupplier()!!)
            else if (info?.bindButton?.mousePressed(mc, mouseX, mouseY) == true)
                onBindButtonClicked(widget.infoSupplier()!!)
            else if (info?.renameButton?.mousePressed(mc, mouseX, mouseY) == true) {
                if (renameBar.info != widget.infoSupplier()!!) {
                    saveP2PChannel()
                }
                widget.renderNameTextfield = false
                onRenameButtonClicked(widget.infoSupplier()!!, index)
                clickRenameButton = true
            }
        }
        if(!clickRenameButton && renameBar.visible) {
            saveP2PChannel()
        }
        if (modeButton.mousePressed(mc, mouseX, mouseY)) {
            switchMode()
        }
        scrollBar.click(mouseX, mouseY)
        searchBar.mouseClicked(mouseX, mouseY, mouseButton)
        renameBar.mouseClicked(mouseX, mouseY, mouseButton)
        if (mouseButton == 1 && searchBar.isMouseIn(mouseX, mouseY)) {
            this.searchBar.text = ""
            infos.refilter()
        }
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        scrollBar.click(mouseX, mouseY)
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        val i = Mouse.getEventDWheel()
        if (i != 0 && isShiftKeyDown()) {
            val x = Mouse.getEventX() * width / mc.displayWidth
            val y = height - Mouse.getEventY() * height / mc.displayHeight - 1
//            this.mouseWheelEvent(x, y, i / Math.abs(i))
        } else if (i != 0) {
            scrollBar.wheel(i)
            saveP2PChannel()
        }
    }

    override fun bindTexture(modid: String, location: String) {
        val loc = ResourceLocation(modid, location)
        mc.textureManager.bindTexture(loc)
    }

    private fun drawBackground() {
        bindTexture(MODID, "textures/gui/advanced_memory_card.png")
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, this.xSize, this.ySize)

        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer;
        val f = 1f / this.xSize.toFloat()
        val f1 = 1f / this.ySize.toFloat()
        val u = 0
        val v = 0

        bufferBuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        bufferBuilder.pos(guiLeft.toDouble(), (guiTop + this.ySize).toDouble(), 0.0).tex((u.toFloat() * f).toDouble(), ((v + this.ySize).toFloat() * f1).toDouble()).endVertex()
        bufferBuilder.pos((guiLeft + this.xSize).toDouble(), (guiTop + this.ySize).toDouble(), 0.0).tex(((u + xSize).toFloat() * f).toDouble(), ((v + this.ySize).toFloat() * f1).toDouble()).endVertex()
        bufferBuilder.pos((guiLeft + this.xSize).toDouble(), guiTop.toDouble(), 0.0).tex(((u + xSize).toFloat() * f).toDouble(), (v.toFloat() * f1).toDouble()).endVertex()
        bufferBuilder.pos(guiLeft.toDouble(), guiTop.toDouble(), 0.0).tex((u.toFloat() * f).toDouble(), (v.toFloat() * f1).toDouble()).endVertex()
        tessellator.draw()
    }

    override fun doesGuiPauseGame(): Boolean {
        return false
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_LSHIFT) {
            return
        }
        if (renameBar.isFocused) {
            if (keyCode == Keyboard.KEY_RETURN) {
                saveP2PChannel()
            } else {
                renameBar.textboxKeyTyped(typedChar, keyCode)
            }
        } else if (!(typedChar.isWhitespace() && searchBar.text.isEmpty() && searchBar.textboxKeyTyped(typedChar, keyCode))) {
            searchBar.textboxKeyTyped(typedChar, keyCode)
            infos.refilter()
        }
        return super.keyTyped(typedChar, keyCode)
    }
}
