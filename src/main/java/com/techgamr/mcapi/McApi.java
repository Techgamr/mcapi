package com.techgamr.mcapi;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(McApi.MODID)
public class McApi {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "mcapi";

    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ApiServer http = new ApiServer();

    public static final PermissionNode<Boolean> P_RELOAD = new PermissionNode<>(
            new ResourceLocation(McApi.MODID, "reload"),
            PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> player != null && player.hasPermissions(4)
    );

    public McApi() {
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStart(ServerStartingEvent event) {
        http.start(event.getServer());
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppingEvent event) {
        http.stop();
    }

    @SubscribeEvent
    public void onPermissionNodesRegister(PermissionGatherEvent.Nodes event) {
        P_RELOAD.setInformation(Component.literal("Reload"), Component.literal("Can reload the config"));
        event.addNodes(List.of(P_RELOAD));
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> commandBase = Commands.literal(MODID);
        dispatcher.register(commandBase.then(Commands.literal("key")
                .executes(context -> {
                    String apiKey;
                    try {
                        apiKey = ApiKeyManager.generateApiKey(Objects.requireNonNull(context.getSource().getPlayer()).getUUID());
                    } catch (Exception e) {
                        UUID exUuid = UUID.randomUUID();
                        LOGGER.error("Failed to regenerate API key (Error {})", exUuid, e);
                        context.getSource().sendFailure(
                                Component.empty()
                                        .append(
                                                Component.literal("Failed to generate a new API key, contact the server admin")
                                                        .withStyle(ChatFormatting.RED)
                                        )
                                        .append("\n")
                                        .append(Component.empty()
                                                .withStyle(ChatFormatting.DARK_RED)
                                                .withStyle(ChatFormatting.ITALIC)
                                                .append(Component.literal("Error ID").withStyle(ChatFormatting.BOLD))
                                                .append(": ")
                                                .append(
                                                        Component.literal(exUuid.toString()).withStyle(style -> style
                                                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, exUuid.toString()))
                                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy!")))
                                                        )
                                                )
                                        )
                        );
                        return 1;
                    }
                    context.getSource().sendSuccess(
                            () -> Component.empty()
                                    .append(
                                            Component.literal("Your API key has been regenerated:")
                                                    .withStyle(ChatFormatting.GREEN)
                                                    .withStyle(ChatFormatting.BOLD)
                                    )
                                    .append("\n")
                                    .append(
                                            Component.literal(apiKey).withStyle(style -> style
                                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, apiKey))
                                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy!")))
                                                    .withColor(ChatFormatting.DARK_GREEN)
                                                    .withUnderlined(true)
                                            )
                                    ),
                            false
                    );
                    return 1;
                })
        ));
        dispatcher.register(commandBase.then(Commands.literal("reload")
                .requires(src -> {
                    if (src.isPlayer()) {
                        return PermissionAPI.getPermission(Objects.requireNonNull(src.getPlayer()), P_RELOAD);
                    }
                    return src.hasPermission(4);
                })
                .executes(context -> {
                    http.restart(context.getSource().getServer());
                    context.getSource().sendSuccess(
                            () -> Component.literal("Reload command executed!"),
                            false
                    );
                    return 1;
                })
        ));
    }
}
