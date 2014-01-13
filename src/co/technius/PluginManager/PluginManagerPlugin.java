package co.technius.PluginManager;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import co.technius.PluginManager.DBOUtilities.VersionInfo;

public class PluginManagerPlugin extends JavaPlugin
{

    private PluginControl control;
    private boolean       unl = false;

    private StringConfig  cmdConfig;

    StringConfig getCmdConfig()
    {
        return cmdConfig;
    }

    public PluginControl getPluginControl()
    {
        return control;
    }

    @Override
    public void onDisable()
    {
        try
        {
            cmdConfig.save();
            cmdConfig.close();
        }
        catch (IOException e)
        {
            getLogger().severe("Failed to save config!");
            e.printStackTrace();
        }
        if (!unl && control != null)
        {
            control.cleanup();
        }
    }

    @Override
    public void onEnable()
    {
        try
        {
            control = new PluginControl(cmdConfig);
        }
        catch (Exception e)
        {
        	getLogger().log(Level.SEVERE, "Failed to start", e);
            setEnabled(false);
            return;
        }
        
        cmdConfig = new StringConfig(
                new File(getDataFolder() + File.separator + "commands.cfg").getAbsolutePath());
        try
        {
            cmdConfig.start();
            cmdConfig.load();
        }
        catch (IOException e1)
        {
            getLogger().severe("Failed to initalize config!");
            e1.printStackTrace();
        }

        getCommand("pluginmanager").setExecutor(new PMCommandExecutor(this, control));
        final Plugin pm = this;
        getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run()
            {
                try
                {
                    VersionInfo info = DBOUtilities.isUpToDate(pm, "pm-pluginmanager");
                    if (info == VersionInfo.OLD)
                    {
                        getLogger()
                                .info("A new version of PluginManager is available: "
                                        + DBOUtilities.getLatestVersion("pm-pluginmanager").version);
                    }
                }
                catch (Exception e)
                {
                    getLogger().log(Level.WARNING, "Could not find update", e);
                }
            }
        });
        updateCommandPriorities();
        getServer().getPluginManager().registerEvents(new PluginListener(this), this);
    }

    void setUnload(boolean unl)
    {
        this.unl = unl;
    }

    private void updateCommandPriorities()
    {
        for (Plugin p : Bukkit.getPluginManager().getPlugins())
        {
            final String[] cmds = cmdConfig.getStringList(p.getName(), null);
            if (cmds != null)
            {
                for (String s : cmds)
                {
                    PluginCommand cmd = control.getCommand((JavaPlugin) p, s);
                    if (cmd != null)
                    {
                        control.changePriority(p, cmd, true);
                    }
                }
            }

		}
	}
}