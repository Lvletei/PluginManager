package net.skycraftmc.PluginManager;

import java.net.URL;

public class UpdateInformation {

	private String version;
	private URL    link;
	private String changelog;

	public UpdateInformation(String version, URL link, String changelog) {
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
