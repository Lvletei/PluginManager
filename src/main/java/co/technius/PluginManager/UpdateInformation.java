package co.technius.PluginManager;

import java.net.URL;

public class UpdateInformation {

    private final String version;
    private final URL link;
    private final String changelog;

    public UpdateInformation(final String version, final URL link, final String changelog) {
        this.link = link;
        this.version = version;
        this.changelog = changelog;
    }

    public String getChangelog() {
        return changelog;
    }

    public URL getLink() {
        return link;
    }

    public String getVersion() {
        return version;
    }
}
