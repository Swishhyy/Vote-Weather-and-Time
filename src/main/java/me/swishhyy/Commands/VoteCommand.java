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

public class VoteCommand implements CommandExecutor, TabCompleter {

    private final VW plugin;
    private final VotingManager votingManager;

    public VoteCommand(VW plugin, VotingManager votingManager) {
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
            plugin.sendWeatherVoterMessage(sender, "Only players can vote!");
            return true;
        }

        // Permission check
        if (!player.hasPermission("vw.vote")) {
            plugin.sendWeatherVoterMessage(player, "You don't have permission to vote!");
            return true;
        }

        // Check usage
        if (args.length < 1) {
            plugin.sendWeatherVoterMessage(player, "Usage: /vote <yes|no>");
            return true;
        }

        // Make sure a vote is active
        if (!votingManager.isVoteActive()) {
            plugin.sendWeatherVoterMessage(player,
                    "There is no active vote to participate in!");
            return true;
        }

        // Only "yes" or "no"
        String voteArg = args[0].toLowerCase();
        if (voteArg.equals("yes")) {
            votingManager.castVote(player, true);
        } else if (voteArg.equals("no")) {
            votingManager.castVote(player, false);
        } else {
            plugin.sendWeatherVoterMessage(player,
                    "Invalid vote! Use /vote yes or /vote no.");
        }

        return true;
    }

    /**
     * Tab-completion for "/vote".
     * If player types "/vote " and presses tab, suggests "yes" or "no".
     */
    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        // If they're typing the first argument (args.length == 1), suggest yes/no
        if (args.length == 1) {
            return Arrays.asList("yes", "no");
        }
        // Otherwise, no suggestions
        return Collections.emptyList();
    }
}
