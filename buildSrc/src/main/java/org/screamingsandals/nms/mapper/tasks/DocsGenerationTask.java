package org.screamingsandals.nms.mapper.tasks;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.VersionNumber;
import org.screamingsandals.nms.mapper.single.ClassDefinition;
import org.screamingsandals.nms.mapper.utils.UtilsHolder;
import org.screamingsandals.nms.mapper.web.*;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class DocsGenerationTask extends DefaultTask {
    @Input
    public abstract Property<File> getOutputFolder();
    @Input
    public abstract Property<UtilsHolder> getUtils();

    @SneakyThrows
    @TaskAction
    public void run() {
        System.out.println("Generating web docs");
        var outputFolder = getOutputFolder().get();
        var versions = getUtils().get().getNewlyGeneratedMappings();
        var mappings = getUtils().get().getMappings();

        outputFolder.mkdirs();

        versions.object2ObjectEntrySet().fastForEach(versionEntry -> {
            var version = versionEntry.getKey();
            var defaultMapping = versionEntry.getValue();
            System.out.println("Generating docs for version " + version);

            var searchIndex = new ObjectArrayList<Map<String, String>>();

            var versionDirectory = new File(outputFolder, version);
            versionDirectory.mkdirs();

            var packages = new Object2ObjectOpenHashMap<String, ObjectList<Map.Entry<ClassDefinition.Type, String>>>();

            mappings.get(version).object2ObjectEntrySet().fastForEach(mappingEntry -> {
                var key = mappingEntry.getKey();
                var classDefinition = mappingEntry.getValue();
                var key2 = classDefinition.getMapping().getOrDefault(defaultMapping, key);

                var pathKey = key2
                        .replace(".", "/")
                        .replace("${V}", "VVV");

                var packageStr = key2.lastIndexOf(".") == -1 ? "default-pkg" : key2.substring(0, key2.lastIndexOf("."));

                if (!packages.containsKey(packageStr)) {
                    packages.put(packageStr, new ObjectArrayList<>());
                }

                if (packageStr.equals("default-pkg")) {
                    pathKey = "default-pkg/" + pathKey;
                }

                packages.get(packageStr).add(Map.entry(classDefinition.getType(), pathKey.substring(pathKey.lastIndexOf("/") + 1) + ".html"));

                classDefinition.setPathKey(pathKey + ".html");

                getUtils().get().getJoinedMappings().get(classDefinition.getJoinedKey()).getPathKeys().put(version, pathKey + ".html");

                var finalHtml = new File(versionDirectory, pathKey + ".html");
                finalHtml.getParentFile().mkdirs();

                searchIndex.add(Map.of(
                    "label", key2,
                    "value",  pathKey + ".html"
                ));

                var page = new DescriptionPage(key2, classDefinition, mappings.get(version), defaultMapping);
                try (var fileWriter = new FileWriter(finalHtml)) {
                    page.generate().render(fileWriter);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            });

            packages.object2ObjectEntrySet().fastForEach(packageEntry -> {
                var key = packageEntry.getKey();
                var paths = packageEntry.getValue();
                var pathKey = key
                        .replace(".", "/")
                        .replace("${V}", "VVV");

                var finalHtml = new File(versionDirectory, pathKey + "/index.html");
                finalHtml.getParentFile().mkdirs();

                var page = new PackageInfoPage(key, paths, defaultMapping);
                try (var fileWriter = new FileWriter(finalHtml)) {
                    page.generate().render(fileWriter);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            });

            var finalHtml = new File(versionDirectory, "index.html");
            finalHtml.getParentFile().mkdirs();

            var page = new OverviewPage("NMS mapping - v" + version, packages.keySet(), defaultMapping, getUtils().get()
                    .getLicenses()
                    .entrySet()
                    .stream()
                    .filter(e -> e.getKey().getKey().equals(version))
                    .collect(Collectors.toMap(e -> e.getKey().getValue(), Map.Entry::getValue))
            );
            try (var fileWriter = new FileWriter(finalHtml)) {
                page.generate().render(fileWriter);
            } catch (IOException exception) {
                exception.printStackTrace();
            }

            try {
                var saver = GsonConfigurationLoader.builder()
                        .file(new File(versionDirectory, "search-index.json"))
                        .build();

                var node = saver.createNode();

                node.set(searchIndex);
                saver.save(node);
            } catch (ConfigurateException e) {
                e.printStackTrace();
            }
        });

        System.out.println("Generating classes history");

        getUtils().get().getJoinedMappings().object2ObjectEntrySet().fastForEach(joinedMappingEntry -> {
            var s = joinedMappingEntry.getKey();
            var m = joinedMappingEntry.getValue();
            var finalHtml = new File(outputFolder, "history/" + s + ".html");
            finalHtml.getParentFile().mkdirs();

            var l = getUtils().get()
                    .getJoinedMappingsClassLinks()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().equals(s))
                    .findFirst()
                    .or(() -> getUtils().get()
                            .getSpigotJoinedMappingsClassLinks()
                            .entrySet()
                            .stream()
                            .filter(entry -> entry.getValue().equals(s))
                            .findFirst()
                    )
                    .map(Map.Entry::getKey)
                    .orElse(s);
            var page = new HistoryPage(l, m, getUtils().get().getJoinedMappingsClassLinks(), getUtils().get().getJoinedMappings());
            try (var fileWriter = new FileWriter(finalHtml)) {
                page.generate().render(fileWriter);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });

        var finalHtml = new File(outputFolder, "index.html");
        finalHtml.getParentFile().mkdirs();

        var page = new MainPage(versions.keySet().stream().sorted(Comparator.comparing(VersionNumber::parse)).collect(Collectors.toList()));
        try (var fileWriter = new FileWriter(finalHtml)) {
            page.generate().render(fileWriter);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        System.out.println("Updating static contents...");
        var staticFolder = new File(outputFolder, "static");
        if (staticFolder.exists()) {
            FileUtils.deleteDirectory(staticFolder);
        }
        FileUtils.copyDirectory(getProject().file("static"), staticFolder);
    }
}
