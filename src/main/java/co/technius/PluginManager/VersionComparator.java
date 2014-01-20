package co.technius.PluginManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionComparator
{
	public static final VersionComparator THREE_DIGIT
		= new VersionComparator("(?:.* )?v?([\\d]+)\\.?([\\d]+)?\\.?([\\d]+)?") {
		protected int compare0(Matcher m1, Matcher m2)
		{
			int vc1 = m1.groupCount();
			int vc2 = m2.groupCount();
			int m = Math.min(vc1, vc2);
			for(int i = 1; i < m; i ++)
			{
				int v1 = Integer.parseInt(m1.group(i));
				int v2 = Integer.parseInt(m2.group(i));
				if(v1 > v2)return 1;
				else if(v2 > v1)return 2;
			}
			if(vc1 != vc2)return vc1 > vc2 ? 1 : 2;
			return 0;
		}
	};
	
	public static final VersionComparator[] MATCHERS = {
		THREE_DIGIT
	};
	private Pattern pattern;
	private VersionComparator(String pattern)
	{
		this.pattern = Pattern.compile(pattern);
	}
	
	public final int compare(String v1, String v2)
	{
		Matcher m1 = pattern.matcher(Matcher.quoteReplacement(v1));
		Matcher m2 = pattern.matcher(Matcher.quoteReplacement(v2));
		if(!m1.find() || !m2.find())return -1;
		return compare0(m1, m2);
	}
	
	protected int compare0(Matcher m1, Matcher m2)
	{
		return 0;
	}
}
