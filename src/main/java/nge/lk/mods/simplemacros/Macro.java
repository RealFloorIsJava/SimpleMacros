package nge.lk.mods.simplemacros;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents a macro.
 */
@Getter
@Setter
@EqualsAndHashCode
public class Macro {

    /**
     * The group this macro is in.
     */
    private String group;

    /**
     * Whether this macro is only triggered when pressing shift.
     */
    private boolean shiftModifier;

    /**
     * Whether this macro is only triggered when pressing ctrl.
     */
    private boolean ctrlModifier;

    /**
     * The key this macro is bound to.
     */
    private int macroKey;

    /**
     * The commands in this macro.
     */
    private List<String> commands;

    /**
     * The delay in milliseconds between each command.
     */
    private int delay;

    /**
     * Whether this macro is currently active.
     */
    private transient boolean active;

    /**
     * Constructor.
     */
    public Macro(final String group, final boolean shiftModifier, final boolean ctrlModifier, final int macroKey,
                 final int delay, final List<String> commands) {
        this.group = group;
        this.shiftModifier = shiftModifier;
        this.ctrlModifier = ctrlModifier;
        this.macroKey = macroKey;
        this.delay = delay;
        this.commands = commands;
        if (commands.isEmpty()) {
            commands.add("");
        }
    }

    /**
     * Whether the macro should be saved.
     *
     * @return Whether the macro should be saved.
     */
    public boolean shouldSave() {
        boolean save = false;
        for (final String cmd : commands) {
            if (!cmd.trim().isEmpty()) {
                save = true;
            }
        }
        return save;
    }

    /**
     * Returns the export part of this tab.
     *
     * @return The export string.
     */
    public String getExport() {
        return group + "§" + Boolean.toString(shiftModifier) + "§" + Boolean.toString(ctrlModifier) + "§" + macroKey
                + "§" + delay + "§" + String.join("\t", commands);
    }
}
