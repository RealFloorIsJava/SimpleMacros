package nge.lk.mods.simplemacros;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import nge.lk.mods.commonlib.util.DebugUtil;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import static nge.lk.mods.simplemacros.SimpleMacrosMod.MODID;
import static nge.lk.mods.simplemacros.SimpleMacrosMod.VERSION;

/**
 * Main mod class.
 */
@Mod(modid = MODID, version = VERSION, clientSideOnly = true)
public class SimpleMacrosMod {

    /**
     * The ID of the mod.
     */
    public static final String MODID = "simplemacros";

    /**
     * The version of the mod.
     */
    public static final String VERSION = "@VERSION@";

    /**
     * All currently scheduled tick runnables.
     */
    private final Queue<TickRunnable> tickRunnables = new PriorityQueue<>();

    /**
     * The manager for macro IO.
     */
    private MacroIO macroIO;

    /**
     * The key binding for the editor.
     */
    private KeyBinding editorKey;

    /**
     * The key binding for changing the macro group.
     */
    private KeyBinding groupKey;

    /**
     * All loaded macros.
     */
    private List<Macro> macros;

    /**
     * The active macro group.
     */
    private String activeGroup = "";

    /**
     * Counts the ticks, for decreasing click speeds.
     */
    private long tickCounter;

    @EventHandler
    public void onPreInit(final FMLPreInitializationEvent event) {
        DebugUtil.initializeLogger(MODID);
        macroIO = new MacroIO(new File(event.getModConfigurationDirectory(), "macros.dat"));
    }

    @EventHandler
    public void onInit(final FMLInitializationEvent event) {
        macros = macroIO.loadState();
        if (macros.isEmpty()) {
            macros.add(new Macro("", false, false, Keyboard.KEY_ESCAPE, 1000,
                    new ArrayList<>()));
        }

        editorKey = new KeyBinding("Macro Manager", Keyboard.KEY_F7, "Simple Macros");
        ClientRegistry.registerKeyBinding(editorKey);
        groupKey = new KeyBinding("Change Macro Group", Keyboard.KEY_F8, "Simple Macros");
        ClientRegistry.registerKeyBinding(groupKey);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onKeyPress(final KeyInputEvent event) {
        if (Minecraft.getMinecraft().currentScreen != null) {
            return;
        }

        if (editorKey.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiMacroManager(macroIO, macros));
        }

        if (groupKey.isPressed()) {
            final Collection<String> groups = new ArrayList<>();
            final Collection<String> groupMarks = new HashSet<>();

            // Ensure the general group is first
            groups.add("");
            groupMarks.add("");

            for (final Macro macro : macros) {
                if (!groupMarks.contains(macro.getGroup()) && !macro.getGroup().isEmpty()) {
                    groupMarks.add(macro.getGroup().toLowerCase());
                    groups.add(macro.getGroup().toLowerCase());
                }
            }

            boolean found = false;
            String result = "";
            for (final String group : groups) {
                if (found) {
                    result = group;
                    break;
                }
                if (group.equals(activeGroup)) {
                    found = true;
                }
            }

            activeGroup = result;
            final String msg;
            if (activeGroup.isEmpty()) {
                msg = "Disabled all macros except global macros.";
            } else {
                msg = "Macro group '" + activeGroup + "' is now active.";
            }
            Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText(msg));
        }

        final boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        final boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        for (final Macro macro : macros) {
            if (!macro.getGroup().isEmpty() && !macro.getGroup().equalsIgnoreCase(activeGroup)) {
                continue;
            }
            if (!Keyboard.isKeyDown(macro.getMacroKey())) {
                macro.setActive(false);
            }
            if (macro.isCtrlModifier() != ctrl) {
                continue;
            }
            if (macro.isShiftModifier() != shift) {
                continue;
            }
            if (Keyboard.isKeyDown(macro.getMacroKey())) {
                if (!macro.isActive()) {
                    macro.setActive(true);
                    long target = tickCounter + 1;
                    for (final String command : macro.getCommands()) {
                        if (!command.isEmpty()) {
                            tickRunnables.add(new TickRunnable(target,
                                    () -> Minecraft.getMinecraft().thePlayer.sendChatMessage(command)));
                        }
                        target += macro.getDelay() / 50;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onTick(final ClientTickEvent event) {
        if (event.phase != ClientTickEvent.Phase.END) {
            return;
        }
        tickCounter++;
        while (!tickRunnables.isEmpty()) {
            if (tickRunnables.peek().getTick() == tickCounter) {
                tickRunnables.poll().getTask().run();
            } else {
                break;
            }
        }
    }

    /**
     * A runnable that runs in a certain tick.
     */
    @RequiredArgsConstructor
    private static class TickRunnable implements Comparable {

        @Getter private final long tick;

        @Getter private final Runnable task;

        @Override
        public int compareTo(final Object o) {
            assert o instanceof TickRunnable;
            final TickRunnable other = (TickRunnable) o;
            return Long.compare(tick, ((TickRunnable) o).tick);
        }
    }
}
