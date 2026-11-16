package dev.pandemonium;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
                .then(Commands.literal("recipes").executes(ctx -> {
                    reply(ctx, "§e§lMascarilla§r§7: hilo + lana blanca + hilo (en fila, mesa de crafteo) → 2 mascarillas. Se equipan en la cabeza.");
                    reply(ctx, "§e§lFiltro§r§7: papel + carbón vegetal (carbón activo) → 2 filtros. Ten filtros en el inventario con la mascarilla puesta: se usan solos y duran 10 min.");
                    reply(ctx, "§7Protección al contagiar/recibir: nada 100%, casco 80%, mascarilla 45%, mascarilla con filtro 12%.");
                    return 1;
                }))
                .then(Commands.literal("timer").executes(ctx -> {
                    if (!Settings.scheduled()) {
                        reply(ctx, "§7Modo natural. Usa §f/pandemonium mode scheduled§7 para el modo con temporizador.");
                    } else {
                        int s = Settings.remainingTicks() / 20;
                        reply(ctx, String.format("§eSiguiente condición en %d:%02d §7(cada %d min)", s / 60, s % 60, Settings.intervalMinutes()));
                    }
                    return 1;
                }))
                .then(Commands.literal("mode")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("natural").executes(ctx -> {
                            Settings.setScheduled(ctx.getSource().getServer(), false);
                            return 1;
                        }))
                        .then(Commands.literal("scheduled").executes(ctx -> {
                            Settings.setScheduled(ctx.getSource().getServer(), true);
                            return 1;
                        })))
                .then(Commands.literal("interval")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("minutos", IntegerArgumentType.integer(1, 1440)).executes(ctx -> {
                            int min = IntegerArgumentType.getInteger(ctx, "minutos");
                            Settings.setInterval(ctx.getSource().getServer(), min);
                            reply(ctx, "§aIntervalo: " + min + " min (temporizador reiniciado).");
                            return min;
                        })))
                .then(Commands.literal("contagion")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("on").executes(ctx -> {
                            Settings.setContagion(ctx.getSource().getServer(), true);
                            reply(ctx, "§aContagio entre jugadores activado.");
                            return 1;
                        }))
                        .then(Commands.literal("off").executes(ctx -> {
                            Settings.setContagion(ctx.getSource().getServer(), false);
                            reply(ctx, "§aContagio entre jugadores desactivado.");
                            return 1;
                        })))
                .then(Commands.literal("target")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("all").executes(ctx -> {
                            Settings.setTargetAll(ctx.getSource().getServer(), true);
                            reply(ctx, "§aCada ronda toca una condición a TODOS (distinta para cada uno).");
                            return 1;
                        }))
                        .then(Commands.literal("one").executes(ctx -> {
                            Settings.setTargetAll(ctx.getSource().getServer(), false);
                            reply(ctx, "§aCada ronda toca a UN jugador al azar.");
                            return 1;
                        })))
                .then(Commands.literal("next")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> {
                            Settings.dispense(ctx.getSource().getServer());
                            return 1;
                        }))
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
