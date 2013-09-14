package net.skycraftmc.PluginManager;

import java.io.File;
import java.io.IOException;

import org.bukkit.plugin.java.JavaPlugin;

public class PluginManagerPlugin extends JavaPlugin
{

    private PluginControl control;
    private boolean       unl = false;

    private StringConfig  cmdConfig;

    public PluginControl getPluginControl()
    {
        return control;
    }

    StringConfig getCmdConfig()
    {
        return cmdConfig;
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
        if (!unl)
            control.cleanup();
    }

    @Override
    public void onEnable()
    {
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

        try
        {
            control = new PluginControl(cmdConfig);
        }
        catch (Exception e)
        {
            getLogger().severe("Failed to start!");
            setEnabled(false);
            e.printStackTrace();
            return;
        }

        getCommand("pluginmanager").setExecutor(new PMCommandExecutor(this, control));
        getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {

            public void run()
            {
                try
                {
                    UpdateInformation inf = Updater.findUpdate("pm-pluginmanager");
                    if (!getDescription().getVersion().equals(inf.getVersion()))
                        getLogger().info(
                                "A new version of PluginManager is available: " + inf.getVersion());
                }
                catch (Exception e)
                {
                    getLogger().warning("Could not find update: " + e.getMessage());
                }
            }
        });
        getServer().getPluginManager().registerEvents(new PluginListener(this), this);
    }

    void setUnload(boolean unl)
    {
        this.unl = unl;
    }
}
