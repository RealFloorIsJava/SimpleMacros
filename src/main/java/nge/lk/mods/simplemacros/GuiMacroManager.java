package nge.lk.mods.simplemacros;

import nge.lk.mods.commonlib.gui.factory.GuiFactory;
import nge.lk.mods.commonlib.gui.factory.Positioning;
import nge.lk.mods.commonlib.gui.factory.element.ButtonElement;
import nge.lk.mods.commonlib.gui.factory.element.InputElement;
import nge.lk.mods.commonlib.gui.factory.element.SliderElement;
import nge.lk.mods.commonlib.gui.factory.element.TextElement;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * An editor for creating or changing tabs.
 */
public class GuiMacroManager extends GuiFactory implements Consumer<ButtonElement>, BiConsumer<SliderElement, Float>,
        BiFunction<SliderElement, Float, String> {

    /**
     * The macro IO manager.
     */
    private final MacroIO macroIO;

    /**
     * The loaded macros.
     */
    private final List<Macro> macros;

    /**
     * The button to save the tab and return to the parent.
     */
    private ButtonElement doneButton;

    /**
     * The pointer text.
     */
    private TextElement pointerText;

    /**
     * The text field for the command.
     */
    private InputElement commandInput;

    /**
     * The button which shows the previous macro.
     */
    private ButtonElement prevMacroButton;

    /**
     * The button which shows the next macro.
     */
    private ButtonElement nextMacroButton;

    /**
     * The button which shows the previous command.
     */
    private ButtonElement prevCommandButton;

    /**
     * The button which shows the next command.
     */
    private ButtonElement nextCommandButton;

    /**
     * The slider for the delay.
     */
    private SliderElement delaySlider;

    /**
     * The button for recording a key binding.
     */
    private ButtonElement recordButton;

    /**
     * The text field for the macro group.
     */
    private InputElement groupInput;

    /**
     * A pointer to the macro that's currently being edited.
     */
    private int macroPointer;

    /**
     * A pointer to the command that's currently being edited.
     */
    private int commandPointer;

    /**
     * Whether a key binding is currently being recorded.
     */
    private boolean recording;

    /**
     * Constructor.
     *
     * @param macroIO The macro IO manager.
     * @param macros The loaded macros.
     */
    public GuiMacroManager(final MacroIO macroIO, final List<Macro> macros) {
        this.macroIO = macroIO;
        this.macros = macros;
        createGui();
        loadMacro();
    }

    @Override
    public void accept(final ButtonElement buttonElement) {
        if (buttonElement == doneButton) {
            saveChanges();
            macroIO.saveState(macros);
            closeGui();
        } else if (buttonElement == recordButton) {
            recording = true;
            updateCaptions();
        } else if (buttonElement == prevMacroButton) {
            saveChanges();
            if (macroPointer > 0) {
                macroPointer--;
                commandPointer = 0;
                loadMacro();
            }
        } else if (buttonElement == prevCommandButton) {
            saveChanges();
            if (commandPointer > 0) {
                commandPointer--;
                loadMacro();
            }
        } else if (buttonElement == nextMacroButton) {
            saveChanges();
            if (macroPointer == macros.size() - 1) {
                if (macros.get(macroPointer).shouldSave()) {
                    macros.add(new Macro("", false, false, Keyboard.KEY_ESCAPE, 1000,
                            new ArrayList<>()));
                    macroPointer++;
                    commandPointer = 0;
                    loadMacro();
                }
            } else {
                macroPointer++;
                commandPointer = 0;
                loadMacro();
            }
        } else if (buttonElement == nextCommandButton) {
            saveChanges();
            final Macro macro = macros.get(macroPointer);
            if (commandPointer == macro.getCommands().size() - 1) {
                if (!macro.getCommands().get(commandPointer).isEmpty()) {
                    macro.getCommands().add("");
                    commandPointer++;
                    loadMacro();
                }
            } else {
                commandPointer++;
                loadMacro();
            }
        }
    }

    @Override
    public void accept(final SliderElement sliderElement, final Float val) {
        macros.get(macroPointer).setDelay(val.intValue() * 50);
    }

    @Override
    public String apply(final SliderElement sliderElement, final Float val) {
        return Integer.toString(val.intValue() * 50) + "ms Delay";
    }

    @Override
    protected void keyTyped(final char typedChar, final int keyCode) throws IOException {
        if (recording) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                recording = false;
                macros.get(macroPointer).setMacroKey(Keyboard.KEY_ESCAPE);
                updateCaptions();
            } else if (keyCode != Keyboard.KEY_LSHIFT && keyCode != Keyboard.KEY_RSHIFT
                    && keyCode != Keyboard.KEY_RCONTROL && keyCode != Keyboard.KEY_LCONTROL) {
                recording = false;
                final Macro macro = macros.get(macroPointer);
                macro.setMacroKey(keyCode);
                macro.setShiftModifier(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                        || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
                macro.setCtrlModifier(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                        || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL));
                updateCaptions();
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void createGui() {
        setPadding(0.05, 0.05, 0.1, 0.05);

        addText(new Positioning().center()).setText("Macro Manager", 0xA0A0A0);
        addBlank(new Positioning().breakRow().absoluteHeight(15));

        pointerText = addText(new Positioning().breakRow());
        commandInput = addInput(new Positioning().relativeWidth(90).absoluteHeight(20).breakRow());
        commandInput.getTextField().setMaxStringLength(100);

        prevMacroButton = addButton(this, new Positioning().relativeWidth(20).absoluteHeight(20));
        prevMacroButton.getButton().displayString = "Prev Macro";
        nextMacroButton = addButton(this, new Positioning().relativeWidth(20).absoluteHeight(20));
        nextMacroButton.getButton().displayString = "Next Macro";
        nextCommandButton = addButton(this,
                new Positioning().alignRight().relativeWidth(20).absoluteHeight(20));
        nextCommandButton.getButton().displayString = "Next Command";
        prevCommandButton = addButton(this,
                new Positioning().alignRight().relativeWidth(20).absoluteHeight(20).breakRow());
        prevCommandButton.getButton().displayString = "Prev Command";
        addBlank(new Positioning().absoluteHeight(10).breakRow());

        addText(new Positioning().breakRow()).setText("Delay Between Commands", 0xA0A0A0);
        delaySlider = addSlider(1, 50, 20, this, this,
                new Positioning().relativeWidth(75).absoluteHeight(20).breakRow());
        addBlank(new Positioning().absoluteHeight(10).breakRow());

        addText(new Positioning().breakRow()).setText("Key Binding & Macro Group", 0xA0A0A0);
        recordButton = addButton(this, new Positioning().relativeWidth(35).absoluteHeight(20));
        groupInput = addInput(new Positioning().breakRow().relativeWidth(35).absoluteHeight(20));
        groupInput.getTextField().setMaxStringLength(16);

        addText(new Positioning().breakRow()).setText(
                "By setting a macro group you can enable and disable multiple macros at the", 0xA0A0A0);
        addText(new Positioning().breakRow()).setText(
                "same time. If you leave the text field empty the macro will be always enabled.",
                0xA0A0A0);

        doneButton = addButton(this,
                new Positioning().alignBottom().center().relativeWidth(30).absoluteHeight(20));
        doneButton.getButton().displayString = "Done";

        updateCaptions();
    }

    /**
     * Saves all changes in the GUI to the macro.
     */
    private void saveChanges() {
        final Macro macro = macros.get(macroPointer);
        macro.getCommands().set(commandPointer, commandInput.getTextField().getText());
        macro.setGroup(groupInput.getTextField().getText());
    }

    /**
     * Sets all GUI elements to the correct values for the current macro/command.
     */
    private void loadMacro() {
        final Macro macro = macros.get(macroPointer);
        final String command = macro.getCommands().get(commandPointer);
        commandInput.getTextField().setText(command);
        commandInput.getTextField().setCursorPosition(0);
        delaySlider.getSlider().func_175218_a(macro.getDelay() / 50, false);
        groupInput.getTextField().setText(macro.getGroup());
        updateCaptions();
    }

    /**
     * Updates the captions of all GUI elements.
     */
    private void updateCaptions() {
        final int nMacros = macros.size();
        final Macro macro = macros.get(macroPointer);
        final int nCommands = macro.getCommands().size();
        pointerText.setText("Macro " + (macroPointer + 1) + " of " + nMacros + "  »  Command "
                + (commandPointer + 1) + " of " + nCommands, 0xA0A0A0);

        if (recording) {
            recordButton.getButton().displayString = "Recording...";
        } else if (macro.getMacroKey() == Keyboard.KEY_ESCAPE) {
            recordButton.getButton().displayString = "Record Binding";
        } else {
            String binding = macro.isCtrlModifier() ? "Ctrl-" : "";
            binding += macro.isShiftModifier() ? "Shift-" : "";
            binding += Keyboard.getKeyName(macro.getMacroKey());
            recordButton.getButton().displayString = binding;
        }
    }
}
