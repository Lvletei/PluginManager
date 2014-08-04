package co.technius.PluginManager;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
import org.bukkit.scheduler.BukkitRunnable;

import co.technius.PluginManager.DBOUtilities.VersionInfo;
import co.technius.PluginManager.DBOUtilities.VersionInformation;

public class PMCommandExecutor implements CommandExecutor {

    private class CmdDesc {

        private final String cmd;
        private final String desc;
        private final String perm;

        private CmdDesc(final String cmd, final String desc, final String perm) {
            this.cmd = cmd;
            this.desc = desc;
            this.perm = perm;
        }

        public String asDef() {
            return def(cmd, desc);
        }

        public String getPerm() {
            return perm;
        }
    }

    private final PluginManagerPlugin pluginMngr;
    private final PluginControl control;
    private final Server server;

    private final CmdDesc[] help = {
            new CmdDesc("plm enable <plugin>", "Enables a plugin", "pluginmanager.enable"),
            new CmdDesc("plm disable <plugin>", "Disables a plugin", "pluginmanager.disable"),
            new CmdDesc("plm load <plugin>", "Loads a plugin(Must use a file name, no .jar needed)", "pluginmanager.load"),
            new CmdDesc("plm unload <plugin>", "Unloads a plugin", "pluginmanager.unload"),
            new CmdDesc("plm reload <plugin>", "Unloads and loads a plugin", "pluginmanager.reload"),
            new CmdDesc("plm sreload <plugin>", "Disables and enables a plugin", "pluginmanager.softreload"),
            new CmdDesc("plm show <plugin>", "Shows detailed information about a plugin", "pluginmanager.show"),
            new CmdDesc("plm list [options]", "Lists plugins with specified options", "pluginmanager.list"),
            new CmdDesc("plm cmd", "Shows command manipulation menu", null),
            new CmdDesc("plm plug-get", "Shows BukkitDev lookup menu", null) };

    private final CmdDesc[] pluggethelp = {
            new CmdDesc("plm plug-get search <name>", "Searches for a plugin on BukkitDev", "pluginmanager.plugget.search"),
            new CmdDesc("plm plug-get check <slug>", "Checks if an update is available", "pluginmanager.plugget.check") };

    private final CmdDesc[] cmdhelp = {
            new CmdDesc("plm cmd unregister <command> <plugin>", "Unregisters a command", "pluginmanager.cmd.unregister"),
            new CmdDesc("plm cmd priority <command> <plugin>", "Elevates command priority to highest", "pluginmanager.cmd.priority") };

    PMCommandExecutor(final PluginManagerPlugin plugin, final PluginControl control) {
        this.pluginMngr = plugin;
        this.control = control;
        server = Bukkit.getServer();
    }

    public boolean cmdCmd(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            return helpCmd(sender, args, "Command Help", cmdhelp);
        } else if (args[1].equalsIgnoreCase("unregister")) {
            if (noPerm(sender, "pluginmanager.cmd.unregister")) {
                return true;
            }

            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage: /plm cmd unregister <command> <plugin>");
                return true;
            }

            final String pName = StringUtils.getStringOfArray(args, 3);
            final Plugin plugin = server.getPluginManager().getPlugin(pName);
            if (plugin == null) {
                sender.sendMessage(ChatColor.RED + "No such plugin: " + pName);
                return true;
            }

