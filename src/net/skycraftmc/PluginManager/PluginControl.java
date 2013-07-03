package net.skycraftmc.PluginManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.plugin.java.PluginClassLoader;

public class PluginControl {

	private SimpleCommandMap     scm;
	private Map<String, Command> kc;
	private Field                loadersF;

	@SuppressWarnings("unchecked")
	public PluginControl() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		SimplePluginManager spm = (SimplePluginManager) Bukkit.getServer().getPluginManager();

		Field scmF;
		scmF = spm.getClass().getDeclaredField("commandMap");
		scmF.setAccessible(true);
		scm = (SimpleCommandMap) scmF.get(spm);

		Field kcF;
		kcF = scm.getClass().getDeclaredField("knownCommands");
		kcF.setAccessible(true);
		kc = (Map<String, Command>) kcF.get(scm);
	}

	public boolean changeDataFolder(JavaPlugin plugin, String name) {
		Field f;
		try {
			f = plugin.getDescription().getClass().getDeclaredField("dataFolder");
			f.setAccessible(true);
			f.set(plugin, new File("plugins" + File.separator + name));
		} catch (SecurityException | NoSuchFieldException e) {
			e.printStackTrace();
			return false;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return true;
	}

	public boolean changePriority(Plugin p, PluginCommand command) {
		if (isTopPriority(p, command.getName()))
			return true;

		synchronized (scm) {
			if (kc.containsKey(command.getName())) {
				Command ctemp = kc.get(command.getName());
				if (ctemp instanceof PluginCommand) {
					kc.put(((PluginCommand) ctemp).getPlugin().getName() + ":" + ctemp.getName(), ctemp);
					kc.put(command.getName(), command);
				}
			}
		}

		return true;
	}

	void cleanup() {
		scm = null;
		kc = null;
	}

	public boolean closeClassLoader(Plugin plugin) {
		ClassLoader cl = plugin.getClass().getClassLoader();
		if (cl instanceof PluginClassLoader) {
			PluginClassLoader pcl = (PluginClassLoader) cl;
			try {
				Method m = pcl.getClass().getMethod("close");
				m.setAccessible(true);
				m.invoke(pcl);
				return true;
			} catch (Exception e) {
			}
		}

		return false;
	}

	public void disablePlugin(Plugin plugin) {
		Bukkit.getServer().getPluginManager().disablePlugin(plugin);
	}

	public void enablePlugin(Plugin plugin) {
		Bukkit.getServer().getPluginManager().enablePlugin(plugin);
	}

	public PluginCommand getCommand(JavaPlugin plugin, String command) {
		Method m;
		PluginCommand cmd = null;
		try {
			m = JavaPlugin.class.getDeclaredMethod("getCommand", String.class);
			m.setAccessible(true);
			cmd = (PluginCommand) m.invoke(plugin, command);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return cmd;
	}

	public PluginDescriptionFile getDescriptionFromJar(File plugin) {
		if (!plugin.exists())
			return null;

		if (plugin.isDirectory())
			return null;

		if (!plugin.getName().endsWith(".jar"))
			return null;

		try {
			JarFile jf = new JarFile(plugin);
			ZipEntry pyml = jf.getEntry("plugin.yml");
			if (pyml == null)
				return null;

			PluginDescriptionFile pdf = new PluginDescriptionFile(jf.getInputStream(pyml));
			return pdf;
		} catch (IOException | InvalidDescriptionException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	public File getFile(JavaPlugin p) {
		Field f;
		try {
			f = JavaPlugin.class.getDeclaredField("file");
			f.setAccessible(true);
			return (File) f.get(p);
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException e) {
			e.printStackTrace();
		}

		return null;
	}

	public boolean hasCommand(JavaPlugin plugin, String command) {
		return getCommand(plugin, command) != null;
	}

	public boolean isTopPriority(Plugin p, String command) {
		Command c = kc.get(command);
		if (!(c instanceof PluginCommand))
			return false;

		PluginCommand pc = (PluginCommand) c;
		if (pc.getPlugin() == p)
			return true;

		return false;
	}

	public Plugin loadPlugin(String name) {
		Plugin plugin;
		try {
			plugin = Bukkit.getServer().getPluginManager().loadPlugin(new File("plugins" + File.separator + name + ".jar"));
		} catch (InvalidPluginException | InvalidDescriptionException | UnknownDependencyException e) {
			e.printStackTrace();
			return null;
		}

		return plugin;
	}

	public void registerCommands(Plugin p) {
		JavaPlugin plugin = (JavaPlugin) p;
		for (Map.Entry<String, Map<String, Object>> entry : plugin.getDescription().getCommands().entrySet()) {
			PluginCommand c = plugin.getCommand(entry.getKey());
			if (c == null)
				continue;
			kc.put(c.getName().toLowerCase(), c);
		}
	}

	public boolean setName(Plugin plugin, String name) {
		for (Plugin p : Bukkit.getServer().getPluginManager().getPlugins())
			try {
				Field f = PluginDescriptionFile.class.getDeclaredField("name");
				f.setAccessible(true);
				f.set(p.getDescription(), name);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean unloadPlugin(Plugin plugin) {
		SimplePluginManager spm = (SimplePluginManager) Bukkit.getServer().getPluginManager();
		List<Plugin> pl;
		Map<String, Plugin> ln;
		try {
			Field lnF;
			lnF = spm.getClass().getDeclaredField("lookupNames");
			lnF.setAccessible(true);
			ln = (Map<String, Plugin>) lnF.get(spm);

			Field plF;
			plF = spm.getClass().getDeclaredField("plugins");
			plF.setAccessible(true);
			pl = (List<Plugin>) plF.get(spm);
		} catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return false;
		}

		spm.disablePlugin(plugin);
		synchronized (spm) {
			ln.remove(plugin.getName());
			pl.remove(plugin);
		}

		synchronized (scm) {
			Iterator<Map.Entry<String, Command>> it = kc.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Command> entry = it.next();
				if (entry.getValue() instanceof PluginCommand) {
					PluginCommand c = (PluginCommand) entry.getValue();
					if (c.getPlugin().getName().equalsIgnoreCase(plugin.getName())) {
						c.unregister(scm);
						it.remove();
					}
				}
			}
		}

		JavaPluginLoader jpl = (JavaPluginLoader) plugin.getPluginLoader();
		if (loadersF == null)
			try {
				loadersF = jpl.getClass().getDeclaredField("loaders0");
				loadersF.setAccessible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}

		try {
			Map<String, ClassLoader> loaderMap = (Map<String, ClassLoader>) loadersF.get(jpl);
			loaderMap.remove(plugin.getDescription().getName());
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		closeClassLoader(plugin);
		System.gc();
		System.gc();

		return true;
	}

	public boolean unregisterCommand(JavaPlugin plugin, String command) {
		PluginCommand cmd = getCommand(plugin, command);
		if (cmd == null)
			return false;

		synchronized (scm) {
			cmd.unregister(scm);
			if (kc.get(cmd.getName()) == cmd)
				kc.remove(cmd.getName());
			else
				kc.remove(cmd.getName() + ":" + cmd.getName());
		}

		return true;
	}
}
