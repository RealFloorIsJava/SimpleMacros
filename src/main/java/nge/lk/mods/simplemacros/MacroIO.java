package nge.lk.mods.simplemacros;

import lombok.RequiredArgsConstructor;
import nge.lk.mods.commonlib.util.DebugUtil;
import nge.lk.mods.commonlib.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Handles IO of macros.
 */
@RequiredArgsConstructor
public class MacroIO {

    /**
     * The file where the macro configurations are stored in.
     */
    private final File saveFile;

    /**
     * Returns an iterator iterating over export strings of macros.
     *
     * @param macros The macros which should be iterated over.
     *
     * @return An iterator over all export strings.
     */
    private static Iterator<String> getExportIterator(final Iterable<Macro> macros) {
        final Collection<String> mapped = new LinkedList<>();
        for (final Macro macro : macros) {
            if (macro.shouldSave()) {
                mapped.add(macro.getExport());
            }
        }
        return mapped.iterator();
    }

    /**
     * Saves the macro configurations to the save file.
     *
     * @param macros The macros to be saved.
     */
    public void saveState(final Iterable<Macro> macros) {
        try {
            FileUtil.writeLineStorage(1, saveFile, getExportIterator(macros));
        } catch (final IOException e) {
            DebugUtil.recoverableError(e);
        }
    }

    /**
     * Loads the macro configurations from the save file.
     */
    public List<Macro> loadState() {
        final List<Macro> results = new ArrayList<>();
        try {
            FileUtil.readLineStorage(saveFile, new MacroBuilder(results), new MacroVersionConverter());
        } catch (final IOException e) {
            DebugUtil.recoverableError(e);
        }
        return results;
    }

    /**
     * Converts macro data between different versions.
     */
    private static class MacroVersionConverter implements BiFunction<Integer, String, String> {

        @Override
        public String apply(final Integer version, final String line) {
            // int newVersion = version;
            // String newLine = line;

            // if (newVersion == 1) { // Converter: v1 -> v2
            //     // Change: ...
            //     newVersion++;
            // }

            // return newLine;
            return line;
        }
    }

    /**
     * Builds macros from line data.
     */
    @RequiredArgsConstructor
    private static class MacroBuilder implements BiConsumer<String, Integer> {

        private final List<Macro> results;

        @Override
        public void accept(final String line, final Integer lineNo) {
            // Avoid trimming of the array by adding a high limit.
            final String[] split = line.split("§", 99);

            // Create the macro.
            final String group = split[0];
            final boolean shiftModifier = Boolean.parseBoolean(split[1]);
            final boolean ctrlModifier = Boolean.parseBoolean(split[2]);
            final int macroKey = Integer.parseInt(split[3]);
            final int delay = Integer.parseInt(split[4]);
            final List<String> commands = new ArrayList<>(Arrays.asList(split[5].split("\t")));
            results.add(new Macro(group, shiftModifier, ctrlModifier, macroKey, delay, commands));
        }
    }
}
