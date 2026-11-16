package dev.pandemonium;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /pandemonium list                          -> tus condiciones
 * /pandemonium catalog                       -> todas las que existen
 * /pandemonium give <jugadores> <id|random>  -> (op) da una
 * /pandemonium remove <jugadores> <id|all>   -> (op) quita una o todas
 */
public final class PandemoniumCommands {
    private PandemoniumCommands() {}

    private static final SuggestionProvider<CommandSourceStack> GIVE_IDS = (ctx, builder) -> {
        List<String> ids = Conditions.ids();
        ids.add("random");
        return SharedSuggestionProvider.suggest(ids, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> REMOVE_IDS = (ctx, builder) -> {
        List<String> ids = Conditions.ids();
        ids.add("all");
        return SharedSuggestionProvider.suggest(ids, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pandemonium")
                .then(Commands.literal("list").executes(PandemoniumCommands::list))
                .then(Commands.literal("catalog").executes(PandemoniumCommands::catalog))
                .then(Commands.literal("give")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("condition", StringArgumentType.word())
                                        .suggests(GIVE_IDS)
                                        .executes(PandemoniumCommands::give))))
                .then(Commands.literal("remove")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("condition", StringArgumentType.word())
                                        .suggests(REMOVE_IDS)
                                        .executes(PandemoniumCommands::remove)))));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        Triggers.onList(p);
        List<Condition> mine = ConditionStore.list(p);
        if (mine.isEmpty()) {
            reply(ctx, "§aEstás sano... por ahora.");
            return 0;
        }
        reply(ctx, "§6Tus condiciones:");
        for (Condition c : mine) {
            reply(ctx, "§c" + c.name + " §7- " + c.description);
        }
        return mine.size();
    }

    private static int catalog(CommandContext<CommandSourceStack> ctx) {
        reply(ctx, "§6Todas las condiciones (" + Conditions.ALL.size() + "):");
        for (Condition c : Conditions.ALL) {
            reply(ctx, "§e" + c.id + " §f" + c.name + " §7- " + c.description);
        }
        return Conditions.ALL.size();
    }

    private static int give(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        String id = StringArgumentType.getString(ctx, "condition");
        int done = 0;
        for (ServerPlayer p : targets) {
            Condition c;
            if (id.equals("random")) {
                c = Conditions.ALL.get(p.getRandom().nextInt(Conditions.ALL.size()));
            } else {
                c = Conditions.get(id);
                if (c == null) {
                    ctx.getSource().sendFailure(Component.literal("No existe la condición '" + id + "'. Mira /pandemonium catalog"));
                    return 0;
                }
            }
            if (ConditionStore.give(p, c)) done++;
        }
        reply(ctx, "§aCondiciones dadas: " + done);
        return done;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        String id = StringArgumentType.getString(ctx, "condition");
        int done = 0;
        for (ServerPlayer p : targets) {
            if (id.equals("all")) {
                done += ConditionStore.list(p).size();
                ConditionStore.removeAll(p);
            } else {
                Condition c = Conditions.get(id);
                if (c == null) {
                    ctx.getSource().sendFailure(Component.literal("No existe la condición '" + id + "'. Mira /pandemonium catalog"));
                    return 0;
                }
                if (ConditionStore.remove(p, c)) done++;
            }
        }
        reply(ctx, "§aCondiciones quitadas: " + done);
        return done;
    }

    private static void reply(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }
}
