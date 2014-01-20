package co.technius.PluginManager;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import co.technius.PluginManager.DBOUtilities.VersionInfo;

public class PluginManagerPlugin extends JavaPlugin
{

	private PluginControl control;
	private boolean unl = false;

	private StringConfig cmdConfig;
	private StringConfig config;

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
			getLogger().severe("Failed to save command priorities!");
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
		cmdConfig = new StringConfig(new File(getDataFolder(), "commands.cfg"));
		config = new StringConfig(new File(getDataFolder(), "config.txt"));
		try
		{
			cmdConfig.start();
			cmdConfig.load();
			config.start();
			config.load();
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
			getLogger().log(Level.SEVERE, "Failed to start", e);
			setEnabled(false);
			return;
		}

		getCommand("pluginmanager").setExecutor(
				new PMCommandExecutor(this, control));
		if(config.getBoolean("updater.startcheck", true))
		{
			final Plugin pm = this;
			new BukkitRunnable()
			{
				public void run()
				{
					try
					{
						VersionInfo info = DBOUtilities.isUpToDate(pm,
								"pm-pluginmanager");
						if (info == VersionInfo.OLD)
						{
							getLogger()
									.info("A new version of PluginManager is available: "
											+ DBOUtilities
													.getLatestVersion("pm-pluginmanager").version);
						}
					}
					catch (Exception e)
					{
						getLogger().log(Level.WARNING, "Could not find update", e);
					}
				}
			}.runTaskAsynchronously(this);
		}
		updateCommandPriorities();
		getServer().getPluginManager().registerEvents(new PluginListener(this),
				this);
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
