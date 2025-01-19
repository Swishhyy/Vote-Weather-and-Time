package me.swishhyy.Commands;

import me.swishhyy.VW;
import me.swishhyy.VotingManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StartVoteCommand implements CommandExecutor, TabCompleter {

    private final VW plugin;
    private final VotingManager votingManager;

    public StartVoteCommand(VW plugin, VotingManager votingManager) {
        this.plugin = plugin;
        this.votingManager = votingManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        // Must be a player
        if (!(sender instanceof Player player)) {
            plugin.sendWeatherVoterMessage(sender, "Only players can start a vote!");
            return true;
        }

        // Permission check
        if (!player.hasPermission("vw.startvote")) {
            plugin.sendWeatherVoterMessage(player, "You don't have permission to start votes!");
            return true;
        }

        // Usage check
        if (args.length < 2) {
            plugin.sendWeatherVoterMessage(player,
                    "Usage: /startvote <weatherchange|timechange> <option>");
            return true;
        }

        // If there's already a vote in progress
        if (votingManager.isVoteActive()) {
            plugin.sendWeatherVoterMessage(player,
                    "There's already an active vote in progress!");
            return true;
        }

        // Cooldown check
        if (!votingManager.canStartVote(player)) {
            plugin.sendWeatherVoterMessage(player,
                    "You're on cooldown and cannot start a vote yet!");
            return true;
        }

        // Parse vote type & option
        String voteTypeArg = args[0].toLowerCase();
        String voteOptionArg = args[1].toLowerCase();

        if (voteTypeArg.equals("weatherchange")) {
            votingManager.startVote(player, "weather", voteOptionArg);
        } else if (voteTypeArg.equals("timechange")) {
            votingManager.startVote(player, "time", voteOptionArg);
        } else {
            plugin.sendWeatherVoterMessage(player,
                    "Invalid vote type! Use weatherchange or timechange.");
        }

        return true;
    }

    /**
     * Tab-completion for "/startvote".
     * /startvote <weatherchange|timechange> [options]
     */
    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        // First argument suggestions
        if (args.length == 1) {
            return Arrays.asList("weatherchange", "timechange");
        }

        // Second argument suggestions depend on the first
        if (args.length == 2) {
            String firstArg = args[0].toLowerCase();
            if (firstArg.equals("weatherchange")) {
                // Return weather options
                return Arrays.asList("sunny", "rainy", "stormy");
            } else if (firstArg.equals("timechange")) {
                // Return time options
                return Arrays.asList("day", "noon", "afternoon", "midnight", "dawn");
            } else {
                return Collections.emptyList();
            }
        }

        // No suggestions for further arguments
        return Collections.emptyList();
    }
}
