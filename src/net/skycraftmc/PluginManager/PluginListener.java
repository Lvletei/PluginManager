package net.skycraftmc.PluginManager;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginListener implements Listener
{
    StringConfig        cmdConfig;
    PluginControl       con;
    PluginManagerPlugin pmp;

    PluginListener(PluginManagerPlugin pmp)
    {
        this.pmp = pmp;
        this.cmdConfig = pmp.getCmdConfig();
        this.con = pmp.getPluginControl();
    }

    @EventHandler()
    public void onEnablePlugin(final PluginEnableEvent e)
    {
        final Plugin p = e.getPlugin();
        final String[] cmds = cmdConfig.getStringList(p.getName(), null);
        if (cmds != null)
        {
            Bukkit.getScheduler().scheduleSyncDelayedTask(pmp, new Runnable() {

                @Override
                public void run()
                {
                    for (String s : cmds)
                    {
                        PluginCommand cmd = con.getCommand((JavaPlugin) p, s);
                        if (cmd != null)
                        {
                            con.changePriority(p, cmd, true);
                        }
                    }
                }

            }, 1L);
        }
    }
}