            if (!control.hasCommand((JavaPlugin) plugin, args[2])) {
                sender.sendMessage(ChatColor.RED + plugin.getDescription().getName()
                        + " doesn't have the command " + args[2] + "!");
                return true;
            }
            control.unregisterCommand((JavaPlugin) plugin, args[2]);
            sender.sendMessage(ChatColor.GREEN + args[2] + " unregistered!");
        } else if (args[1].equalsIgnoreCase("priority")) {
            if (noPerm(sender, "pluginmanager.cmd.priority")) {
                return true;
            }

            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage: /plm cmd priority <command> <plugin>");
                return true;
            }

            final String pName = StringUtils.getStringOfArray(args, 3);
            final JavaPlugin plugin = (JavaPlugin) server.getPluginManager().getPlugin(pName);
            if (plugin == null) {
                sender.sendMessage(ChatColor.RED + "No such plugin: " + pName);
                return true;
            }

            if (!control.hasCommand(plugin, args[2])) {
                sender.sendMessage(ChatColor.RED + plugin.getDescription().getName()
                        + " doesn't have the command " + args[2] + "!");
                return true;
            }

            if (control.isTopPriority(plugin, args[2])) {
                sender.sendMessage(ChatColor.RED + args[2] + " command of "
                        + plugin.getDescription().getName() + " is already top priority!");
                return true;
            }

            final PluginCommand pcmd = control.getCommand(plugin, args[2]);
            control.changePriority(plugin, pcmd, false);
            sender.sendMessage(ChatColor.GREEN + "Priority of " + plugin.getDescription().getName()
                    + "'s " + pcmd.getName() + " command set to highest!");
        } else {
            return msg(sender, ChatColor.GOLD + "Command unrecognized.  Type " + ChatColor.AQUA + "/plm cmd" + ChatColor.GOLD + " for help");
        }
        return true;
    }

    public void noPerm(final CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "You are not allowed to use this command!");
    }

    public boolean noPerm(final CommandSender sender, final String perm) {
        if (!sender.hasPermission(perm) && sender != server.getConsoleSender()) {
            noPerm(sender);
            return true;
        }
        return false;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length >= 1) {
            switch (args[0].toLowerCase()) {
            case "list":
                return listCmd(sender, args);
            case "enable":
                return enableCmd(sender, args);
            case "disable":
                return disableCmd(sender, args);
            case "load":
                return loadCmd(sender, args);
            case "unload":
                return unloadCmd(sender, args);
            case "reload":
                return reloadCmd(sender, args);
            case "sreload":
            case "softreload":
                return sreloadCmd(sender, args);
            case "show":
                return showCmd(sender, args);
            case "cmd":
                return cmdCmd(sender, args);
            case "plug-get":
                return plugGetCmd(sender, args);
            default:
                return msg(sender, ChatColor.GOLD + "Command unrecognized.  Type "
                        + ChatColor.AQUA + "/plm" + ChatColor.GOLD + " for help");
            }
        } else {
            helpCmd(sender, args, "PluginManager", help);
        }
        return true;
    }

    private String def(final String cmd, final String desc) {
        return ChatColor.GOLD + cmd + ChatColor.AQUA + " - " + desc;
    }

    private boolean disableCmd(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("pluginmanager.disable") && sender != server.getConsoleSender()) {
            noPerm(sender);
            return true;
        }

        if (args.length < 2) {
            return usage(sender, "plm disable <plugin>");
        }

        final String pName = StringUtils.getStringOfArray(args, 1);
        final Plugin plugin = server.getPluginManager().getPlugin(pName);
        if (plugin == null) {
            sender.sendMessage(ChatColor.RED + "No such plugin: " + pName);
        } else if (!plugin.isEnabled()) {
            sender.sendMessage(ChatColor.RED + pName + " is already disabled!");
        } else {
            control.disablePlugin(plugin);
            sender.sendMessage(ChatColor.GREEN + pName + " disabled!");
        }
        return true;
    }

    private boolean enableCmd(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("pluginmanager.enable") && sender != server.getConsoleSender()) {
            noPerm(sender);
            return true;
        }
        if (args.length < 2) {
            return usage(sender, "plm enable <plugin>");
        }

        final String pName = StringUtils.getStringOfArray(args, 1);
        final Plugin plugin = server.getPluginManager().getPlugin(pName);
        if (plugin == null) {
            sender.sendMessage(ChatColor.RED + "No such plugin: " + pName);
        } else if (plugin.isEnabled()) {
            sender.sendMessage(ChatColor.RED + pName + " is already enabled!");
        } else {
            control.enablePlugin(plugin);
            sender.sendMessage(ChatColor.GREEN + pName + " enabled!");
        }
        return true;
    }

    private boolean helpCmd(final CommandSender sender, final String[] args, final String title, final CmdDesc[] help) {
        int page = 1;
        if (args.length == 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (final NumberFormatException nfe) {
                return msg(sender, ChatColor.RED + "\"" + args[1] + "\" is not a valid number");
            }
        }

        final ArrayList<String> d = new ArrayList<String>();
        int max = 1;
        int cmda = 0;
        for (int i = 0; i < help.length; i++) {
            final CmdDesc c = help[i];
            if (c.getPerm() != null) {
                if (!sender.hasPermission(c.getPerm()) && sender != server.getConsoleSender()) {
                    continue;
                }
            }

            if (d.size() < 10) {
                if (i >= (page - 1) * 10 && i <= (page - 1) * 10 + 9) {
                    d.add((sender instanceof Player ? "/" : "") + c.asDef());
                }
            }

            if (cmda > 10 && cmda % 10 == 1) {
                max++;
            }

            cmda++;
        }

        sender.sendMessage(ChatColor.GOLD + title + " Help (" + ChatColor.AQUA + page
                + ChatColor.GOLD + "/" + ChatColor.AQUA + max + ChatColor.GOLD + ")");
        for (final String s : d) {
            sender.sendMessage(s);
        }

        return true;
    }

    private boolean listCmd(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("pluginmanager.list") && sender != server.getConsoleSender()) {
            noPerm(sender);
            return true;
        }

        boolean versions = false;
        boolean options = false;
        boolean alphabetical = false;
        String search = "";

        for (int i = 1; i < args.length; i++) {
            final String s = args[i];
            if (s.equalsIgnoreCase("-v") || s.equalsIgnoreCase("-version")) {
                versions = true;
            } else if (s.equalsIgnoreCase("-options") || s.equalsIgnoreCase("-o")) {
                options = true;
            } else if (s.equalsIgnoreCase("-alphabetical") || s.equalsIgnoreCase("-a")) {
                alphabetical = true;
            } else if (s.startsWith("-s:") || s.startsWith("-search:")) {
                final String[] t = s.split("[:]", 2);
                if (t.length != 2) {
                    continue;
                }
                search = t[1];
            }
        }

        if (options) {
            sender.sendMessage(ChatColor.YELLOW + "List options");
            sender.sendMessage(ChatColor.YELLOW + "-v" + ChatColor.GOLD + " - Shows plugins with versions");
            sender.sendMessage(ChatColor.YELLOW + "-o" + ChatColor.GOLD + " - Lists options");
            sender.sendMessage(ChatColor.YELLOW + "-a" + ChatColor.GOLD + " - Lists plugins in alphabetical order");
            sender.sendMessage(ChatColor.YELLOW + "-s:[plugin]" + ChatColor.GOLD + " - List plugins that only contain the name");
            return true;
        }

        final Plugin[] pl = server.getPluginManager().getPlugins();
        String pes = "";
        String pds = "";

        java.util.ArrayList<Plugin> plugins = new java.util.ArrayList<Plugin>(
                java.util.Arrays.asList(pl));
        if (!search.isEmpty()) {
            final java.util.Iterator<Plugin> it = plugins.iterator();
            while (it.hasNext()) {
                final Plugin p = it.next();
                if (!p.getName().contains(search)) {
                    it.remove();
                }
            }
        }

        if (alphabetical) {
            final java.util.ArrayList<String> s = new java.util.ArrayList<String>();
            for (final Plugin p : plugins) {
                s.add(p.getName());
            }
            java.util.Collections.sort(s);
            plugins = new java.util.ArrayList<Plugin>();
            for (final String a : s) {
                plugins.add(server.getPluginManager().getPlugin(a));
            }
        }

        for (final Plugin p : plugins) {
            final String l = p.getName() + (versions ? " " + p.getDescription().getVersion() : "");
            if (p.isEnabled()) {
                if (pes.isEmpty()) {
                    pes = l;
                } else {
                    pes = pes + ", " + l;
                }
            } else if (pds.isEmpty()) {
                pds = l;
            } else {
                pds = pds + ", " + l;
            }
        }

        if (!pes.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Enabled plugins: " + ChatColor.GREEN + pes);
        }
        if (!pds.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Disabled plugins: " + ChatColor.RED + pds);
        }

        return true;
    }

    private boolean loadCmd(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("pluginmanager.load") && sender != server.getConsoleSender()) {
            noPerm(sender);
            return true;
        }

        if (args.length < 2) {
            return usage(sender, "plm load <plugin>");
        }

        final String fname = StringUtils.getStringOfArray(args, 1);
        final File f = new File("plugins" + File.separator + fname + ".jar");
        if (!f.exists()) {
            sender.sendMessage(ChatColor.RED + "No such file: " + fname + ".jar");
            return true;
        }

        final PluginDescriptionFile pdf = control.getDescriptionFromJar(f);
        if (pdf == null) {
            sender.sendMessage(ChatColor.RED + "Jar file doesn't contain a plugin.yml: " + f.getName());
            return true;
        }

        if (server.getPluginManager().getPlugin(pdf.getName()) != null) {
            sender.sendMessage(ChatColor.RED + pdf.getName() + " is already loaded!");
            return true;
        }

        Plugin p = null;
        if ((p = control.loadPlugin(fname)) != null) {
            server.getPluginManager().enablePlugin(p);
            sender.sendMessage(ChatColor.GREEN + p.getDescription().getName() + " " + p.getDescription().getVersion() + " loaded successfully!");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to load " + args[1] + "!" + (sender instanceof org.bukkit.entity.Player ? "Check console for details!" : ""));
        }

        return true;
    }

    private boolean msg(final CommandSender sender, final String msg)
    {
        sender.sendMessage(msg);
        return true;
    }

    private boolean plugGetCheckCmd(final CommandSender sender, final String[] args) {
        if (noPerm(sender, "pluginmanager.plugget.check")) {
            return true;
        }

        if (args.length < 3) {
            return usage(sender, "plm plug-get check <slug>");
        }

        new BukkitRunnable() {

            @Override
            public void run() {
                if (args[2].equalsIgnoreCase("pluginmanager")) {
                    sender.sendMessage(ChatColor.RED + "Are you sure you have the right plugin?"
                            + " This plugin's slug is pm-pluginmanager.");
                }
                try {
                    final VersionInformation ver = DBOUtilities.getLatestVersion(args[2].toLowerCase());
                    if (ver == null) {
                        sender.sendMessage(ChatColor.RED + "\"" + args[2] + "\" does not exist!");
                    } else if (ver.version == null) {
                        sender.sendMessage(ChatColor.YELLOW + "No version of the plugin exists on BukkitDev yet.");
                    } else {
                        final VersionInfo info = DBOUtilities.isUpToDate(Bukkit.getPluginManager().getPlugin(ver.pluginname), ver);
                        switch (info) {
                        case LATEST:
                            sender.sendMessage(ChatColor.GREEN + "You are using the latest version of '" + ver.pluginname + "'.");
                            break;

                        case OLD:
                            sender.sendMessage(ChatColor.GREEN + "There is a newer version of '" + ver.pluginname + "' available: " + ver.version + "");
                            break;

                        case NOT_IN_USE:
                            sender.sendMessage(ChatColor.RED + "You are not using '" + ver.pluginname + "' on your server!");
                            break;

                        case UNKNOWN:
                            sender.sendMessage(ChatColor.RED + "The '" + ver.version + "'version of '" + ver.pluginname + "' is"
                                    + " abnormal; please check the version manually on BukkitDev.");
                            break;

                        default:
                            sender.sendMessage(ChatColor.RED + "Failed to check for updates for " + args[2] + "!" + (sender instanceof Player ? " Check console for details!" : ""));
                            break;

                        }
                    }

                } catch (final MalformedURLException e) {
                    sender.sendMessage(ChatColor.RED + "Failed to check for updates for " + args[2] + "!" + (sender instanceof Player ? " Check console for details!" : ""));
                    e.printStackTrace();
                } catch (final IOException e) {
                    sender.sendMessage(ChatColor.RED + "Failed to check for updates for " + args[2] + "!" + (sender instanceof Player ? " Check console for details!" : ""));
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(pluginMngr);

        return true;
    }

    private boolean plugGetCmd(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            return helpCmd(sender, args, "Updater Help", pluggethelp);
        } else {
            if (args[1].equalsIgnoreCase("search")) {
                plugGetSearchCmd(sender, args);
            } else if (args[1].equalsIgnoreCase("check")) {
                plugGetCheckCmd(sender, args);
            } else {
                return msg(sender, ChatColor.GOLD + "Command unrecognized.  Type " + ChatColor.AQUA + "/plm plug-get" + ChatColor.GOLD + " for help");
            }
        }
        return true;
    }

    private boolean plugGetSearchCmd(final CommandSender sender, final String[] args) {
        if (noPerm(sender, "pluginmanager.plugget.search")) {
            return true;
        }

        if (args.length < 3) {
            return usage(sender, "plm plug-get search <slug>");
        }

        new BukkitRunnable() {

            @Override
            public void run() {
                final List<SlugInformation> slugInfo = DBOUtilities.getSlugInformationList(args[2]);

                if (slugInfo.size() == 0) {
                    sender.sendMessage(ChatColor.RED + "No results found for " + args[2] + "!");
                } else {
                    sender.sendMessage(ChatColor.AQUA + String.format("|----BukkitDev Plugin Search: %s----|", ChatColor.GREEN + args[2] + ChatColor.AQUA));
                    sender.sendMessage(ChatColor.GOLD + "|----Plugin name: DBO Slug----|");

                    for (final SlugInformation si : slugInfo) {
                        sender.sendMessage(ChatColor.GOLD + String.format("%1$s: %2$s", si.getPluginName(), si.getSlug()));
                    }
                }
            }

        }.runTaskAsynchronously(pluginMngr);

        return true;
    }

    private boolean reloadCmd(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("pluginmanager.reload") && sender != server.getConsoleSender()) {
            noPerm(sender);
            return true;
        }

        if (args.length < 2) {
            return usage(sender, "plm reload <plugin>");
        }

        final String pName = StringUtils.getStringOfArray(args, 1);
        final Plugin plugin = server.getPluginManager().getPlugin(pName);
        if (plugin == null) {
            sender.sendMessage(ChatColor.RED + "No such plugin: " + pName);
        } else {
            final File file = control.getFile((JavaPlugin) plugin);
            JavaPlugin loaded = null;
            if (file == null) {
                sender.sendMessage(ChatColor.RED + plugin.getName() + "'s jar file is missing!");
                return true;
            }

            final String fname = file.getName().substring(0, file.getName().length() - 4);
            // boolean t = plugin == this; I don't think that this is right
            final boolean t = plugin.equals(pluginMngr);
            pluginMngr.setUnload(t);

            if (!control.unloadPlugin(plugin)) {
                sender.sendMessage(ChatColor.RED + "An error occurred while unloading " + pName + "!");
            } else if ((loaded = (JavaPlugin) control.loadPlugin(fname)) == null) {
                sender.sendMessage(ChatColor.RED + "Failed to load " + fname + "!" + (sender != server.getConsoleSender() ? "Check console for details!" : ""));
            }

            server.getPluginManager().enablePlugin(loaded);
            sender.sendMessage(ChatColor.GREEN + loaded.getDescription().getName() + " reloaded successfully.");

            if (t) {
                control.cleanup();
            }
        }

        return true;
    }

    private boolean showCmd(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("pluginmanager.show") && sender != server.getConsoleSender()) {
            noPerm(sender);
            return true;
        }

        if (args.length < 2) {
            return usage(sender, "plm show <plugin>");
        }

        final String pName = StringUtils.getStringOfArray(args, 1);
        final Plugin plugin = server.getPluginManager().getPlugin(pName);
        if (plugin == null) {
            sender.sendMessage(ChatColor.RED + "No such plugin: " + pName);
        } else {
            final File file = control.getFile((JavaPlugin) plugin);
            sender.sendMessage(ChatColor.AQUA + "|----Plugin information: " + ChatColor.GREEN + plugin.getName() + ChatColor.AQUA + "----|");
            sender.sendMessage(ChatColor.GOLD + "Status: " + (plugin.isEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
            if (plugin.getDescription().getDescription() != null) {
                sender.sendMessage(ChatColor.AQUA + "Description: " + ChatColor.GREEN + plugin.getDescription().getDescription());
            }

            sender.sendMessage(ChatColor.AQUA + "Version: " + ChatColor.GREEN + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.AQUA + "Main class: " + ChatColor.GREEN + plugin.getDescription().getMain());
            sender.sendMessage(ChatColor.AQUA + "Jar file: " + ChatColor.GREEN + file.getName());
            final StringBuffer authors = new StringBuffer();

            if (plugin.getDescription().getAuthors() != null) {
                if (!plugin.getDescription().getAuthors().isEmpty()) {
                    for (final String a : plugin.getDescription().getAuthors()) {
                        if (authors.length() > 0) {
                            authors.append(", ");
                        }

                        authors.append(a);
                    }
                }
            }

            if (authors != null) {
                sender.sendMessage(ChatColor.AQUA + (plugin.getDescription().getAuthors().size() == 1 ? "Author: " : "Authors: ") + ChatColor.GREEN + authors);
            }

            if (plugin.getDescription().getWebsite() != null) {
                sender.sendMessage(ChatColor.AQUA + "Website: " + ChatColor.GREEN + plugin.getDescription().getWebsite());
            }
        }
        return true;
    }

    private boolean sreloadCmd(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("pluginmanager.softreload") && sender != server.getConsoleSender()) {
            noPerm(sender);
            return true;
        }

        if (args.length < 2) {
            return usage(sender, "plm sreload <plugin>");
        }

        final String pName = StringUtils.getStringOfArray(args, 1);
        final Plugin plugin = server.getPluginManager().getPlugin(pName);
        if (plugin == null) {
            sender.sendMessage(ChatColor.RED + "No such plugin: " + pName);
        } else if (!plugin.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "The plugin is disabled!");
        } else {
            server.getPluginManager().disablePlugin(plugin);
            server.getPluginManager().enablePlugin(plugin);
            sender.sendMessage(ChatColor.GREEN + plugin.getDescription().getName() + " "
                    + plugin.getDescription().getVersion() + " soft reloaded successfully.");
        }

        return true;
    }

    private boolean unloadCmd(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("pluginmanager.unload") && sender != server.getConsoleSender()) {
            noPerm(sender);
            return true;
        }

        if (args.length < 2) {
            return usage(sender, "plm unload <plugin>");
        }

        final String pName = StringUtils.getStringOfArray(args, 1);
        final Plugin plugin = server.getPluginManager().getPlugin(pName);
        if (plugin == null) {
            sender.sendMessage(ChatColor.RED + "No such plugin: " + args[1]);
        } else {
            final boolean t = plugin == this;
            pluginMngr.setUnload(t);
            // if (control.unloadPlugin(plugin)) {
            if (control.unloadRecursively(plugin)) {
                sender.sendMessage(ChatColor.GREEN + pName + " " + plugin.getDescription().getVersion() + " successfully unloaded!");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to unload " + pName + "!" + (sender instanceof org.bukkit.entity.Player ? "Check console for details!" : ""));
            }

            if (t) {
                control.cleanup();
            }
        }

        return true;
    }

    private boolean usage(final CommandSender sender, final String usage) {
        sender.sendMessage(ChatColor.RED + "Usage: " + (sender instanceof Player ? "/" : "") + usage);
        return true;
    }
}
