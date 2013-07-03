package net.skycraftmc.PluginManager;

import org.bukkit.plugin.java.JavaPlugin;

public class PluginManagerPlugin extends JavaPlugin {

	private PluginControl control;
	private boolean       unl = false;

	public PluginControl getPluginControl() {
		return control;
	}

	@Override
	public void onDisable() {
		if (!unl)
			control.cleanup();
	}

	@Override
	public void onEnable() {
		try {
			control = new PluginControl();
		} catch (Exception e) {
			getLogger().severe("Failed to start!");
			setEnabled(false);
			e.printStackTrace();
			return;
		}

		getCommand("pluginmanager").setExecutor(new PMCommandExecutor(this, control));
		getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {

			public void run() {
				try {
					UpdateInformation inf = Updater.findUpdate("pm-pluginmanager");
					if (!getDescription().getVersion().equals(inf.getVersion()))
						getLogger().info("A new version of PluginManager is available: " + inf.getVersion());
				} catch (Exception e) {
					getLogger().warning("Could not find update: " + e.getMessage());
				}
			}
		});
	}

	void setUnload(boolean unl) {
		this.unl = unl;
	}
}
