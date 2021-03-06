package nl.zandervdm.stayput;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import nl.zandervdm.stayput.Commands.StayputCommand;
import nl.zandervdm.stayput.Listeners.PlayerQuitEventListener;
import nl.zandervdm.stayput.Listeners.PlayerTeleportEventListener;
import nl.zandervdm.stayput.Models.Position;
import nl.zandervdm.stayput.Repositories.PositionRepository;
import nl.zandervdm.stayput.Utils.ConfigManager;
import nl.zandervdm.stayput.Utils.RuleManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.Collection;

public class Main extends JavaPlugin {

    public static FileConfiguration config;

    //Util classes
    protected ConfigManager configManager;
    protected RuleManager ruleManager;

    //Database connection stuff
    protected ConnectionSource connectionSource;

    //Data mappers
    protected Dao<Position, Integer> positionMapper;

    //Repositories
    protected PositionRepository positionRepository;

    /**
     * Permissions:
     * stayput.use
     * stayput.override
     * stayput.admin
     */

    @Override
    public void onEnable(){
        setupClasses();
        setupConfig();
        setupListeners();
        setupCommands();
        setupDatabase();
        setupDao();
        setupTables();
    }

    @Override
    public void onDisable(){
        syncPlayerstoDatabase();
    }

    public Dao<Position, Integer> getPositionMapper(){
        return this.positionMapper;
    }

    public PositionRepository getPositionRepository(){
        return this.positionRepository;
    }

    public RuleManager getRuleManager(){
        return this.ruleManager;
    }

    protected void setupClasses(){
        this.configManager = new ConfigManager(this);
        this.ruleManager = new RuleManager(this);

        this.positionRepository = new PositionRepository(this);
    }

    public void setupConfig(){
        this.configManager.createConfig();
        config = getConfig();
        if(Main.config.getBoolean("debug")) getLogger().info("Setting up config");
    }

    protected void setupListeners(){
        getServer().getPluginManager().registerEvents(new PlayerTeleportEventListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitEventListener(this), this);
        if(Main.config.getBoolean("debug")) getLogger().info("Setting up listeners");
    }

    protected void setupCommands(){
        this.getCommand("stayput").setExecutor(new StayputCommand(this));
    }

    protected void setupDatabase(){
        if(Main.config.getString("type").equals("sqlite")){
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            File file = new File(this.getDataFolder(), "database.db");
            String datasource = "jdbc:sqlite:" + file;
            connectionSource = null;
            try {
                connectionSource = new JdbcConnectionSource(datasource);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }else if(Main.config.getString("type").equals("mysql")) {
            String host = Main.config.getString("mysql.host");
            Integer port = Main.config.getInt("mysql.port");
            String database = Main.config.getString("mysql.database");
            String username = Main.config.getString("mysql.username");
            String password = Main.config.getString("mysql.password");
            String datasource = "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true";
            connectionSource = null;
            try {
                connectionSource = new JdbcConnectionSource(datasource, username, password);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }else{
            getLogger().warning("Invalid database connection type chosen!");
        }
        if(Main.config.getBoolean("debug")) getLogger().info("Setting up database");
    }

    protected void setupDao(){
        positionMapper = null;
        try {
            positionMapper = DaoManager.createDao(connectionSource, Position.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void setupTables() {
        try {
            TableUtils.createTableIfNotExists(connectionSource, Position.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if(Main.config.getBoolean("debug")) getLogger().info("Setting up tables");
    }

    protected void syncPlayerstoDatabase(){
        Collection<? extends Player> players = getServer().getOnlinePlayers();
        for(Player player : players){
            if(player.hasPermission("stayput.use")) {
                this.getPositionRepository().updateLocationForPlayer(player, player.getLocation());
            }
        }
    }

}
