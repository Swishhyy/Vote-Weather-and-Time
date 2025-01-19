package me.swishhyy;

import me.swishhyy.Commands.StartVoteCommand;
import me.swishhyy.Commands.VoteCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class VWT extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Store a single VotingManager instance for use throughout the plugin
        VotingManager votingManager = new VotingManager(this);

        if (getCommand("startvote") != null) {
            StartVoteCommand startVoteCmd = new StartVoteCommand(this, votingManager);
            Objects.requireNonNull(getCommand("startvote")).setExecutor(startVoteCmd);
            Objects.requireNonNull(getCommand("startvote")).setTabCompleter(startVoteCmd);
        }

        if (getCommand("vote") != null) {
            VoteCommand voteCmd = new VoteCommand(this, votingManager);
            Objects.requireNonNull(getCommand("vote")).setExecutor(voteCmd);
            Objects.requireNonNull(getCommand("vote")).setTabCompleter(voteCmd);
        }

        getLogger().info("VWT plugin enabled! /startvote and /vote are registered.");
    }

    public void sendWeatherVoterMessage(CommandSender recipient, String text) {
        // Example of a prefix with bold & color. This uses simple color codes.
        // For more advanced styles, you could use Adventure components.
        String prefix = "§e§lWeather§a§lVoter §r"; // Bold yellow "Weather", Bold green "Voter"
        recipient.sendMessage(prefix + text);
    }


    @Override
    public void onDisable() {
        getLogger().info("VWT plugin disabled!");
    }

}
