package com.mohistmc.youer.gradle;

import java.io.IOException;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Produces a compatibility-patched copy of an Adventure artifact for bundling into the server jar.
 * See {@link AdventureCompatPatcher} for what each patch restores and why.
 */
@CacheableTask
public abstract class PatchAdventureJar extends DefaultTask {
    @Input
    public abstract Property<AdventureCompatPatcher.Module> getModule();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInputJar();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @TaskAction
    public void patch() throws IOException {
        var input = getInputJar().get().getAsFile().toPath();
        var output = getOutputJar().get().getAsFile().toPath();
        if (AdventureCompatPatcher.patch(getModule().get(), input, output)) {
            getLogger().lifecycle("Restored the Adventure 4.x API removed from {}", output.getFileName());
        } else {
            getLogger().lifecycle("{} already exposes the Adventure 4.x API, bundling as-is", input.getFileName());
        }
    }
}
