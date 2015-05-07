package co.technius.PluginManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
//import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

public class PluginControl {

    @SuppressWarnings("serial")
    static class PMStartupException extends Exception {

        public PMStartupException(final String string) {
            super(string);
        }
    }

    private SimpleCommandMap scm;
    private Map<String, Command> kc;
    private Field loadersF;

    private final StringConfig cmdConfig;

    @SuppressWarnings("unchecked")
    public PluginControl(final StringConfig cmdConfig) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, PMStartupException {
        this.cmdConfig = cmdConfig;
        final PluginManager bpm = Bukkit.getServer().getPluginManager();
        
// PerWorldPlugins is outdated. Will re-examine later.
//        if (!(bpm instanceof SimplePluginManager)) {
//            if (Bukkit.getPluginManager().getPlugin("PerWorldPlugins") == null ) {
//                throw new PMStartupException("Unknown Bukkit plugin system detected: " + bpm.getClass().getName());
//            }
//        }

        // SimplePluginManager spm = (SimplePluginManager) bpm;
        Field scmF;
        scmF = bpm.getClass().getDeclaredField("commandMap");
        scmF.setAccessible(true);
        scm = (SimpleCommandMap) scmF.get(bpm);

        if (!(scm instanceof SimpleCommandMap)) {
            throw new PMStartupException("Unsupported Bukkit command system detected: " + scm.getClass().getName());
        }

        Field kcF;
        kcF = SimpleCommandMap.class.getDeclaredField("knownCommands");
        kcF.setAccessible(true);
        kc = (Map<String, Command>) kcF.get(scm);
    }

