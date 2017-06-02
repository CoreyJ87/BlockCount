package info.coreyjones.blockcounter;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class main extends JavaPlugin {

    protected BlockScoreboard sMngr;
    protected Scoreboard board;
    protected static Connection connection;
    FileConfiguration config = getConfig();

    public void onEnable() {
        this.getConfig();
        config.addDefault("host","jdbc:mysql://localhost:3306/blockcount");
        config.addDefault("username","myuser");
        config.addDefault("password","mypass");
        config.options().copyDefaults(true);
        saveConfig();
        connectToDB();

        board = Bukkit.getScoreboardManager().getNewScoreboard();
        board.registerNewObjective("blockcount", "dummy");
        Objective objective = board.getObjective("blockcount");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.RED + "Blocks");
        sMngr = new BlockScoreboard(this,connection);


        getServer().getPluginManager().registerEvents(new BlockScoreboard(this,connection),this);
        this.getCommand("blockcount").setExecutor(sMngr);
        Bukkit.getLogger().info("[BlockCount] Enabled!");
    }



    public void onDisable() {
        try {
            if (connection!=null && !connection.isClosed()){

                connection.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        Bukkit.getLogger().info("[BlockCount] Disabled!");
    }


    private void connectToDB(){

        final String username=config.getString("username");
        final String password=config.getString("password");
        final String url=config.getString("host");

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("jdbc driver unavailable!");
            return;
        }
        try {
            connection = DriverManager.getConnection(url,username,password);
            Bukkit.getLogger().info("Made Connection to DB: " + url);
        } catch (SQLException e) {
            Bukkit.getLogger().info("Disabling Block Counter. Cannot connect to database!!!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        String sql = "CREATE TABLE IF NOT EXISTS BlockCount (id INT(6) UNSIGNED AUTO_INCREMENT PRIMARY KEY,username VARCHAR(30) NOT NULL,count INT(30) NOT NULL,reg_date TIMESTAMP)";
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
