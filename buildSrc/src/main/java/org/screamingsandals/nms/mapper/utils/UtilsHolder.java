package org.screamingsandals.nms.mapper.utils;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Data;
import org.screamingsandals.nms.mapper.joined.JoinedClassDefinition;
import org.screamingsandals.nms.mapper.single.ClassDefinition;
import org.screamingsandals.nms.mapper.single.MappingType;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Data
public class UtilsHolder {
    private final Object2ObjectOpenHashMap<String, Object2ObjectOpenHashMap<String, ClassDefinition>> mappings = new Object2ObjectOpenHashMap<>();
    private final File resourceDir;
    private final Object2ObjectOpenHashMap<String, MappingType> newlyGeneratedMappings = new Object2ObjectOpenHashMap<>();
    private final AtomicReference<ConfigurationNode> versionManifest = new AtomicReference<>();
    private final Object2ObjectOpenHashMap<String, String> joinedMappingsClassLinks = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, JoinedClassDefinition> joinedMappings = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, String> spigotJoinedMappingsClassLinks = new Object2ObjectOpenHashMap<>();
    private final ObjectList<String> undefinedClassLinks = new ObjectArrayList<>();
    private final Object2ObjectOpenHashMap<Map.Entry<String, MappingType>, String> licenses = new Object2ObjectOpenHashMap<>();
}
