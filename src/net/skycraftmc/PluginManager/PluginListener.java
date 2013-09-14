package net.skycraftmc.PluginManager;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

public class PluginListener implements Listener
{
    StringConfig  cmdConfig;
    PluginControl con;

    PluginListener(PluginManagerPlugin pmp)
    {
        this.cmdConfig = pmp.getCmdConfig();
        this.con = pmp.getPluginControl();
    }

    @EventHandler()
    public void onEnablePlugin(PluginEnableEvent e)
    {
        Plugin p = e.getPlugin();
        String[] cmds = cmdConfig.getStringList(p.getName(), null);
        if (cmds != null)
            for (String s : cmds)
            {
                PluginCommand cmd = Bukkit.getPluginCommand(s);
                if (cmd != null)
                    con.changePriority(p, cmd, true);
            }
    }
}
