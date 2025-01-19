package me.swishhyy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VotingManager {

    private final VW plugin;             // Reference to your main plugin
    private boolean voteActive = false;
    private String voteType;              // "weather" or "time"
    private String proposedOption;        // e.g., "sunny", "stormy", "day", "noon", etc.

    // Cooldown tracking: map player UUID -> last time (ms) they started a vote
    private final Map<String, Long> voteCooldowns = new HashMap<>();

    // Sets of voters (yes/no)
    private final Set<String> yesVoters = new HashSet<>();
    private final Set<String> noVoters  = new HashSet<>();

    // Example cooldown of 60 seconds
    private static final long COOLDOWN_MS = 60_000L;

    public VotingManager(VW plugin) {
        this.plugin = plugin;
    }

    // ----- Public Methods -----

    public boolean isVoteActive() {
        return voteActive;
    }

    public boolean canStartVote(Player player) {
        // Admins bypass cooldown
        if (player.hasPermission("vw.admin")) {
            return true;
        }
        // Check if the player has started a vote recently
        Long lastVoteTime = voteCooldowns.get(player.getUniqueId().toString());
        if (lastVoteTime == null) {
            return true; // never started a vote
        }
        long elapsed = System.currentTimeMillis() - lastVoteTime;
        return elapsed >= COOLDOWN_MS;
    }

    public void startVote(Player starter, String voteType, String proposedOption) {
        this.voteActive = true;
        this.voteType = voteType;
        this.proposedOption = proposedOption;
        yesVoters.clear();
        noVoters.clear();

        // Record the time the player started a vote
        voteCooldowns.put(starter.getUniqueId().toString(), System.currentTimeMillis());

        // Announce vote start to everyone, using your "WeatherVoter" prefix
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.sendWeatherVoterMessage(p,
                    "A vote has been started by " + starter.getName()
                            + " for " + voteType + ": " + proposedOption
            );
            plugin.sendWeatherVoterMessage(p,
                    "Type /vote yes or /vote no to cast your vote!"
            );
        }
    }

    public void castVote(Player player, boolean voteYes) {
        // Remove from both sets in case the player is changing their vote
        yesVoters.remove(player.getUniqueId().toString());
        noVoters.remove(player.getUniqueId().toString());

        if (voteYes) {
            yesVoters.add(player.getUniqueId().toString());
            plugin.sendWeatherVoterMessage(player,
                    "You voted YES for the " + voteType + " vote!"
            );
        } else {
            noVoters.add(player.getUniqueId().toString());
            plugin.sendWeatherVoterMessage(player,
                    "You voted NO for the " + voteType + " vote!"
            );
        }

        checkVoteOutcome();
    }

    // ----- Internal Logic -----

    private void checkVoteOutcome() {
        final int yesCount = yesVoters.size();
        final int noCount  = noVoters.size();
        final int totalVotes = yesCount + noCount;

        // Simple logic: if there's at least 1 vote and yes > no, pass immediately
        if (totalVotes > 0 && yesCount > noCount) {
            applyVoteResult();
            return;
        }

        // Example threshold: after 3 or more total votes, we finalize that the vote fails
        // (You can adjust this logic as needed.)
        if (totalVotes >= 3) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.sendWeatherVoterMessage(p,
                        "Vote failed! No majority to change " + voteType
                );
            }
            resetVote();
        }
    }

    /**
     * Called when the vote passes (yes > no).
     * Starts a 5-second countdown, then applies the relevant changes.
     */
    private void applyVoteResult() {
        final String finalVoteType    = this.voteType;
        final String finalOption      = this.proposedOption;

        // We'll disable "voteActive" so no more votes can come in
        // but keep the data around (finalVoteType, finalOption) for the countdown.
        voteActive = false;
        yesVoters.clear();
        noVoters.clear();

        // Announce the vote result to everyone
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.sendWeatherVoterMessage(p,
                    "Vote passed! " + finalVoteType + " will be changed to '"
                            + finalOption + "' in 5 seconds..."
            );
        }

        // Start a repeating task that counts down from 5 to 0
        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                // If we've hit 0 seconds, apply the changes & reset
                if (countdown <= 0) {
                    applyFinalChanges(finalVoteType, finalOption);
                    resetVote();
                    this.cancel();
                    return;
                }

                // Otherwise, broadcast the countdown
                for (Player p : Bukkit.getOnlinePlayers()) {
                    // "solid bold red text" => "§c§l"
                    plugin.sendWeatherVoterMessage(p, "§c§l" + countdown + "...");
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
        // 20 ticks = 1 second, so starts in 1s, repeats every 1s
    }

    /**
     * Applies the actual weather/time changes now that countdown is complete.
     */
    private void applyFinalChanges(String finalVoteType, String finalOption) {
        // Let everyone know changes are being applied
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.sendWeatherVoterMessage(p,
                    "Applying " + finalVoteType + " change to '" + finalOption + "'!"
            );
        }

        if ("weather".equalsIgnoreCase(finalVoteType)) {
            applyWeatherChange(finalOption);
        } else if ("time".equalsIgnoreCase(finalVoteType)) {
            applyTimeChange(finalOption);
        }
    }

    private void applyWeatherChange(String option) {
        Bukkit.getWorlds().forEach(world -> {
            switch (option.toLowerCase()) {
                case "sunny" -> {
                    world.setStorm(false);
                    world.setThundering(false);
                }
                case "rainy" -> {
                    world.setStorm(true);
                    world.setThundering(false);
                }
                case "stormy" -> {
                    world.setStorm(true);
                    world.setThundering(true);
                }
                // Otherwise, do nothing for unknown option
            }
        });
    }

    private void applyTimeChange(String option) {
        // Example times in ticks
        long timeValue = switch (option.toLowerCase()) {
            case "day"       -> 1000L;
            case "noon"      -> 6000L;
            case "afternoon" -> 12000L;
            case "midnight"  -> 18000L;
            case "dawn"      -> 23000L;
            default          -> 0L;
        };

        Bukkit.getWorlds().forEach(world -> world.setTime(timeValue));
    }

    /**
     * Reset the vote so new ones can be started.
     */
    private void resetVote() {
        voteActive     = false;
        voteType       = null;
        proposedOption = null;
        yesVoters.clear();
        noVoters.clear();
    }
}
