package net.skycraftmc.PluginManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.plugin.Plugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class DBOUtilities
{

    public enum VersionInfo
    {
        LATEST, OLD, NOT_IN_USE, ERROR, UNKNOWN, NONEXISTANT
    }

    static class VersionInformation
    {
        public String version;
        public String pluginname;
        public String slug;
        public String type;

        public VersionInformation(String version, String pluginname, String slug, String type)
        {
            this.version = version;
            this.pluginname = pluginname;
            this.slug = slug;
            this.type = type;
        }
    }

    private static Pattern versionPattern = Pattern.compile("^[^0-9]*(([0-9]+\\.)*[0-9]+).*$");

    // private static Pattern changelogPattern =
    // Pattern.compile(".*?<p>(.*)</p>.*?", Pattern.DOTALL);

    /**
     * compare two version strings
     * 
     * @param v1
     *            first version string
     * @param v2
     *            the version string to compare
     * @return 0 if both are same, 1 if v1 > v2 or -1 if v2 > v1
     */
    private static int compareVersions(String v1, String v2)
    {
        v1 = v1.replaceAll("\\s", "");
        v2 = v2.replaceAll("\\s", "");
        String[] a1 = v1.split("\\.");
        String[] a2 = v2.split("\\.");
        List<String> l1 = Arrays.asList(a1);
        List<String> l2 = Arrays.asList(a2);

        int i = 0;
        while (true)
        {
            Double d1 = null;
            Double d2 = null;

            try
            {
                d1 = Double.parseDouble(l1.get(i));
            }
            catch (IndexOutOfBoundsException e)
            {
            }

            try
            {
                d2 = Double.parseDouble(l2.get(i));
            }
            catch (IndexOutOfBoundsException e)
            {
            }

            if (d1 != null && d2 != null)
            {
                if (d1.doubleValue() > d2.doubleValue())
                {
                    return 1;
                }
                else if (d1.doubleValue() < d2.doubleValue())
                {
                    return -1;
                }
            }
            else if (d2 == null && d1 != null)
            {
                if (d1.doubleValue() > 0)
                {
                    return 1;
                }
            }
            else if (d1 == null && d2 != null)
            {
                if (d2.doubleValue() > 0)
                {
                    return -1;
                }
            }
            else
            {
                break;
            }
            i++;
        }
        return 0;
    }

    public static VersionInformation getLatestVersion(String slug) throws MalformedURLException,
            IOException
    {
        URL url;
        url = new URL("http://api.bukget.org/3/plugins/bukkit/" + slug.replace(" ", "%20")
                + "?size=1");
        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        if (huc.getResponseCode() == 404)
        {
            return null;
        }
        InputStreamReader reader = new InputStreamReader(huc.getInputStream());
        JSONObject o = (JSONObject) JSONValue.parse(reader);
        JSONArray versions = (JSONArray) o.get("versions");
        VersionInformation inf = new VersionInformation(null, null, null, null);
        if (versions.size() == 0)
        {
            return inf;
        }
        JSONObject v = (JSONObject) versions.get(0);
        inf.version = (String) v.get("version");
        inf.type = (String) v.get("type");
        inf.pluginname = (String) o.get("plugin_name");
        inf.slug = slug;
        reader.close();
        return inf;
    }

    /**
     * 
     * @param name
     *            the plugin name which is used for the search, if a slug or
     *            plugin name equals or contains that name it will be added to
     *            the result
     * @return a list of available slugs for the given name
     */
    public static List<SlugInformation> getSlugInformationList(String name)
    {
        List<SlugInformation> slugInfo = new ArrayList<SlugInformation>();
        URL url = null;
        try
        {
            url = new URL("http://api.bukget.org/3/search/plugin_name/like/" + name.toLowerCase());
        }
        catch (MalformedURLException e)
        {
            // that will never happen....
        }

        try
        {
            JSONArray slugArray = (JSONArray) JSONValue.parse(new InputStreamReader(url.openStream()));
            for (Object value : slugArray.toArray())
            {
                if (value instanceof JSONObject)
                {
                    JSONObject object = (JSONObject) value;
                    String slug = (String) object.get("slug");
                    String pluginName = (String) object.get("plugin_name");
                    slugInfo.add(new SlugInformation(slug, pluginName));
                }
            }
        }
        catch (IOException e)
        {
            // BukGet unavailable
            return null;
        }

        return slugInfo;
    }

    public static VersionInfo isUpToDate(Plugin plugin, String slug)
    {
        String dbolatest;
        URL url;
        try
        {

            url = new URL("http://api.bukget.org/3/plugins/bukkit/" + slug + "?size=1");
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            if (huc.getResponseCode() == 404)
            {
                return VersionInfo.NONEXISTANT;
            }
            InputStreamReader reader = new InputStreamReader(huc.getInputStream());
            JSONObject o = (JSONObject) JSONValue.parse(reader);
            JSONArray versions = (JSONArray) o.get("versions");
            if (versions.size() == 0)
            {
                return VersionInfo.LATEST;
            }
            dbolatest = (String) ((JSONObject)versions.get(0)).get("version");
            reader.close();
        }
        catch (MalformedURLException e1)
        {
            return VersionInfo.ERROR;
        }
        catch (IOException e)
        {
            return VersionInfo.ERROR;
        }
        try
        {
            if (plugin == null)
            {
                return VersionInfo.NOT_IN_USE;
            }

            String pluginVersion = plugin.getDescription().getVersion();
            if (pluginVersion.equalsIgnoreCase(dbolatest))
            {
                return VersionInfo.LATEST;
            }

            Matcher plVersionMatcher = versionPattern.matcher(pluginVersion);
            String extractedPluginVersion;

            if (plVersionMatcher.matches())
            {
                extractedPluginVersion = plVersionMatcher.group(1);
            }
            else
            {
                return VersionInfo.UNKNOWN;
            }

            Matcher latestVersionMatcher = versionPattern.matcher(dbolatest);
            String extractedLatestVersion;

            if (latestVersionMatcher.matches())
            {
                extractedLatestVersion = latestVersionMatcher.group(1);
            }
            else
            {
                return VersionInfo.UNKNOWN;
            }

            int result = compareVersions(extractedPluginVersion, extractedLatestVersion);
            switch (result)
            {
                case -1:
                {
                    return VersionInfo.OLD;
                }
                default:
                {
                    return VersionInfo.LATEST;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return VersionInfo.ERROR;
        }
    }
}
