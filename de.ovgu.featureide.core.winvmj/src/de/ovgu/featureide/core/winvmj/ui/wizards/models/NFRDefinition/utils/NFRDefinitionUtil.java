package de.ovgu.featureide.core.winvmj.ui.wizards.models.NFRDefinition.utils;

import java.util.Set;
import de.ovgu.featureide.fm.core.base.IFeature;

public class NFRDefinitionUtil {
    private NFRDefinitionUtil() {}

    public static boolean isNumber(String input) {
        try {
            Integer.parseInt(input);
            return true;
        }

        catch (Exception e) {
            return false;
        }
    }

    public static String getNFRPrefix(Set<IFeature> selected) {
        return selected.stream()
            .map(IFeature::getName)                 
            .filter(name -> name.contains("NFR")) 
            .findFirst()                       
            .map(name -> {
                int dotIndex = name.indexOf('.');
                return (dotIndex > 0) ? name.substring(0, dotIndex + 1) : "";
            })
            .orElse("");
    }

    public static String formatDotSuffix(String input) {
        return input.contains(".") ? input.split("\\.")[0] : input;
    }

    public static String formatDotPrefix(String input) {
        return input.contains(".")  ? input.split("\\.")[1] : input;
    }
}
