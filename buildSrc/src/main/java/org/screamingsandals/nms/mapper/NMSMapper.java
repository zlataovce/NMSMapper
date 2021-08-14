package org.screamingsandals.nms.mapper;

import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import lombok.SneakyThrows;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.screamingsandals.nms.mapper.extension.Version;
import org.screamingsandals.nms.mapper.tasks.*;
import org.screamingsandals.nms.mapper.utils.UtilsHolder;
import org.screamingsandals.nms.mapper.workspace.Workspace;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

import java.nio.file.Files;
import java.util.Objects;
import java.util.stream.Stream;

public class NMSMapper implements Plugin<Project> {
    @SneakyThrows
    @Override
    public void apply(Project project) {
        project.getTasks().create("generateNmsConfig", ConfigGenerationTask.class, task ->
                task.getConfigFolder().set(project.file("config"))
        );

        var utilsHolder = new UtilsHolder(
                project.file("src/main/resources/nms-mappings")
        );

        var configFolder = project.file("config");

        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }

        var mainWorkspaceDirectory = project.file("work");

        if (!mainWorkspaceDirectory.exists()) {
            mainWorkspaceDirectory.mkdirs();
        }

        try (var stream = Files.walk(configFolder.toPath().toAbsolutePath())) {
            final var versions = stream.filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().toLowerCase().endsWith(".disabled"))
                    .map(path -> path.resolve("info.json"))
                    .filter(Files::exists)
                    .filter(Files::isRegularFile)
                    .map(path -> {
                                try {
                                    return GsonConfigurationLoader.builder()
                                        .path(path)
                                            .build()
                                            .load()
                                            .get(Version.class);
                                } catch (ConfigurateException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }
                    )
                    .filter(Objects::nonNull)
                    .collect(ObjectImmutableList.toList());

            if (!versions.isEmpty()) {
                versions.forEach(version -> {
                    version.setWorkspace(new Workspace(version.getVersion(), mainWorkspaceDirectory));
                    project.getTasks().create("remapVersion" + version.getVersion(), RemappingTask.class, remappingTask -> {
                        remappingTask.getVersion().set(version);

                        remappingTask.getUtils().set(utilsHolder);
                    });
                });

                project.getTasks().create("createJoinedMappings", JoinedMappingTask.class, joinedMappingTask -> {
                    joinedMappingTask.getUtils().set(utilsHolder);

                    joinedMappingTask.dependsOn(versions.stream().map(s -> "remapVersion" + s.getVersion()).toArray());
                });

                project.getTasks().create("saveNmsMappings", SaveMappingsTask.class, saveMappingsTask -> {
                    saveMappingsTask.getUtils().set(utilsHolder);

                    saveMappingsTask.dependsOn(Stream.concat(versions.stream().map(s -> "remapVersion" + s.getVersion()), Stream.of("createJoinedMappings")).toArray());
                });

                project.getTasks().create("generateNmsDocs", DocsGenerationTask.class, docsGenerationTask -> {
                    docsGenerationTask.getUtils().set(utilsHolder);
                    docsGenerationTask.getOutputFolder().set(project.file("build/docs"));

                    docsGenerationTask.dependsOn(Stream.concat(versions.stream().map(s -> "remapVersion" + s.getVersion()), Stream.of("createJoinedMappings")).toArray());
                });

                project.getTasks().create("uploadNmsDocs", UploadNmsDocsTask.class, uploadNmsDocsTask -> {
                    uploadNmsDocsTask.getDocsFolder().set(project.file("build/docs"));

                    uploadNmsDocsTask.dependsOn("generateNmsDocs");
                });
            }
        }
    }
}
