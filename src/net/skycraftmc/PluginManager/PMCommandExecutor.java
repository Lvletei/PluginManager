package net.skycraftmc.PluginManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class PMCommandExecutor implements CommandExecutor
{

    private class CmdDesc
    {

        private String cmd;
        private String desc;
        private String perm;

        private CmdDesc( String cmd, String desc, String perm )
        {
            this.cmd = cmd;
            this.desc = desc;
            this.perm = perm;
        }

        public String asDef()
        {
            return def(cmd, desc);
        }

        public String getPerm()
        {
            return perm;
        }
    }

    private PluginManagerPlugin pluginMngr;
    private PluginControl       control;

    private Server              server;

    private final CmdDesc[]     help =
                                     { new CmdDesc("plm enable <plugin>", "Enables a plugin", "pluginmanager.enable"),
        new CmdDesc("plm disable <plugin>", "Disables a plugin", "pluginmanager.disable"),
        new CmdDesc("plm load <plugin>", "Loads a plugin(Must use a file name, no .jar needed)", "pluginmanager.load"), new CmdDesc("plm unload <plugin>", "Unloads a plugin", "pluginmanager.unload"),
        new CmdDesc("plm reload <plugin>", "Unloads and loads a plugin", "pluginmanager.reload"), new CmdDesc("plm sreload <plugin>", "Disables and enables a plugin", "pluginmanager.softreload"),
        new CmdDesc("plm show <plugin>", "Shows detailed information about a plugin", "pluginmanager.show"),
        new CmdDesc("plm list [options]", "Lists plugins with specified options, use -option to show options", "pluginmanager.list"), new CmdDesc("plm cmd", "Shows command manipulation menu", null) };

    PMCommandExecutor( PluginManagerPlugin plugin, PluginControl control )
    {
        this.pluginMngr = plugin;
        this.control = control;
        server = Bukkit.getServer();
    }

    public boolean cmdCmd( CommandSender sender, String[] args )
    {
        if (args.length == 1)
        {
            sender.sendMessage(def("/plm cmd unregister <plugin> <command>", "Unregisters a command"));
            sender.sendMessage(def("/plm cmd priority <plugin> <command>", "Sets priority of specified command to highest"));

        }
        else if (args[1].equalsIgnoreCase("unregister"))
        {
            if (noPerm(sender, "pluginmanager.cmd.unregister")) return true;

            if (args.length != 4)
            {
                sender.sendMessage(ChatColor.RED + "Usage: /plm cmd unregister <plugin> <command>");
                return true;
            }

            Plugin plugin = server.getPluginManager().getPlugin(args[2]);
            if (plugin == null)
            {
                sender.sendMessage(ChatColor.RED + "No such plugin: " + args[2]);
                return true;
            }

            if (!control.hasCommand((JavaPlugin) plugin, args[3]))
            {
                sender.sendMessage(ChatColor.RED + plugin.getDescription().getName() + " doesn't have the command " + args[3] + "!");
                return true;
            }
            control.unregisterCommand((JavaPlugin) plugin, args[3]);
            sender.sendMessage(ChatColor.GREEN + args[3] + " unregistered!");

        }
        else if (args[1].equalsIgnoreCase("priority"))
        {
            if (noPerm(sender, "pluginmanager.cmd.priority")) return true;

            if (args.length != 4)
            {
                sender.sendMessage(ChatColor.RED + "Usage: /plm cmd priority <plugin> <command>");
                return true;
            }

            JavaPlugin plugin = (JavaPlugin) server.getPluginManager().getPlugin(args[2]);
            if (plugin == null)
            {
                sender.sendMessage(ChatColor.RED + "No such plugin: " + args[2]);
                return true;
            }

            if (!control.hasCommand(plugin, args[3]))
            {
                sender.sendMessage(ChatColor.RED + plugin.getDescription().getName() + " doesn't have the command " + args[3] + "!");
                return true;
            }

            if (control.isTopPriority(plugin, args[3]))
            {
                sender.sendMessage(ChatColor.RED + args[3] + " command of " + plugin.getDescription().getName() + " is already top priority!");
                return true;
            }

            PluginCommand pcmd = control.getCommand(plugin, args[3]);
            control.changePriority(plugin, pcmd);
            sender.sendMessage(ChatColor.RED + "Priority of " + plugin.getDescription().getName() + "'s " + pcmd.getName() + " command set to highest!");
        }
        return true;
    }

    private String def( String cmd, String desc )
    {
        return ChatColor.GOLD + cmd + ChatColor.AQUA + " - " + desc;
    }

    private boolean disableCmd( CommandSender sender, String[] args )
    {
        if (!sender.hasPermission("pluginmanager.disable") && sender != server.getConsoleSender())
        {
            noPerm(sender);
            return true;
        }

        if (args.length != 2) return usage(sender, "plm disable <plugin>");

        Plugin plugin = server.getPluginManager().getPlugin(args[1]);
        if (plugin == null)
            sender.sendMessage(ChatColor.RED + "No such plugin: " + args[1]);

        else if (!plugin.isEnabled())
            sender.sendMessage(ChatColor.RED + args[1] + " is already disabled!");

        else
        {
            control.disablePlugin(plugin);
            sender.sendMessage(ChatColor.GREEN + args[1] + " disabled!");
        }
        return true;
    }

    private boolean enableCmd( CommandSender sender, String[] args )
    {
        if (!sender.hasPermission("pluginmanager.enable") && sender != server.getConsoleSender())
        {
            noPerm(sender);
            return true;
        }
        if (args.length != 2) return usage(sender, "plm enable <plugin>");

        Plugin plugin = server.getPluginManager().getPlugin(args[1]);
        if (plugin == null)
            sender.sendMessage(ChatColor.RED + "No such plugin: " + args[1]);

        else if (plugin.isEnabled())
            sender.sendMessage(ChatColor.RED + args[1] + " is already enabled!");

        else
        {
            control.enablePlugin(plugin);
            sender.sendMessage(ChatColor.GREEN + args[1] + " enabled!");
        }
        return true;
    }

    private boolean helpCmd( CommandSender sender, String[] args, String title, CmdDesc[] help )
    {
        int page = 1;
        if (args.length == 2) try
        {
            page = Integer.parseInt(args[1]);
        } catch ( NumberFormatException nfe )
        {
            return msg(sender, ChatColor.RED + "\"" + args[1] + "\" is not a valid number");
        }

        ArrayList<String> d = new ArrayList<String>();
        int max = 1;
        int cmda = 0;
        for (int i = 0; i < help.length; i++)
        {
            CmdDesc c = help[i];
            if (c.getPerm() != null) if (!sender.hasPermission(c.getPerm()) && sender != server.getConsoleSender()) continue;

            if (d.size() < 10) if (i >= ( page - 1 ) * 10 && i <= ( page - 1 ) * 10 + 9) d.add(( sender instanceof Player ? "/" : "" ) + c.asDef());

            if (cmda > 10 && cmda % 10 == 1) max++;

            cmda++;
        }

        sender.sendMessage(ChatColor.GOLD + title + " Help (" + ChatColor.AQUA + page + ChatColor.GOLD + "/" + ChatColor.AQUA + max + ChatColor.GOLD + ")");
        for (String s : d)
            sender.sendMessage(s);

        return true;
    }

    private boolean listCmd( CommandSender sender, String[] args )
    {
        if (!sender.hasPermission("pluginmanager.list") && sender != server.getConsoleSender())
        {
            noPerm(sender);
            return true;
        }

        boolean versions = false;
        boolean options = false;
        boolean alphabetical = false;
        String search = "";

        for (int i = 1; i < args.length; i++)
        {
            String s = args[i];
            if (s.equalsIgnoreCase("-v") || s.equalsIgnoreCase("-version"))
                versions = true;
            else if (s.equalsIgnoreCase("-options") || s.equalsIgnoreCase("-o"))
                options = true;
            else if (s.equalsIgnoreCase("-alphabetical") || s.equalsIgnoreCase("-a"))
                alphabetical = true;
            else if (s.startsWith("-s:") || s.startsWith("-search:"))
            {
                String[] t = s.split("[:]", 2);
                if (t.length != 2) continue;
                search = t[1];
            }
        }

        if (options)
        {
            sender.sendMessage(ChatColor.YELLOW + "List options");
            sender.sendMessage(ChatColor.YELLOW + "-v" + ChatColor.GOLD + " - Shows plugins with versions");
            sender.sendMessage(ChatColor.YELLOW + "-o" + ChatColor.GOLD + " - Lists options");
            sender.sendMessage(ChatColor.YELLOW + "-a" + ChatColor.GOLD + " - Lists plugins in alphabetical order");
            sender.sendMessage(ChatColor.YELLOW + "-s:[plugin]" + ChatColor.GOLD + " - List plugins that only contain the name");
            return true;
        }

        Plugin[] pl = server.getPluginManager().getPlugins();
        String pes = "";
        String pds = "";

        java.util.ArrayList<Plugin> plugins = new java.util.ArrayList<Plugin>(java.util.Arrays.asList(pl));
        if (!search.isEmpty())
        {
            java.util.Iterator<Plugin> it = plugins.iterator();
            while (it.hasNext())
            {
                Plugin p = it.next();
                if (!p.getName().contains(search)) it.remove();
            }
        }

        if (alphabetical)
        {
            java.util.ArrayList<String> s = new java.util.ArrayList<String>();
            for (Plugin p : plugins)
                s.add(p.getName());
            java.util.Collections.sort(s);
            plugins = new java.util.ArrayList<Plugin>();
            for (String a : s)
                plugins.add(server.getPluginManager().getPlugin(a));
        }

        for (Plugin p : plugins)
        {
            String l = p.getName();
            if (versions) l = l + " " + p.getDescription().getVersion();
            if (p.isEnabled())
            {
                if (pes.isEmpty())
                    pes = l;
                else
                    pes = pes + ", " + l;
            }
            else if (pds.isEmpty())
                pds = l;
            else
                pds = pds + ", " + l;
        }

        if (!pes.isEmpty()) sender.sendMessage(ChatColor.YELLOW + "Enabled plugins: " + ChatColor.GREEN + pes);
        if (!pds.isEmpty()) sender.sendMessage(ChatColor.YELLOW + "Disabled plugins: " + ChatColor.RED + pds);
        return true;
    }

    private boolean loadCmd( CommandSender sender, String[] args )
    {
        if (!sender.hasPermission("pluginmanager.load") && sender != server.getConsoleSender())
        {
            noPerm(sender);
            return true;
        }

        if (args.length <= 1) return usage(sender, "plm load <plugin>");

        String fname = "";
        for (int i = 1; i < args.length; i++)
            if (fname.isEmpty())
                fname = fname + args[i];
            else
                fname = fname + " " + args[i];

        File f = new File("plugins" + File.separator + fname + ".jar");
        if (!f.exists())
        {
            sender.sendMessage(ChatColor.RED + "No such file: " + fname + ".jar");
            return true;
        }

        PluginDescriptionFile pdf = control.getDescriptionFromJar(f);
        if (pdf == null)
        {
            sender.sendMessage(ChatColor.RED + "Jar file doesn't contain a plugin.yml: " + f.getName());
            return true;
        }

        if (server.getPluginManager().getPlugin(pdf.getName()) != null)
        {
            sender.sendMessage(ChatColor.RED + pdf.getName() + " is already loaded!");
            return true;
        }

        Plugin p = null;
        if (( p = control.loadPlugin(fname) ) != null)
        {
            server.getPluginManager().enablePlugin(p);
            sender.sendMessage(ChatColor.GREEN + p.getDescription().getName() + " " + p.getDescription().getVersion() + " loaded successfully!");
        }
        else
            sender.sendMessage(ChatColor.RED + "Failed to load " + args[1] + "!" + ( sender instanceof org.bukkit.entity.Player ? "Check console for details!" : "" ));

        return true;
    }

    private boolean msg( CommandSender sender, String msg )
    {
        sender.sendMessage(msg);
        return true;
    }

    public void noPerm( CommandSender sender )
    {
        sender.sendMessage(ChatColor.RED + "You are not allowed to use this command!");
    }

    public boolean noPerm( CommandSender sender, String perm )
    {
        if (!sender.hasPermission(perm) && sender != server.getConsoleSender())
        {
            noPerm(sender);
            return true;
        }
        return false;
    }

    @Override
    public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
    {
        if (args.length >= 1)
        {
            if (args[0].equalsIgnoreCase("list"))
                return listCmd(sender, args);
            else if (args[0].equalsIgnoreCase("enable"))
                return enableCmd(sender, args);
            else if (args[0].equalsIgnoreCase("disable"))
                return disableCmd(sender, args);
            else if (args[0].equalsIgnoreCase("load"))
                return loadCmd(sender, args);
            else if (args[0].equalsIgnoreCase("unload"))
                return unloadCmd(sender, args);
            else if (args[0].equalsIgnoreCase("reload"))
                return reloadCmd(sender, args);
            else if (args[0].equalsIgnoreCase("sreload") || args[0].equalsIgnoreCase("softreload"))
                return sreloadCmd(sender, args);
            else if (args[0].equalsIgnoreCase("show"))
                return showCmd(sender, args);
            else if (args[0].equalsIgnoreCase("cmd"))
                return cmdCmd(sender, args);
            else
                return msg(sender, ChatColor.GOLD + "Command unrecognized.  Type " + ChatColor.AQUA + "/plm" + ChatColor.GOLD + " for help");

        }
        else
            helpCmd(sender, args, "PluginManager", help);
        return true;
    }

    private boolean reloadCmd( CommandSender sender, String[] args )
    {
        if (!sender.hasPermission("pluginmanager.reload") && sender != server.getConsoleSender())
        {
            noPerm(sender);
            return true;
        }

        if (args.length != 2) return usage(sender, "plm reload <plugin>");

        Plugin plugin = server.getPluginManager().getPlugin(args[1]);
        if (plugin == null)
            sender.sendMessage(ChatColor.RED + "No such plugin: " + args[1]);
        else
        {
            File file = control.getFile((JavaPlugin) plugin);
            JavaPlugin loaded = null;
            if (file == null)
            {
                sender.sendMessage(ChatColor.RED + plugin.getName() + "'s jar file is missing!");
                return true;
            }

            String fname = file.getName().substring(0, file.getName().length() - 4);
            boolean t = plugin == this;
            pluginMngr.setUnload(t);

            if (!control.unloadPlugin(plugin))
                sender.sendMessage(ChatColor.RED + "An error occurred while unloading " + args[1] + "!");
            else if (( loaded = (JavaPlugin) control.loadPlugin(fname) ) == null)
                sender.sendMessage(ChatColor.RED + "Failed to load " + fname + "!" + ( sender != server.getConsoleSender() ? "Check console for details!" : "" ));

            server.getPluginManager().enablePlugin(loaded);
            sender.sendMessage(ChatColor.GREEN + loaded.getDescription().getName() + " reloaded successfully.");
            if (t) control.cleanup();
        }

        return true;
    }

    private boolean searchDBOCmd( CommandSender sender, String[] args )
    {
        if (!sender.hasPermission("pluginmanager.searchdbo") && sender != server.getConsoleSender())
        {
            noPerm(sender);
            return true;
        }

        if (!( args.length > 2 )) return usage(sender, "plm plug-get find <slug>");

        List<SlugInformation> slugInfo = DBOUtilities.getSlugInformationList(args[1]);

        if (slugInfo.size() == 0)
            sender.sendMessage(ChatColor.RED + String.format("Nothing found for '%s'!", args[1]));
        else
        {
            sender.sendMessage(ChatColor.AQUA + String.format("|----DBO slug information: %s----|", ChatColor.GREEN + args[1] + ChatColor.AQUA));
            sender.sendMessage(ChatColor.GOLD + "|----Format: plugin name : slug----|");

            for (SlugInformation si : slugInfo)
                sender.sendMessage(ChatColor.GOLD + String.format("%1$s : %2$s", si.getPluginName(), si.getSlug()));
        }

        return true;
    }

    private boolean showCmd( CommandSender sender, String[] args )
    {
        if (!sender.hasPermission("pluginmanager.show") && sender != server.getConsoleSender())
        {
            noPerm(sender);
            return true;
        }

        if (args.length != 2) return usage(sender, "plm show <plugin>");

        Plugin plugin = server.getPluginManager().getPlugin(args[1]);
        if (plugin == null)
            sender.sendMessage(ChatColor.RED + "No such plugin: " + args[1]);
        else
        {
            File file = control.getFile((JavaPlugin) plugin);
            sender.sendMessage(ChatColor.AQUA + "|----Plugin information: " + ChatColor.GREEN + plugin.getName() + ChatColor.AQUA + "----|");
            sender.sendMessage(ChatColor.GOLD + "Status: " + ( plugin.isEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled" ));
            if (plugin.getDescription().getDescription() != null) sender.sendMessage(ChatColor.AQUA + "Description: " + ChatColor.GREEN + plugin.getDescription().getDescription());

            sender.sendMessage(ChatColor.AQUA + "Version: " + ChatColor.GREEN + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.AQUA + "Main class: " + ChatColor.GREEN + plugin.getDescription().getMain());
            sender.sendMessage(ChatColor.AQUA + "Jar file: " + ChatColor.GREEN + file.getName());
            String authors = null;

            if (plugin.getDescription().getAuthors() != null) if (!plugin.getDescription().getAuthors().isEmpty()) for (String a : plugin.getDescription().getAuthors())
                if (authors == null)
                    authors = a;
                else
                    authors = authors + ", " + a;

            if (authors != null) sender.sendMessage(ChatColor.AQUA + ( plugin.getDescription().getAuthors().size() == 1 ? "Author: " : "Authors: " ) + ChatColor.GREEN + authors);

            if (plugin.getDescription().getWebsite() != null) sender.sendMessage(ChatColor.AQUA + "Website: " + ChatColor.GREEN + plugin.getDescription().getWebsite());
        }
        return true;
    }

    private boolean sreloadCmd( CommandSender sender, String[] args )
    {
        if (!sender.hasPermission("pluginmanager.softreload") && sender != server.getConsoleSender())
        {
            noPerm(sender);
            return true;
        }

        if (args.length != 2) return usage(sender, "plm sreload <plugin>");

        Plugin plugin = server.getPluginManager().getPlugin(args[1]);
        if (plugin == null)
            sender.sendMessage(ChatColor.RED + "No such plugin: " + args[1]);
        else if (!plugin.isEnabled())
            sender.sendMessage(ChatColor.RED + "The plugin is disabled!");
        else
        {
            server.getPluginManager().disablePlugin(plugin);
            server.getPluginManager().enablePlugin(plugin);
            sender.sendMessage(ChatColor.GREEN + plugin.getDescription().getName() + " " + plugin.getDescription().getVersion() + " soft reloaded successfully.");
        }

        return true;
    }

    private boolean unloadCmd( CommandSender sender, String[] args )
    {
        if (!sender.hasPermission("pluginmanager.unload") && sender != server.getConsoleSender())
        {
            noPerm(sender);
            return true;
        }

        if (args.length != 2) return usage(sender, "plm unload <plugin>");

        Plugin plugin = server.getPluginManager().getPlugin(args[1]);
        if (plugin == null)
            sender.sendMessage(ChatColor.RED + "No such plugin: " + args[1]);
        else
        {
            boolean t = plugin == this;
            pluginMngr.setUnload(t);
            if (control.unloadPlugin(plugin))
                sender.sendMessage(ChatColor.GREEN + args[1] + " " + plugin.getDescription().getVersion() + " successfully unloaded!");
            else
                sender.sendMessage(ChatColor.RED + "Failed to unload " + args[1] + "!" + ( sender instanceof org.bukkit.entity.Player ? "Check console for details!" : "" ));
            if (t) control.cleanup();
        }

        return true;
    }

    private boolean usage( CommandSender sender, String usage )
    {
        sender.sendMessage(ChatColor.RED + "Usage: " + ( sender instanceof Player ? "/" : "" ) + usage);
        return true;
    }
}
