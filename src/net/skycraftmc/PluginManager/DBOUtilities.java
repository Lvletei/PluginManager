package net.skycraftmc.PluginManager;

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
    
    public static int compareVersions(String v1, String v2)
    {
    	for(VersionComparator c: VersionComparator.MATCHERS)
    	{
    		int code = c.compare(v1, v2);
    		if(code != -1)return code;
    	}
    	return -1;
    }

    public static VersionInformation getLatestVersion(String slug) throws MalformedURLException,
            IOException
    {
    	JSONObject po = getProjectObject(slugSearch(slug), slug);
    	int id = getId(po);
    	if(id == -1)return null;
        URL url = new URL("https://api.curseforge.com/servermods/files?projectIds=" + id);
        HttpURLConnection huc = openConnection(url);
        InputStreamReader reader = new InputStreamReader(huc.getInputStream());
        JSONArray files = (JSONArray) JSONValue.parse(reader);
        VersionInformation inf = new VersionInformation(null, (String) po.get("name"),
        	null, null);
        if(files.size() == 0)
            return inf;
        JSONObject v = (JSONObject) files.get(files.size() - 1);
        inf.version = (String) v.get("name");
        inf.type = (String) v.get("releaseType");
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
            url = new URL("https://api.curseforge.com/servermods/projects?search=" + name.toLowerCase());
        }
        catch (MalformedURLException e)
        {
            // that will never happen....
        }

        try
        {
        	HttpURLConnection huc = openConnection(url);
            JSONArray slugArray = (JSONArray) JSONValue.parse(new InputStreamReader(huc.getInputStream()));
            for (Object value : slugArray.toArray())
            {
                if (value instanceof JSONObject)
                {
                    JSONObject object = (JSONObject) value;
                    String slug = (String) object.get("slug");
                    String pluginName = (String) object.get("name");
                    slugInfo.add(new SlugInformation(slug, pluginName));
                }
            }
        }
        catch (IOException e)
        {
            // ServerModsAPI unavailable
            return null;
        }

        return slugInfo;
    }
    
    public static VersionInfo isUpToDate(Plugin plugin, String slug)
    {
    	try
		{
			return isUpToDate(plugin, getLatestVersion(slug));
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
    	return VersionInfo.ERROR;
    }

    public static VersionInfo isUpToDate(Plugin plugin, VersionInformation info)
    {
        if(plugin == null)
        	return VersionInfo.NOT_IN_USE;
        if(info.version == null)
        	return VersionInfo.NONEXISTANT;
        try
        {
            String pluginVersion = plugin.getDescription().getVersion();
            if(pluginVersion.equalsIgnoreCase(info.version))
                return VersionInfo.LATEST;
            switch (compareVersions(pluginVersion, info.version))
            {
                case -1: return VersionInfo.UNKNOWN;
                case 1: return VersionInfo.LATEST;
                case 2: return VersionInfo.OLD;
                default: return VersionInfo.LATEST;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return VersionInfo.ERROR;
        }
    }
    
    private static JSONArray slugSearch(String slug) throws MalformedURLException, IOException
    {
    	HttpURLConnection huc = openConnection(new URL(
    		"https://api.curseforge.com/servermods/projects?search=" + slug));
    	InputStreamReader reader = new InputStreamReader(huc.getInputStream());
    	JSONArray a = (JSONArray) JSONValue.parse(reader);
    	reader.close();
    	return a;
    }
    
    private static JSONObject getProjectObject(JSONArray a, String slug)
    {
    	for(Object o: a)
    	{
    		JSONObject p = (JSONObject) o;
    		if(((String)p.get("slug")).equals(slug))
    			return p;
    	}
    	return null;
    }
    
    private static int getId(JSONObject searchObject)
    {
		return ((Long) searchObject.get("id")).intValue();
    }
    
    private static HttpURLConnection openConnection(URL u) throws IOException
    {
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();
        huc.addRequestProperty("User-Agent", "PluginManager/v1.2 (by Technius and Sehales)");
        return huc;
    }
}
