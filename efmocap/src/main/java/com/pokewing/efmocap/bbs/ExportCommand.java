package com.pokewing.efmocap.bbs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.List;

/**
 * In-game commands:
 * <ul>
 *   <li>{@code /efbbs list} - list all Epic Fight animation keys</li>
 *   <li>{@code /efbbs export <key>} - export one animation to BBS format</li>
 *   <li>{@code /efbbs exportall} - export every animation</li>
 *   <li>{@code /efbbs reloadconfig} - reload retarget.json</li>
 * </ul>
 */
public final class ExportCommand {
    private ExportCommand() {}

    /** Subtree grafted under /efmocap, so one mod owns one command root. */
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("bbsexport")
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("exportall").executes(ctx -> exportAll(ctx.getSource())))
                .then(Commands.literal("reloadconfig").executes(ctx -> {
                    RetargetConfig.loadOrCreate();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "[efbbs] Reloaded " + RetargetConfig.configPath()), false);
                    return 1;
                }))
                .then(Commands.literal("export")
                        .then(Commands.argument("key", StringArgumentType.greedyString())
                                .executes(ctx -> exportOne(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "key")))));
    }

    private static boolean guard(CommandSourceStack src) {
        if (!com.pokewing.efmocap.EFMocap.isEpicFightLoaded()) {
            src.sendFailure(Component.literal("[efbbs] Epic Fight is not installed."));
            return false;
        }
        return true;
    }

    private static int list(CommandSourceStack src) {
        if (!guard(src)) return 0;
        try {
            List<ResourceLocation> keys = EpicFightAccess.listAllKeys();
            if (keys.isEmpty()) {
                src.sendFailure(Component.literal(
                        "[efbbs] No animations found (registry not readable on this Epic Fight build)."));
                return 0;
            }
            src.sendSuccess(() -> Component.literal("[efbbs] " + keys.size() + " animations:"), false);
            keys.stream().limit(200).forEach(k ->
                    src.sendSuccess(() -> Component.literal(" - " + k), false));
            return keys.size();
        } catch (Exception e) {
            src.sendFailure(Component.literal("[efbbs] list failed: " + e.getMessage()));
            com.pokewing.efmocap.EFMocap.LOG.error("[efbbs] list failed", e);
            return 0;
        }
    }

    private static int exportOne(CommandSourceStack src, String keyStr) {
        if (!guard(src)) return 0;
        RetargetConfig cfg = RetargetConfig.loadOrCreate();
        try {
            ResourceLocation key = new ResourceLocation(keyStr.trim());
            Path out = AnimationExporter.export(key, cfg);
            src.sendSuccess(() -> Component.literal("[efbbs] Exported -> " + out), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("[efbbs] export failed: " + e.getMessage()));
            com.pokewing.efmocap.EFMocap.LOG.error("[efbbs] export failed for {}", keyStr, e);
            return 0;
        }
    }

    private static int exportAll(CommandSourceStack src) {
        if (!guard(src)) return 0;
        RetargetConfig cfg = RetargetConfig.loadOrCreate();
        int ok = 0, fail = 0;
        try {
            List<ResourceLocation> keys = EpicFightAccess.listAllKeys();
            for (ResourceLocation key : keys) {
                try {
                    AnimationExporter.export(key, cfg);
                    ok++;
                } catch (Exception e) {
                    fail++;
                    com.pokewing.efmocap.EFMocap.LOG.warn("[efbbs] skip {}: {}", key, e.getMessage());
                }
            }
        } catch (Exception e) {
            src.sendFailure(Component.literal("[efbbs] exportall failed: " + e.getMessage()));
            return 0;
        }
        final int fok = ok, ffail = fail;
        src.sendSuccess(() -> Component.literal(
                "[efbbs] Exported " + fok + " animations (" + ffail + " skipped) -> "
                        + AnimationExporter.outputDir()), false);
        return ok;
    }
}
