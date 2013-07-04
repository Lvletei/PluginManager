package net.skycraftmc.PluginManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class DBOUtilities {

	public enum VersionInfo {
		MAYBE, LATEST, OLD, ERROR
	}

	private static Pattern versionPattern   = Pattern.compile("^[^0-9]*(([0-9]+\\.)*[0-9]+).*$");
	private static Pattern changelogPattern = Pattern.compile(".*?<p>(.*)</p>.*?", Pattern.DOTALL);

	/**
	 * compare two version strings
	 * 
	 * @param v1
	 *            first version string
	 * @param v2
	 *            the version string to compare
	 * @return 0 if both are same, 1 if v1 > v2 or -1 if v2 > v1
	 */
	private static int compareVersions(String v1, String v2) {
		v1 = v1.replaceAll("\\s", "");
		v2 = v2.replaceAll("\\s", "");
		String[] a1 = v1.split("\\.");
		String[] a2 = v2.split("\\.");
		List<String> l1 = Arrays.asList(a1);
		List<String> l2 = Arrays.asList(a2);

		int i = 0;
		while (true) {
			Double d1 = null;
			Double d2 = null;

			try {
				d1 = Double.parseDouble(l1.get(i));
			} catch (IndexOutOfBoundsException e) {
			}

			try {
				d2 = Double.parseDouble(l2.get(i));
			} catch (IndexOutOfBoundsException e) {
			}

			if (d1 != null && d2 != null) {
				if (d1.doubleValue() > d2.doubleValue())
					return 1;
				else if (d1.doubleValue() < d2.doubleValue())
					return -1;
			} else if (d2 == null && d1 != null) {
				if (d1.doubleValue() > 0)
					return 1;
			} else if (d1 == null && d2 != null) {
				if (d2.doubleValue() > 0)
					return -1;
			} else
				break;
			i++;
		}
		return 0;
	}

	/**
	 * 
	 * @param name
	 *            the plugin name which is used for the search, if a slug or plugin name equals or contains that name it will be added to the result
	 * @return a list of available slugs for the given name
	 */
	public static List<SlugInformation> getSlugInformationList(String name) {
		List<SlugInformation> slugInfo = new ArrayList<>();
		URL url = null;
		try {
			url = new URL(String.format("http://api.bukget.org/3/search/plugin_name/=/%s", name)); //case sensitive, I will look for another solution
		} catch (MalformedURLException e) {
			//that will never happen....
		}

		try {
			JsonArray slugArray = JsonArray.readFrom(new InputStreamReader(url.openStream()));
			for (JsonValue value : slugArray.values())
				if (value.isObject()) {
					JsonObject object = value.asObject();
					String slug = object.get("slug").asString();
					String pluginName = object.get("plugin_name").asString();
					slugInfo.add(new SlugInformation(slug, pluginName));
				}
		} catch (IOException e) {
			//If that occurs, then (I believe) it's my fault.
			e.printStackTrace();
		}

		return slugInfo;
	}

	/**
	 * @param slug
	 *            the slug of the dbo plugin site
	 * @return the latest update info or null if there is nothing found
	 */
	public static UpdateInformation getUpdateInfo(String slug) {
		URL infoUrl = null;

		try {
			infoUrl = new URL(String.format("http://api.bukget.org/3/plugins/bukkit/%s/latest?fields=versions.version,versions.download,versions.changelog", slug));
		} catch (MalformedURLException e) {
			//that will never happen....
			e.printStackTrace();
		}

		JsonObject rawInfo = null;
		try {
			rawInfo = JsonObject.readFrom(new InputStreamReader(infoUrl.openStream()));
		} catch (IOException e) {
			//It that occurs, then (I believe) it's my fault.
			e.printStackTrace();
			return null;
		}

		JsonArray versionInfo = rawInfo.get("versions").asArray();

		if (versionInfo.size() > 0) {
			JsonObject latestInfo = versionInfo.get(0).asObject();
			String version = latestInfo.get("version").asString();
			String downloadLink = latestInfo.get("download").asString();
			String encodedChangelog = latestInfo.get("changelog").asString().replaceAll("\\s", "");

			String decodedChangelog = Base64Coder.decodeString(encodedChangelog);

			Matcher clMatcher = changelogPattern.matcher(decodedChangelog);
			String changelog = "No changelog available.";

			if (clMatcher.matches())
				changelog = clMatcher.group(1);

			try {
				return new UpdateInformation(version, new URL(downloadLink), changelog);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public static VersionInfo isUpToDate(Plugin plugin, UpdateInformation latestInfo) {
		try {
			if (plugin == null || latestInfo == null)
				return VersionInfo.ERROR;

			String pluginVersion = plugin.getDescription().getVersion();
			String latestVersion = latestInfo.getVersion();
			if (pluginVersion.equalsIgnoreCase(latestVersion))
				return VersionInfo.LATEST;

			Matcher plVersionMatcher = versionPattern.matcher(pluginVersion);
			String extractedPluginVersion;

			if (plVersionMatcher.matches())
				extractedPluginVersion = plVersionMatcher.group(1);
			else
				return VersionInfo.MAYBE;

			Matcher latestVersionMatcher = versionPattern.matcher(pluginVersion);
			String extractedLatestVersion;

			if (latestVersionMatcher.matches())
				extractedLatestVersion = latestVersionMatcher.group(1);
			else
				return VersionInfo.MAYBE;

			int result = compareVersions(extractedPluginVersion, extractedLatestVersion);
			switch (result) {
				case -1: {
					return VersionInfo.OLD;
				}
				default: {
					return VersionInfo.LATEST;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return VersionInfo.ERROR;
		}
	}

}
