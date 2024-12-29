package net.minearchive.carpetPermission;

import carpet.CarpetExtension;
import carpet.script.external.Carpet;
import carpet.utils.Messenger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public class CarpetPermissionServer implements CarpetExtension {
    private static CarpetPermissionServer INSTANCE;
    public static Logger LOGGER = LoggerFactory.getLogger("CarpetPermission");
    private File file;
;
    // Map of player UUIDs to their permission level
    // 0 -> no permission
    // 1 -> can use /player command
    // 2 -> can use /carpet and /player command
    private final Map<UUID, PermissionLevel> permissionLevels;

    public CarpetPermissionServer() {
        INSTANCE = this;
        permissionLevels = new HashMap<>();
    }

    public static CarpetPermissionServer getInstance() {
        return INSTANCE;
    }

    @Override
    public void onServerLoaded(MinecraftServer server) {
        if (file == null) file = new File(server.getServerDirectory(), "config/carpetPermission.json");
        if (!fileCheck(file)) return;

        loadData(file);

    }

    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("carpet-permission")
                .requires(source -> source.hasPermission(4))
                .then(argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> suggest(getPlayerSuggestions(context.getSource()), builder))
                        .then(literal("level")
                                .then(literal("0")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "player"));
                                            if (player == null) {
                                                Messenger.m(context.getSource(), "r Player not found");
                                                return 0;
                                            }
                                            permissionLevels.put(player.getUUID(), new PermissionLevel(player.getName().getString(), 0));
                                            Messenger.m(context.getSource(), "gi Set permission level to 0");
                                            player.server.getCommands().sendCommands(player);
                                            return 1;
                                        }))
                                .then(literal("1")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "player"));
                                            if (player == null) {
                                                Messenger.m(context.getSource(), "r Player not found");
                                                return 0;
                                            }
                                            permissionLevels.put(player.getUUID(), new PermissionLevel(player.getName().getString(), 1));
                                            Messenger.m(context.getSource(), "gi Set permission level to 1");
                                            player.server.getCommands().sendCommands(player);
                                            return 1;
                                        }))
                                .then(literal("2")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "player"));
                                            if (player == null) {
                                                Messenger.m(context.getSource(), "r Player not found");
                                                return 0;
                                            }
                                            permissionLevels.put(player.getUUID(), new PermissionLevel(player.getName().getString(), 2));
                                            Messenger.m(context.getSource(), "gi Set permission level to 2");
                                            player.server.getCommands().sendCommands(player);
                                            return 1;
                                        }))
                        )
                );

        dispatcher.register(command);
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        if (file == null) file = new File(server.getServerDirectory(), "config/carpetPermission.json");
        if (!fileCheck(file)) return;

        saveData(file);
    }

    @Override
    public void onPlayerLoggedIn(ServerPlayer player) {
        if (!permissionLevels.containsKey(player.getUUID()) && Carpet.isModdedPlayer(player) == null) {
            if (player.hasPermissions(4)) {
                permissionLevels.put(player.getUUID(), new PermissionLevel(player.getName().getString(), 2));
            } else {
                permissionLevels.put(player.getUUID(), new PermissionLevel(player.getName().getString(), 0));
            }
        }
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    public int getLevel(UUID uuid) {
        if (permissionLevels.containsKey(uuid)) return permissionLevels.get(uuid).level();
        else return 0;
    }

    private void saveData(File filename) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(permissionLevels, writer);
        } catch (IOException e) {
            LOGGER.error("Error saving data: {}", e.getMessage());
        }
    }

    private void loadData(File filename) {
        try (FileReader reader = new FileReader(filename)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element.isJsonNull()) return;

            element.getAsJsonObject().asMap().forEach((key, value) -> {
                UUID uuid = UUID.fromString(key);
                int level = value.getAsJsonObject().asMap().get("level").getAsInt();
                permissionLevels.put(uuid, new PermissionLevel(value.getAsJsonObject().asMap().get("mcid").getAsString(), level));
            });

        } catch (IOException e) {
            LOGGER.error("Error loading data: {}", e.getMessage());
        }
    }


    private static boolean fileCheck (File file) {
        if (file.exists()) return true;
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                LOGGER.error("Error creating parent directories for file");
                return false;
            }
        }

        try {
            if (!file.createNewFile()) {
                LOGGER.error("Error creating file");
                return false;
            }
        } catch (IOException e) {
            LOGGER.error("Error creating file: {}", e.getMessage());
            return false;
        }
        return true;
    }


    /**
     * @param source CommandSourceStack
     * @return Collection of player names
     * <br>
     * <a href="https://github.com/gnembon/fabric-carpet/blob/d9c7334400ecd75f307806e312a2e64a1cc42302/src/main/java/carpet/commands/PlayerCommand.java#L138">From Carpet's PlayerCommand class</a>
     */
    private Collection<String> getPlayerSuggestions(CommandSourceStack source)
    {
        Set<String> players = new LinkedHashSet<>(List.of("Steve", "Alex"));
        players.addAll(source.getOnlinePlayerNames());
        return players;
    }

    public record PermissionLevel(String mcid, int level) {

    }
}
