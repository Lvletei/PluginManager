package net.skycraftmc.PluginManager;

public class StringUtils
{
    /**
     * get a string out of the given array, starting at the given position
     * 
     * @param array
     * @param position
     * @return string
     */
    public static String getStringOfArray(String[] array, int position)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = position; i < array.length; i++)
        {
            sb.append(array[i]);
            sb.append(" ");
        }
        return sb.toString();
    }
}
