package fathertoast.specialai.config.field;

import fathertoast.crust.api.config.common.ConfigUtil;
import fathertoast.crust.api.config.common.field.PredicateStringListField;
import fathertoast.crust.api.config.common.file.TomlHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

public class ProfessionNameListField extends PredicateStringListField {

    final Map<ResourceLocation, String[]> componentsForProfession = new HashMap<>();


    public ProfessionNameListField( String key, List<String> defaultValue, @Nullable String... description ) {
        super( key, "Profession and Components", defaultValue, (string) -> {
            String[] components = string.split(" ");
            return components.length > 1 && ResourceLocation.tryParse( components[0] ) != null;
        }, description );
    }

    /** Adds info about the field type, format, and bounds to the end of a field's description. */
    @Override
    public void appendFieldInfo( List<String> comment ) {
        comment.add( TomlHelper.fieldInfoFormat( type + " List", valueDefault,
                "[ \"" + ConfigUtil.toLowerCaseNoSpaces( type ) + "1\", \"" +
                        ConfigUtil.toLowerCaseNoSpaces( type ) + "2\", ... ]" ) );
    }

    @Override
    public void load( @Nullable Object raw ) {
        super.load( raw );
        componentsForProfession.clear();

        for ( String s : value ) {
            String[] parts = s.split( " " );
            ResourceLocation professionId = ResourceLocation.tryParse( parts[0] );

            // Shouldn't happen since we validate earlier, but who knows
            if ( professionId == null ) continue;

            // Remove profession ID from the components
            List<String> components = new ArrayList<>( Arrays.asList( parts ).subList( 1, parts.length ) );
            componentsForProfession.put( professionId, components.toArray( new String[0] ) );
        }
    }

    /**
     * @return A random String name component for the given villager profession.
     *         Returns null if this field's value contains no name components
     *         or the given villager profession does not exist in the Forge registry.
     */
    @Nullable
    public String getRandomComponent( VillagerProfession profession, RandomSource random ) {
        ResourceLocation professionId = ForgeRegistries.VILLAGER_PROFESSIONS.getKey( profession );

        if ( professionId == null ) {
            return null;
        }

        if ( componentsForProfession.containsKey( professionId ) ) {
            String[] components = componentsForProfession.get( professionId );
            if ( components.length == 1 )
                return components[0];

            return components[ random.nextInt( components.length ) ];
        }
        return null;
    }

    /**
     * @return True if the components-per-profession map's keys
     *         contains the ID of the given villager profession.
     *         Returns false otherwise.
     */
    public boolean containsProfession( VillagerProfession profession ) {
         ResourceLocation professionId = ForgeRegistries.VILLAGER_PROFESSIONS.getKey( profession );

         if ( professionId == null ) return false;

         return componentsForProfession.containsKey( professionId );
    }

    /**
     * @return True if the components-per-profession map holds no name components
     *         associated with the given villager profession or if the profession
     *         is unregistered.
     */
    public boolean isEmpty( VillagerProfession profession ) {
        ResourceLocation professionId = ForgeRegistries.VILLAGER_PROFESSIONS.getKey( profession );

        if ( professionId == null ) return true;
        if ( !componentsForProfession.containsKey( professionId ) ) return true;
        return componentsForProfession.get( professionId ).length == 0;
    }
}
