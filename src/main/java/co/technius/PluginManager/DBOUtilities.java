package co.technius.PluginManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.plugin.Plugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class DBOUtilities {

    public enum VersionInfo {
        LATEST,
        OLD,
        NOT_IN_USE,
        ERROR,
        UNKNOWN,
        NONEXISTANT
    }

    static class VersionInformation {

        public String version;
        public String pluginname;
        public String slug;
        public String type;

        public VersionInformation(final String version, final String pluginname, final String slug, final String type) {
            this.version = version;
            this.pluginname = pluginname;
            this.slug = slug;
            this.type = type;
        }
    }

    public static int compareVersions(final String v1, final String v2) {
        for (final VersionComparator c : VersionComparator.MATCHERS) {
            final int code = c.compare(v1, v2);
            if (code != -1) {
                return code;
            }
        }
        return -1;
    }

    public static VersionInformation getLatestVersion(final String slug) throws MalformedURLException, IOException {
        final JSONObject po = getProjectObject(slugSearch(slug), slug);
        final int id = getId(po);
        if (id == -1) {
            return null;
        }
        final URL url = new URL("https://api.curseforge.com/servermods/files?projectIds=" + id);
        final HttpURLConnection huc = openConnection(url);
        final InputStreamReader reader = new InputStreamReader(huc.getInputStream());
        final JSONArray files = (JSONArray) JSONValue.parse(reader);
        final VersionInformation inf = new VersionInformation(null, (String) po.get("name"), null, null);
        if (files.size() == 0) {
            return inf;
        }
        final JSONObject v = (JSONObject) files.get(files.size() - 1);
        inf.version = (String) v.get("name");
        inf.type = (String) v.get("releaseType");
        inf.slug = slug;
        reader.close();
        return inf;
    }

    /**
     * @param name
     *            the plugin name which is used for the search, if a slug or
     *            plugin name equals or contains that name it will be added to
     *            the result
     * @return a list of available slugs for the given name
     */
    public static List<SlugInformation> getSlugInformationList(final String name) {
        final List<SlugInformation> slugInfo = new ArrayList<SlugInformation>();
        URL url = null;
        try {
            url = new URL("https://api.curseforge.com/servermods/projects?search="
                    + name.toLowerCase());
        } catch (final MalformedURLException e) {
            // that will never happen....
        }

        try {
            final HttpURLConnection huc = openConnection(url);
            final JSONArray slugArray = (JSONArray) JSONValue.parse(new InputStreamReader(huc.getInputStream()));
            for (final Object value : slugArray.toArray()) {
                if (value instanceof JSONObject) {
                    final JSONObject object = (JSONObject) value;
                    final String slug = (String) object.get("slug");
                    final String pluginName = (String) object.get("name");
                    slugInfo.add(new SlugInformation(slug, pluginName));
                }
            }
        } catch (final IOException e) {
            // ServerModsAPI unavailable
            return null;
        }

        return slugInfo;
    }

    public static VersionInfo isUpToDate(final Plugin plugin, final String slug) {
        try {
            return isUpToDate(plugin, getLatestVersion(slug));
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return VersionInfo.ERROR;
    }

    public static VersionInfo isUpToDate(final Plugin plugin, final VersionInformation info) {
        if (plugin == null) {
            return VersionInfo.NOT_IN_USE;
        }
        if (info.version == null) {
            return VersionInfo.NONEXISTANT;
        }
        try {
            final String pluginVersion = plugin.getDescription().getVersion();
            if (pluginVersion.equalsIgnoreCase(info.version)) {
                return VersionInfo.LATEST;
            }
            switch (compareVersions(pluginVersion, info.version)) {
            case -1:
                return VersionInfo.UNKNOWN;
            case 1:
                return VersionInfo.LATEST;
            case 2:
                return VersionInfo.OLD;
            default:
                return VersionInfo.LATEST;
            }
        } catch (final Exception e) {
            e.printStackTrace();
            return VersionInfo.ERROR;
        }
    }

    private static int getId(final JSONObject searchObject) {
        return ((Long) searchObject.get("id")).intValue();
    }

    private static JSONObject getProjectObject(final JSONArray a, final String slug) {
        for (final Object o : a) {
            final JSONObject p = (JSONObject) o;
            if (((String) p.get("slug")).equals(slug)) {
                return p;
            }
        }
        return null;
    }

    private static HttpURLConnection openConnection(final URL u) throws IOException {
        final HttpURLConnection huc = (HttpURLConnection) u.openConnection();
        huc.addRequestProperty("User-Agent", "PluginManager/v1.2 (by Technius and Sehales)");
        return huc;
    }

    private static JSONArray slugSearch(final String slug) throws MalformedURLException, IOException {
        final HttpURLConnection huc = openConnection(new URL("https://api.curseforge.com/servermods/projects?search=" + slug));
        final InputStreamReader reader = new InputStreamReader(huc.getInputStream());
        final JSONArray a = (JSONArray) JSONValue.parse(reader);
        reader.close();
        return a;
    }
}
