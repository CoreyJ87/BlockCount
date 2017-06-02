package info.coreyjones.blockcounter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.ScoreboardManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class BlockScoreboard implements CommandExecutor, Listener {

    main plugin;
    Connection dbconnection;
    Map<String, Integer> userCounts = new HashMap<String, Integer>();


    public BlockScoreboard(main plugin, Connection dbconnection) {
        this.plugin = plugin;
        this.dbconnection = dbconnection;
        loadCounts();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        setupPlayerCount(p);
    }
    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {

        Bukkit.getLogger().info(event.getWorld().getName());
        if(event.getWorld().getName().equals("world")) {
            savePlayerCount();
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        savePlayerCount();
    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        p.setScoreboard(plugin.board);
        Objective objective = plugin.board.getObjective("blockcount");
        Score score = objective.getScore(p.getDisplayName());
        score.setScore(userCounts.get(p.getDisplayName()));

        userCounts.put(p.getDisplayName(), userCounts.get(p.getDisplayName()) + 1);

    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Bukkit.getLogger().info("Can't do that from console yet");
            return true;
        } else {
            if(args[0].equals("show")){
                generateBoard( ((Player) sender).getPlayer());
            }
           else if(args[0].equals("hide")){
                ScoreboardManager manager = Bukkit.getScoreboardManager();
                ((Player) sender).getPlayer().setScoreboard(manager.getNewScoreboard());
            }

        }
        return true;
    }

    private void loadCounts() {
        String sql = "SELECT username,count FROM blockcount.BlockCount";
        try {
            PreparedStatement stmt = dbconnection.prepareStatement(sql);
            ResultSet results = stmt.executeQuery();
                while (results.next()) {
                    userCounts.put(results.getString("username"), results.getInt("count"));
                }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void setupPlayerCount(Player eventPlayer) {
        String sql = "SELECT count FROM blockcount.BlockCount WHERE username=\"" + eventPlayer.getDisplayName() + "\"";
        PreparedStatement stmt = null;
        try {
            stmt = dbconnection.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            ResultSet results = stmt.executeQuery();
            if (!results.first()) {
                userCounts.put(eventPlayer.getDisplayName(),0);
                insertUser(eventPlayer.getDisplayName());
                Objective objective = plugin.board.getObjective("blockcount");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Score score = objective.getScore(eventPlayer.getDisplayName());
                    score.setScore(0);
                    p.setScoreboard(plugin.board);
                }
            }
            else {
               generateBoard(eventPlayer);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void generateBoard(Player eventPlayer){
        Objective objective = plugin.board.getObjective("blockcount");
        for (Map.Entry<String, Integer> entry : userCounts.entrySet()) {
            String username = entry.getKey();
            int count = entry.getValue();
            Score score = objective.getScore(username);
            score.setScore(count);
            eventPlayer.setScoreboard(plugin.board);
        }
    }


    private void insertUser(String username) {
        String sql = "INSERT INTO blockcount.BlockCount(username,count) VALUES (?,?)";

        try {
            PreparedStatement stmt = dbconnection.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setInt(2, 0);
            int result = stmt.executeUpdate();
            if (result == 1) {
                Bukkit.getLogger().info("Saved user count for " + username);
            } else {
                Bukkit.getLogger().info("Got back something other than 1 for the update. For username:" + username);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void savePlayerCount() {
        for (Map.Entry<String, Integer> entry : userCounts.entrySet()) {
            String user = entry.getKey();
            int count = entry.getValue();

            String sql = "UPDATE BlockCount SET count=? WHERE username=?";

            try {
                PreparedStatement stmt = dbconnection.prepareStatement(sql);
                stmt.setInt(1,count);
                stmt.setString(2,user);
                int result = stmt.executeUpdate();
                if (result == 1) {
                    Logger logger = plugin.getLogger();
                    logger.info("Saved user count for " + user);
                } else {
                    Logger logger = plugin.getLogger();
                    logger.info("Got back something other than 1 for the update. For username:" + user);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }


        }
    }
}