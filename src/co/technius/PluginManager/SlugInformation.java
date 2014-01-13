package co.technius.PluginManager;

public class SlugInformation {

	private String slug;
	private String pluginName;

	public SlugInformation(String slug, String pluginName) {
		this.slug = slug;
		this.pluginName = pluginName;
	}

	public String getPluginName() {
		return pluginName;
	}

	public String getSlug() {
		return slug;
	}
}
