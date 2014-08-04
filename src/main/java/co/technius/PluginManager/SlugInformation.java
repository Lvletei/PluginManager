package co.technius.PluginManager;

public class SlugInformation {

    private final String slug;
    private final String pluginName;

    public SlugInformation(final String slug, final String pluginName) {
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
