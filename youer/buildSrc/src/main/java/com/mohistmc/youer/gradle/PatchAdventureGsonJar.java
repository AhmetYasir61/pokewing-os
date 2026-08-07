package com.mohistmc.youer.gradle;

import java.io.IOException;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Produces a compatibility-patched copy of {@code adventure-text-serializer-gson} for bundling into
 * the server jar. See {@link AdventureGsonCompatPatcher} for what the patch restores and why.
 */
@CacheableTask
public abstract class PatchAdventureGsonJar extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInputJar();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @TaskAction
    public void patch() throws IOException {
        var input = getInputJar().get().getAsFile().toPath();
        var output = getOutputJar().get().getAsFile().toPath();
        if (AdventureGsonCompatPatcher.patch(input, output)) {
            getLogger().lifecycle("Restored the Adventure 4.x gson legacy-hover API in {}", output.getFileName());
        } else {
            getLogger().lifecycle("{} already exposes the legacy gson hover API, bundling as-is", input.getFileName());
        }
    }
}
