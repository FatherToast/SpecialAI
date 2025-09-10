package fathertoast.specialai.config.field;

import fathertoast.crust.api.config.common.ConfigUtil;
import fathertoast.crust.api.config.common.field.IntField;
import fathertoast.crust.api.config.common.field.PredicateStringListField;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple implementation of an integer list with
 * minimum and maximum value bounds.
 */
public class IntListField extends PredicateStringListField {

    /**
     * Contains the String list of this field converted to integers.
     * Gets populated when the config is loaded.
     */
    private final List<Integer> intList;


    public IntListField( String key, List<String> defaultValue,
                        IntField.Range valueRange, @Nullable String... description ) {
        super( key, "Integer", defaultValue, (line) -> isValidNumber( key, line, valueRange ), description );
        this.intList = new ArrayList<>();
    }

    /**
     * @return True if the given line can be parsed as an integer
     *         and is within the given bounds.
     *
     * @param key The key of the config field calling this.
     * @param line The line to check.
     * @param valueRange The minimum and maximum value bounds.
     */
    private static boolean isValidNumber( String key, String line, IntField.Range valueRange ) {
        try {
            int integer = Integer.parseInt(line);
            return integer >= valueRange.MIN && integer <= valueRange.MAX;
        }
        catch ( NumberFormatException e ) {
            ConfigUtil.LOG.warn( "Value for {} \"{}\" is invalid! Ignoring value. Invalid value: {}",
                    IntListField.class, key, line );
        }
        return false;
    }

    @Override
    public void load( @Nullable Object raw ) {
        super.load( raw );
        intList.clear();

        for( String value : value ) {
            intList.add( Integer.valueOf( value ) );
        }
    }

    /** @return The integer values of this field's String list. */
    public Iterable<Integer> intValues() {
        return intList;
    }
}
