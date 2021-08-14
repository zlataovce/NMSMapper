package org.screamingsandals.nms.mapper.parser;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.screamingsandals.nms.mapper.single.ClassDefinition;
import org.screamingsandals.nms.mapper.single.MappingType;
import org.screamingsandals.nms.mapper.utils.ErrorsLogger;

import java.io.*;

public class MojangMappingParser {
    public static String map(Object2ObjectOpenHashMap<String, ClassDefinition> map, File file, ObjectList<String> excluded, ErrorsLogger errorsLogger) throws IOException {
        AnyMappingParser.map(map, new FileInputStream(file), excluded, MappingType.MOJANG, true, errorsLogger);

        try (var br = new BufferedReader(new FileReader(file))) {
            return br.readLine();
        } catch (Throwable ignored) {}

        return null;
    }
}
