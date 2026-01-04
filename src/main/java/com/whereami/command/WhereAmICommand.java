package com.whereami.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.HashSet;
import java.util.Set;

public class WhereAmICommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("whereami")
                        .then(Commands.literal("here")
                                .executes(ctx -> executeScan(ctx.getSource(), 1))
                        )
                        .then(Commands.literal("scan")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0, 10))
                                        .executes(ctx ->
                                                executeScan(
                                                        ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "radius")
                                                )
                                        )
                                )
                        )
        );
    }

    private static int executeScan(CommandSourceStack source, int radius) {

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();
        ChunkPos centerChunk = new ChunkPos(playerPos);

        Set<ResourceLocation> foundStructures = new HashSet<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {

                ChunkPos chunkPos = new ChunkPos(
                        centerChunk.x + dx,
                        centerChunk.z + dz
                );

                BlockPos checkPos = chunkPos.getWorldPosition();

                var structureStarts =
                        level.structureManager().getAllStructuresAt(checkPos);

                for (Structure structure : structureStarts.keySet()) {
                    ResourceLocation id = level.registryAccess()
                            .registryOrThrow(Registries.STRUCTURE)
                            .getKey(structure);

                    if (id != null) {
                        foundStructures.add(id);
                    }
                }
            }
        }

        if (foundStructures.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal("No structures found in radius"),
                    false
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal("Structures found:"),
                    false
            );

            for (ResourceLocation id : foundStructures) {
                source.sendSuccess(
                        () -> Component.literal("- " + id),
                        false
                );
            }
        }

        return 1;
    }
}