    public boolean changeDataFolder(final JavaPlugin plugin, final String name) {
        Field f;
        try {
            f = plugin.getDescription().getClass().getDeclaredField("dataFolder");
            f.setAccessible(true);
            f.set(plugin, new File("plugins" + File.separator + name));
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean changePriority(final Plugin p, final PluginCommand command, final boolean pluginLoad) {
        if (isTopPriority(p, command.getName())) {
            return true;
        }

        synchronized (scm) {
            if (kc.containsKey(command.getName())) {
                final Command ctemp = kc.get(command.getName());
                if (ctemp instanceof PluginCommand) {
                    kc.put(((PluginCommand) ctemp).getPlugin().getName() + ":" + ctemp.getName(), ctemp);
                    kc.put(command.getName(), command);
                    if (!pluginLoad) {
                        addToConfig(p.getName(), command.getName());
                    }
                }
            }
        }

        return true;
    }

    public boolean closeClassLoader(final Plugin plugin) {
        try {
            ((URLClassLoader) plugin.getClass().getClassLoader()).close();
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void disablePlugin(final Plugin plugin) {
        Bukkit.getServer().getPluginManager().disablePlugin(plugin);
    }

    public void enablePlugin(final Plugin plugin) {
        Bukkit.getServer().getPluginManager().enablePlugin(plugin);
    }
    
    public void enablePlugin( final Plugin[] plugin )
    {
    	for( int i = 0; i < plugin.length; i++ )
    	{
    		Bukkit.getServer( ).getPluginManager( ).enablePlugin( plugin[i] );
    	}
    }

    public PluginCommand getCommand(final JavaPlugin plugin, final String command) {
        Method m;
        PluginCommand cmd = null;
        try {
            m = JavaPlugin.class.getDeclaredMethod("getCommand", String.class);
            m.setAccessible(true);
            cmd = (PluginCommand) m.invoke(plugin, command);
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }

        return cmd;
    }

    public PluginDescriptionFile getDescriptionFromJar(final File plugin) {
        if (!plugin.exists()) {
            return null;
        }

        if (plugin.isDirectory()) {
            return null;
        }

        if (!plugin.getName().endsWith(".jar")) {
            return null;
        }

        try {
            final JarFile jf = new JarFile(plugin);
            final ZipEntry pyml = jf.getEntry("plugin.yml");
            if (pyml == null) {
                jf.close();
                return null;
            }
            final PluginDescriptionFile pdf = new PluginDescriptionFile(jf.getInputStream(pyml));
            jf.close();
            return pdf;
        } catch (final IOException ioe) {
            ioe.printStackTrace();
        } catch (final InvalidDescriptionException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    public File getFile(final JavaPlugin p) {
        Field f;
        try {
            f = JavaPlugin.class.getDeclaredField("file");
            f.setAccessible(true);
            return (File) f.get(p);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean hasCommand(final JavaPlugin plugin, final String command) {
        return getCommand(plugin, command) != null;
    }

    public boolean isTopPriority(final Plugin p, final String command) {
        final Command c = kc.get(command);
        if (!(c instanceof PluginCommand)) {
            return false;
        }

        final PluginCommand pc = (PluginCommand) c;
        if (pc.getPlugin() == p) {
            return true;
        }

        return false;
    }

    public Plugin loadPlugin(final String name) {
        Plugin plugin;
        try {
            plugin = Bukkit.getServer().getPluginManager().loadPlugin(new File("plugins" + File.separator + name + (name.endsWith(".jar") ? "" : ".jar")));
            try {
                plugin.onLoad();
            } catch (final Exception e) {
                System.out.println("Failed to call 'onLoad()' for plugin '" + plugin.getName() + "'");
                e.printStackTrace();
            }
            return plugin;
        } catch (InvalidPluginException | InvalidDescriptionException | UnknownDependencyException e) {
            e.printStackTrace();
        }
        return null;
    }
        
    public Plugin[] loadPlugin(final ArrayList<String> pluginFiles ) {
        Plugin[] plugin = new Plugin[pluginFiles.size( )];
        File[] plugins = new File[pluginFiles.size( )];
        for( int i = 0; i < pluginFiles.size( ); i++ )
        {
        	plugins[i] = new File("plugins" + File.separator + pluginFiles.get(i) + (pluginFiles.get(i).endsWith(".jar") ? "" : ".jar"));
        }
        try {
        	 for( int i = 0; i < pluginFiles.size( ); i++ )
             {
        		 plugin[i] = Bukkit.getPluginManager( ).loadPlugin( plugins[i] );
             }
            try {
            	 for( int i = 0; i < pluginFiles.size( ); i++ )
                 {
            		 plugin[i].onLoad();
            		 pluginFiles.remove( i );
                 }
            } catch (final Exception e) {
               // System.out.println("Failed to call 'onLoad()' for plugin '" + plugin.getName() + "'");
                e.printStackTrace();
            }
            return plugin;
        } catch (InvalidPluginException | InvalidDescriptionException | UnknownDependencyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void registerCommand(final Plugin plugin, final PluginCommand cmd) {
        registerCommand(plugin, cmd, true);
    }

    public void registerCommand(final Plugin plugin, final PluginCommand cmd, final boolean hasPriority) {
        if (hasPriority) {
            kc.put(cmd.getName().toLowerCase(), cmd);
        } else {
            scm.register(plugin.getName(), cmd);
        }
    }

    public void registerCommands(final Plugin p) {
        final JavaPlugin plugin = (JavaPlugin) p;
        for (final Map.Entry<String, Map<String, Object>> entry : plugin.getDescription().getCommands().entrySet()) {
            final PluginCommand c = plugin.getCommand(entry.getKey());
            if (c == null) {
                continue;
            }
            kc.put(c.getName().toLowerCase(), c);
        }
    }

    public boolean setName(final Plugin plugin, final String name) {
        for (final Plugin p : Bukkit.getServer().getPluginManager().getPlugins()) {
            try {
                final Field f = PluginDescriptionFile.class.getDeclaredField("name");
                f.setAccessible(true);
                f.set(p.getDescription(), name);
            } catch (final Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public boolean unloadPlugin(final Plugin plugin) {
        try {
            plugin.getClass().getClassLoader().getResources("*");
        } catch (final IOException e1) {
            e1.printStackTrace();
        }
        // SimplePluginManager spm = (SimplePluginManager)
        // Bukkit.getServer().getPluginManager();
        final PluginManager pm = Bukkit.getServer().getPluginManager();
        List<Plugin> pl;
        Map<String, Plugin> ln;
        try {
            Field lnF;
            lnF = pm.getClass().getDeclaredField("lookupNames");
            lnF.setAccessible(true);
            ln = (Map<String, Plugin>) lnF.get(pm);

            Field plF;
            plF = pm.getClass().getDeclaredField("plugins");
            plF.setAccessible(true);
            pl = (List<Plugin>) plF.get(pm);
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }

        synchronized (scm) {
            final Iterator<Map.Entry<String, Command>> it = kc.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<String, Command> entry = it.next();
                if (entry.getValue() instanceof PluginCommand) {
                    final PluginCommand c = (PluginCommand) entry.getValue();
                    if (c.getPlugin().getName().equalsIgnoreCase(plugin.getName())) {
                        c.unregister(scm);
                        it.remove();
                    }
                }
            }
        }

        pm.disablePlugin(plugin);
        synchronized (pm) {
            ln.remove(plugin.getName());
            pl.remove(plugin);
        }

        final JavaPluginLoader jpl = (JavaPluginLoader) plugin.getPluginLoader();
        if (loadersF == null) {
            try {
                loadersF = jpl.getClass().getDeclaredField("loaders");
                loadersF.setAccessible(true);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        try {
            final Map<String, ?> loaderMap = (Map<String, ?>) loadersF.get(jpl);
            loaderMap.remove(plugin.getDescription().getName());
        } catch (final Exception e) {
            e.printStackTrace();
        }

        closeClassLoader(plugin);
        System.gc();
        System.gc();
        
        return true;
    }

    public boolean unloadRecursively(final Plugin plugin) {
        final ArrayList<String> pluginFiles = unloadRecursively(plugin.getName(), plugin, new ArrayList<String>());
        while (pluginFiles.size() > 0) {
        	enablePlugin( loadPlugin( pluginFiles ) );
        }
        return true;
    }
    
    // it could be shorter and more compact, but I wrote it just straight down
    // for making changes more easier (at least for me)
    // - Converted to ArrayList from Stack - Lvletei
    public ArrayList<String> unloadRecursively(final String doNotLoad, final Plugin plugin, final ArrayList<String> pluginFiles) {

        if (!plugin.getName().equals(doNotLoad)) {
            final File file = getFile((JavaPlugin) plugin);
            if( !pluginFiles.contains( file.getName( ) ) )
            	pluginFiles.add( file.getName( ) );
        }

        final PluginManager pm = Bukkit.getPluginManager();
        for (final Plugin p : pm.getPlugins()) {
            final List<String> depend = p.getDescription().getDepend();
            if (depend != null) {
                for (final String s : depend) {
                    if (s.equals(plugin.getName())) {
                        //unloadRecursively(doNotLoad, p, pluginFiles);
                    	disablePlugin( p );
                    }
                }
            }

            final List<String> softDepend = p.getDescription().getSoftDepend();
            if (softDepend != null) {
                for (final String s : softDepend) {
                    if (s.equals(plugin.getName())) {
                        //unloadRecursively(doNotLoad, p, pluginFiles);
                    	disablePlugin( p );
                    }
                }
            }
        }

        if (unloadPlugin(plugin)) {

            final List<String> depend = plugin.getDescription().getDepend();
            if (depend != null) {
                for (final String s : depend) {
                    final Plugin p = pm.getPlugin(s);
                    if (p != null) {
                        //unloadRecursively(doNotLoad, p, pluginFiles);
                    	disablePlugin( p );
                    }
                }
            }

            final List<String> softDepend = plugin.getDescription().getSoftDepend();
            if (softDepend != null) {
                for (final String s : softDepend) {
                    final Plugin p = pm.getPlugin(s);
                    if (p != null) {
                        //unloadRecursively(doNotLoad, p, pluginFiles);
                    	disablePlugin( p );
                    }
                }
            }
        }

        return pluginFiles;
    }

    public boolean unregisterCommand(final JavaPlugin plugin, final String command) {
        final PluginCommand cmd = getCommand(plugin, command);
        if (cmd == null) {
            return false;
        }

        synchronized (scm) {
            cmd.unregister(scm);
            if (kc.get(cmd.getName()) == cmd) {
                kc.remove(cmd.getName());
            } else {
                kc.remove(plugin.getName() + ":" + cmd.getName());
            }
            for (final String s : cmd.getAliases()) {
                if (kc.get(s) == cmd) {
                    kc.remove(s);
                } else {
                    kc.remove(plugin.getName() + ":" + s);
                }
            }
        }

        return true;
    }

    void cleanup() {
        scm = null;
        kc = null;
    }

    private void addToConfig(final String pName, final String cmdName) {
        if (!cmdConfig.contains(pName)) {
            cmdConfig.set(pName, cmdName);
        } else {
            final StringBuilder sb = new StringBuilder();

            for (final String s : cmdConfig.getStringList(pName, null)) {
                if (s.equals(cmdName)) {
                    return;
                }
                sb.append(s).append(',');
            }
            sb.append(cmdName);
            cmdConfig.set(pName, sb.toString());
        }

        for (final String key : cmdConfig.getKeySet()) {
            if (key.equals(pName)) {
                continue;
            }

            final List<String> values = Arrays.asList(cmdConfig.getStringList(key, null));
            for (final String s : values.toArray(new String[0])) {
                if (cmdName.equalsIgnoreCase(s)) {
                    values.remove(s);
                }
            }
        }
    }
}
