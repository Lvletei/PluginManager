package net.skycraftmc.PluginManager;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginManagerPlugin extends JavaPlugin
{
	PluginControl control;
	private boolean unl = false;
	public void onLoad()
	{
		
	}
	public void onEnable()
	{
		try
		{
			control = new PluginControl();
		}
		catch(Exception e)
		{
			getLogger().severe("Failed to start!");
			setEnabled(false);
			e.printStackTrace();
		}
	}
	public void onDisable()
	{
		if(!unl)control.cleanup();
	}
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if(args.length >= 1)
		{
			if(args[0].equalsIgnoreCase("list"))
			{
				if(!sender.hasPermission("pluginmanager.list") && sender != getServer().getConsoleSender())
				{
					noPerm(sender);
					return true;
				}
				boolean versions = false;
				boolean options = false;
				boolean alphabetical = false;
				String search = "";
				for(int i = 1; i < args.length; i ++)
				{
					String s = args[i];
					if(s.equalsIgnoreCase("-v") || s.equalsIgnoreCase("-version"))versions = true;
					else if(s.equalsIgnoreCase("-options") || s.equalsIgnoreCase("-o"))options = true;
					else if(s.equalsIgnoreCase("-alphabetical") || s.equalsIgnoreCase("-a"))alphabetical = true;
					else if(s.startsWith("-s:") || s.startsWith("-search:"))
					{
						String[] t = s.split("[:]", 2);
						if(t.length != 2)continue;
						search = t[1];
					}
				}
				if(options)
				{
					sender.sendMessage(ChatColor.YELLOW + "List options");
					sender.sendMessage(ChatColor.YELLOW + "-v" + ChatColor.GOLD + " - Shows plugins with versions");
					sender.sendMessage(ChatColor.YELLOW + "-o" + ChatColor.GOLD + " - Lists options");
					sender.sendMessage(ChatColor.YELLOW + "-a" + ChatColor.GOLD + " - Lists plugins in alphabetical order");
					sender.sendMessage(ChatColor.YELLOW + "-s:[plugin]" + ChatColor.GOLD + " - List plugins that only contain the name");
					return true;
				}
				Plugin[] pl = getServer().getPluginManager().getPlugins();
				String pes = "";
				String pds = "";
				java.util.ArrayList<Plugin> plugins = new java.util.ArrayList<Plugin>(java.util.Arrays.asList(pl));
				if(!search.isEmpty())
				{
					java.util.Iterator<Plugin>it = plugins.iterator();
					while(it.hasNext())
					{
						Plugin p = it.next();
						if(!p.getName().contains(search))it.remove();
					}
				}
				if(alphabetical)
				{
					java.util.ArrayList<String>s = new java.util.ArrayList<String>();
					for(Plugin p:plugins)s.add(p.getName());
					java.util.Collections.sort(s);
					plugins = new java.util.ArrayList<Plugin>();
					for(String a:s)plugins.add(getServer().getPluginManager().getPlugin(a));
				}
				for(Plugin p: plugins)
				{
					String l = p.getName();
					if(versions)l = l + " " + p.getDescription().getVersion();
					if(p.isEnabled())
					{
						if(pes.isEmpty())pes = l;
						else pes = pes + ", " + l;
					}
					else
					{
						if(pds.isEmpty())pds = l;
						else pds = pds + ", " + l;
					}
				}
				if(!pes.isEmpty())sender.sendMessage(ChatColor.YELLOW + "Enabled plugins: " + 
						ChatColor.GREEN + pes);
				if(!pds.isEmpty())sender.sendMessage(ChatColor.YELLOW + "Disabled plugins: " + 
						ChatColor.RED + pds);
			}
			else if(args[0].equalsIgnoreCase("cmd"))
			{
				if(args.length == 1)
				{
					sender.sendMessage(def("/plm cmd unregister <plugin> <command>", "Unregisters a command"));
					sender.sendMessage(def("/plm cmd priority <plugin> <command>", "Sets priority of specified command to highest"));
				}
				else if(args[1].equalsIgnoreCase("unregister"))
				{
					if(noPerm(sender, "pluginmanager.cmd.unregister"))return true;
					if(args.length != 4)
					{
						sender.sendMessage(ChatColor.RED + "Usage: /plm cmd unregister <plugin> <command>");
						return true;
					}
					Plugin plugin = getServer().getPluginManager().getPlugin(args[2]);
					if(plugin == null)
					{
						sender.sendMessage(ChatColor.RED + "No such plugin: " + args[2]);
						return true;
					}
					if(!control.hasCommand((JavaPlugin)plugin, args[3]))
					{
						sender.sendMessage(ChatColor.RED + plugin.getDescription().getName() + " doesn't have the command " + args[3] + "!");
						return true;
					}
					control.unregisterCommand((JavaPlugin)plugin, args[3]);
					sender.sendMessage(ChatColor.GREEN + args[3] + " unregistered!");
				}
				else if(args[1].equalsIgnoreCase("priority"))
				{
					if(noPerm(sender, "pluginmanager.cmd.priority"))return true;
					if(args.length != 4)
					{
						sender.sendMessage(ChatColor.RED + "Usage: /plm cmd priority <plugin> <command>");
						return true;
					}
					JavaPlugin plugin = (JavaPlugin) getServer().getPluginManager().getPlugin(args[2]);
					if(plugin == null)
					{
						sender.sendMessage(ChatColor.RED + "No such plugin: " + args[2]);
						return true;
					}
					if(!control.hasCommand(plugin, args[3]))
					{
						sender.sendMessage(ChatColor.RED + plugin.getDescription().getName() + " doesn't have the command " + args[3] + "!");
						return true;
					}
					if(control.isTopPriority(plugin, args[3]))
					{
						sender.sendMessage(ChatColor.RED + args[3] + " command of " + plugin.getDescription().getName() + " is already top priority!");
						return true;
					}
					PluginCommand pcmd = control.getCommand(plugin, args[3]);
					control.changePriority(plugin, pcmd);
					sender.sendMessage(ChatColor.RED + "Priority of " + plugin.getDescription().getName() + "'s " + pcmd.getName() + " command set to highest!");
				}
			}
			else if(args[0].equalsIgnoreCase("load"))
			{
				if(!sender.hasPermission("pluginmanager.load") && sender != getServer().getConsoleSender())
				{
					noPerm(sender);return true;
				}
				if(args.length == 1)
				{
					sender.sendMessage(ChatColor.RED + "Usage: /plm load <plugin>");
					return true;
				}
				String fname = "";
				for(int i = 1; i < args.length; i ++)
				{
					if(fname.isEmpty())fname = fname + args[i];
					else fname = fname + " " + args[i];
				}
				File f = new File("plugins" + File.separator + fname + ".jar");
				if(!f.exists())
				{
					sender.sendMessage(ChatColor.RED + "No such file: " + fname + ".jar");return true;
				}
				PluginDescriptionFile pdf = control.getDescriptionFromJar(f);
				if(pdf == null)
				{
					sender.sendMessage(ChatColor.RED + "Jar file doesn't contain a plugin.yml: " + f.getName());return true;
				}
				if(getServer().getPluginManager().getPlugin(pdf.getName()) != null)
				{
					sender.sendMessage(ChatColor.RED + pdf.getName() + " is already loaded!");return true;
				}
				Plugin p = null;
				if((p=control.loadPlugin(fname)) != null)
				{
					getServer().getPluginManager().enablePlugin(p);
					sender.sendMessage(ChatColor.GREEN + p.getDescription().getName() + " " + p.getDescription().getVersion() + " loaded successfully!");
				}
				else sender.sendMessage(ChatColor.RED + "Failed to load " + args[1] + "!" + (sender instanceof org.bukkit.entity.Player ? "Check console for details!" : ""));
			}
			else if(args.length == 2)
			{
				Plugin plugin = getServer().getPluginManager().getPlugin(args[1]);
				if(args[0].equalsIgnoreCase("enable"))
				{
					if(!sender.hasPermission("pluginmanager.enable") && sender != getServer().getConsoleSender())
					{
						noPerm(sender);return true;
					}
					if(plugin == null)sender.sendMessage(ChatColor.RED + "No such plugin: " + args[1]);
					else
					{
						if(plugin.isEnabled())sender.sendMessage(ChatColor.RED + args[1] + " is already enabled!");
						else
						{
							control.enablePlugin(plugin);
							sender.sendMessage(ChatColor.GREEN + args[1] + " enabled!");
						}
					}
				}
				else if(args[0].equalsIgnoreCase("disable"))
				{
					if(!sender.hasPermission("pluginmanager.disable") && sender != getServer().getConsoleSender())
					{
						noPerm(sender);return true;
					}
					if(plugin == null)sender.sendMessage(ChatColor.RED + "No such plugin: " + args[1]);
					else
					{
						if(!plugin.isEnabled())sender.sendMessage(ChatColor.RED + args[1] + " is already disabled!");
						else
						{
							control.disablePlugin(plugin);
							sender.sendMessage(ChatColor.GREEN + args[1] + " disabled!");
						}
					}
				}
				else if(args[0].equalsIgnoreCase("unload"))
				{
					if(!sender.hasPermission("pluginmanager.unload") && sender != getServer().getConsoleSender())
					{
						noPerm(sender);return true;
					}
					if(plugin == null)sender.sendMessage(ChatColor.RED + "No such plugin: " + args[1]);
					else
					{
						boolean t = plugin == this;
						if(t)unl = true;
						if(control.unloadPlugin(plugin))sender.sendMessage(ChatColor.GREEN + args[1] + " " + plugin.getDescription().getVersion() + " successfully unloaded!");
						else sender.sendMessage(ChatColor.RED + "Failed to unload " + args[1] + "!" + (sender instanceof org.bukkit.entity.Player ? "Check console for details!" : ""));
						if(t)control.cleanup();
					}
				}
				else if(args[0].equalsIgnoreCase("reload"))
				{
					if(!sender.hasPermission("pluginmanager.reload") && sender != getServer().getConsoleSender())
					{
						noPerm(sender);return true;
					}
					if(plugin == null)sender.sendMessage(ChatColor.RED + "No such plugin: " + args[1]);
					else
					{
						File file = control.getFile((JavaPlugin)plugin);
						JavaPlugin loaded = null;
						if(file == null)
						{
							sender.sendMessage(ChatColor.RED + plugin.getName() + "'s jar file is missing!");return true;
						}
						String fname = file.getName().substring(0, file.getName().length() - 4);
						boolean t = plugin == this;
						if(t)unl = true;
						if(!control.unloadPlugin(plugin))sender.sendMessage(ChatColor.RED + "An error occurred while unloading " + args[1] + "!");
						else if((loaded=(JavaPlugin)control.loadPlugin(fname)) == null)
							sender.sendMessage(ChatColor.RED + "Failed to load " + fname + "!" + (sender != getServer().getConsoleSender() ? "Check console for details!" : ""));
						getServer().getPluginManager().enablePlugin(loaded);
						sender.sendMessage(ChatColor.GREEN + loaded.getDescription().getName() + " reloaded successfully.");
						if(t)control.cleanup();
					}
				}
				else if(args[0].equalsIgnoreCase("show"))
				{
					if(!sender.hasPermission("pluginmanager.show") && sender != getServer().getConsoleSender())
					{
						noPerm(sender);return true;
					}
					if(plugin == null)sender.sendMessage(ChatColor.RED + "No such plugin: " + args[1]);
					else
					{
						File file = control.getFile((JavaPlugin)plugin);
						sender.sendMessage(ChatColor.AQUA + "|----Plugin information: " + ChatColor.GREEN + plugin.getName() + ChatColor.AQUA + "----|");
						sender.sendMessage(ChatColor.GOLD + "Status: " + (plugin.isEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
						if(plugin.getDescription().getDescription() != null)sender.sendMessage(ChatColor.AQUA + "Description: " + ChatColor.GREEN + plugin.getDescription().getDescription());
						sender.sendMessage(ChatColor.AQUA + "Version: " + ChatColor.GREEN + plugin.getDescription().getVersion());
						sender.sendMessage(ChatColor.AQUA + "Main class: " + ChatColor.GREEN + plugin.getDescription().getMain());
						sender.sendMessage(ChatColor.AQUA + "Jar file: " + ChatColor.GREEN + file.getName());
						String authors = null;
						if(plugin.getDescription().getAuthors() != null)
						{
							if(!plugin.getDescription().getAuthors().isEmpty())
							{
								for(String a:plugin.getDescription().getAuthors())
								{
									if(authors == null)authors = a;
									else authors = authors + ", " + a;
								}
							}
						}
						if(authors != null)sender.sendMessage(ChatColor.AQUA + (plugin.getDescription().getAuthors().size() == 1 ? "Author: " : "Authors: ") + ChatColor.GREEN + authors);
						if(plugin.getDescription().getWebsite() != null)sender.sendMessage(ChatColor.AQUA + "Website: " + ChatColor.GREEN + plugin.getDescription().getWebsite());
					}
				}
				else if(args[0].equalsIgnoreCase("softreload") || args[0].equalsIgnoreCase("sreload"))
				{
					if(!sender.hasPermission("pluginmanager.softreload") && sender != getServer().getConsoleSender())
					{
						noPerm(sender);return true;
					}
					if(plugin == null)sender.sendMessage(ChatColor.RED + "No such plugin: " + args[1]);
					else if(!plugin.isEnabled())sender.sendMessage(ChatColor.RED + "The plugin is disabled!");
					else
					{
						getServer().getPluginManager().disablePlugin(plugin);
						getServer().getPluginManager().enablePlugin(plugin);
						sender.sendMessage(ChatColor.GREEN + plugin.getDescription().getName() + " " + plugin.getDescription().getVersion() + " soft reloaded successfully.");
					}
				}
				else menu(sender, 1);
			}
		}
		else menu(sender, 1);
		return true;
	}
	public void menu(CommandSender sender, int page)
	{
		sender.sendMessage(ChatColor.GOLD + "PluginManager Page (" + page + "/1)");
		switch(page)
		{
		case 1:
			boolean flag = sender == getServer().getConsoleSender();
			if(sender.hasPermission("pluginmanager.enable") || flag)sender.sendMessage(def("/plm enable <plugin>", "Enables a plugin"));
			if(sender.hasPermission("pluginmanager.disable") || flag)sender.sendMessage(def("/plm disable <plugin>", "Disables a plugin"));
			if(sender.hasPermission("pluginmanager.load") || flag)sender.sendMessage(def("/plm load <plugin>", "Loads a plugin(Must use a file name, no .jar needed)"));
			if(sender.hasPermission("pluginmanager.unload") || flag)sender.sendMessage(def("/plm unload <plugin>", "Unloads a plugin"));
			if(sender.hasPermission("pluginmanager.reload") || flag)sender.sendMessage(def("/plm reload <plugin>", "Unloads then loads a plugin"));
			if(sender.hasPermission("pluginmanager.show") || flag)sender.sendMessage(def("/plm show <plugin>", "Shows information about a plugin"));
			if(sender.hasPermission("pluginmanager.softreload") || flag)sender.sendMessage(def("/plm sreload <plugin>", "Disables and then enables a plugin"));
			if(sender.hasPermission("pluginmanager.list") || flag)sender.sendMessage(def("/plm list [options]", "Lists plugins with specified options, use -option to show options"));
			//if(sender.hasPermission("pluginmanager.jarlist"))sender.sendMessage(def("/pm jlist [page]", "Lists all plugins in the plugin folder"));
			return;
		}
	}
	public String def(String name, String des)
	{
		return ChatColor.GOLD + name + ChatColor.AQUA + " - " + des;
	}
	public void noPerm(CommandSender sender)
	{
		sender.sendMessage(ChatColor.RED + "You do not have enough permissions to use this command!");
	}
	public boolean noPerm(CommandSender sender, String perm)
	{
		if(!sender.hasPermission(perm) && sender != getServer().getConsoleSender())
		{
			noPerm(sender);
			return true;
		}
		return false;
	}
}
