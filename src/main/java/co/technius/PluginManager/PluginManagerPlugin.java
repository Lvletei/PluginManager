package co.technius.PluginManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import co.technius.PluginManager.DBOUtilities.VersionInfo;

public class PluginManagerPlugin extends JavaPlugin {

    private PluginControl control;
    private boolean unl = false;

    private StringConfig cmdConfig;
    private StringConfig config;

    public PluginControl getPluginControl() {
        return control;
    }

    @Override
    public void onDisable() {
        try {
            cmdConfig.save();
            cmdConfig.close();
        } catch (final IOException e) {
            getLogger().severe("Failed to save command priorities!");
            e.printStackTrace();
        }
        if (!unl && control != null) {
            control.cleanup();
        }
    }

    @Override
    public void onEnable() {
        cmdConfig = new StringConfig(new File(getDataFolder(), "commands.cfg"));
        final File fconfig = new File(getDataFolder(), "config.txt");
        config = new StringConfig(fconfig);
        try (InputStream is = getClass().getResourceAsStream("/defaultconfig.txt");){
            cmdConfig.start();
            cmdConfig.load();
            if (!fconfig.exists()) {
                Files.copy(is, fconfig.toPath());
            }
            config.load();
        } catch (final IOException e) {
            getLogger().severe("Failed to initalize config!");
            e.printStackTrace();
        }

        try {
            control = new PluginControl(cmdConfig);
        } catch (final Exception e) {
            getLogger().log(Level.SEVERE, "Failed to start", e);
            setEnabled(false);
            return;
        }

        getCommand("pluginmanager").setExecutor(new PMCommandExecutor(this, control));
        if (config.getBoolean("updater.startcheck", true)) {
            final Plugin pm = this;

            new BukkitRunnable() {

                @Override
                public void run() {
                    try {
                        final VersionInfo info = DBOUtilities.isUpToDate(pm, "pm-pluginmanager");
                        if (info == VersionInfo.OLD) {
                            getLogger().info("A new version of PluginManager is available: " + DBOUtilities.getLatestVersion("pm-pluginmanager").version);
                        }
                    } catch (final Exception e) {
                        getLogger().log(Level.WARNING, "Could not find update", e);
                    }
                }

            }.runTaskAsynchronously(this);
        }
        updateCommandPriorities();
        getServer().getPluginManager().registerEvents(new PluginListener(this), this);
    }

    StringConfig getCmdConfig() {
        return cmdConfig;
    }

    void setUnload(final boolean unl) {
        this.unl = unl;
    }

    private void updateCommandPriorities() {
        for (final Plugin p : Bukkit.getPluginManager().getPlugins()) {
            final String[] cmds = cmdConfig.getStringList(p.getName(), null);
            if (cmds != null) {
                for (final String s : cmds) {
                    final PluginCommand cmd = control.getCommand((JavaPlugin) p, s);
                    if (cmd != null) {
                        control.changePriority(p, cmd, true);
                    }
                }
            }
        }
    }
}
