package net.minecraft.client.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.network.play.client.C0BPacketEntityAction;

@SideOnly(Side.CLIENT)
public class GuiSleepMP extends GuiChat
{
    //private static final String __OBFID = "CL_00000697";

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    public void initGui()
    {
        super.initGui();
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height - 40, I18n.format("multiplayer.stopSleeping", new Object[0])));
    }

    /**
     * Fired when a key is typed. This is the equivalent of KeyListener.keyTyped(KeyEvent e).
     */
    public void keyTyped(char character, int key)
    {
        if (key == 1)
        {
            this.func_146418_g();
        }
        else if (key != 28 && key != 156)
        {
            super.keyTyped(character, key);
        }
        else
        {
            String s = this.inputField.getText().trim();

            if (!s.isEmpty())
            {
                this.func_146403_a(s); // Forge: fix vanilla not adding messages to the sent list while sleeping
            }

            this.inputField.setText("");
            this.mc.ingameGUI.getChatGUI().resetScroll();
        }
    }

    protected void actionPerformed(GuiButton guiButton)
    {
        if (guiButton.id == 1)
        {
            this.func_146418_g();
        }
        else
        {
            super.actionPerformed(guiButton);
        }
    }

    private void func_146418_g()
    {
        NetHandlerPlayClient nethandlerplayclient = this.mc.thePlayer.sendQueue;
        nethandlerplayclient.addToSendQueue(new C0BPacketEntityAction(this.mc.thePlayer, 3));
    }
}