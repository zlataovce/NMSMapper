package org.screamingsandals.nms.mapper.utils;

import j2html.tags.DomContent;
import org.screamingsandals.nms.mapper.single.MappingType;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

public class MiscUtils {
    public static String getModifierString(int modifier) {
        final List<String> modifiers = Arrays.stream(Modifier.toString(modifier).split(" ")).collect(Collectors.toList());
        if (modifiers.contains("interface")) {
            modifiers.remove("abstract");
        }
        return String.join(" ", modifiers);
    }

    public static DomContent descriptions(MappingType defaultMapping) {
        if (defaultMapping != MappingType.MOJANG) {
            return div(
                    "This minecraft version doesn't have published official Mojang mappings. Other mappings are used as default instead: " + defaultMapping
            ).withClass("alert alert-danger");
        }

        return null;
    }

    public static String capitalizeFirst(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    public static DomContent mappingToBadge(MappingType mappingType) {
        return span(capitalizeFirst(mappingType.name())).withClass("badge me-2 bg-" + chooseBootstrapColor(mappingType));
    }

    public static String chooseBootstrapColor(MappingType mappingType) {
        switch (mappingType) {
            case MOJANG:
                return "success";
            case SPIGOT:
                return "warning";
            case SEARGE:
                return "danger";
            case OBFUSCATED:
                return "primary";
            default:
                return "secondary";
        }
    }
}
