package co.technius.PluginManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionComparator {

    public static final VersionComparator THREE_DIGIT = new VersionComparator("(?:.* )?v?([\\d]+)\\.?([\\d]+)?\\.?([\\d]+)?") {

        @Override
        protected int compare0(final Matcher m1, final Matcher m2) {
            final int vc1 = m1.groupCount();
            final int vc2 = m2.groupCount();
            final int m = Math.min(vc1, vc2);
            for (int i = 1; i < m; i++) {
                final int v1 = Integer.parseInt(m1
                        .group(i));
                final int v2 = Integer.parseInt(m2
                        .group(i));
                if (v1 > v2) {
                    return 1;
                } else if (v2 > v1) {
                    return 2;
                }
            }
            return vc1 == vc2 ? 0 : vc1 > vc2 ? 1 : 2;
        }

    };

    public static final VersionComparator[] MATCHERS = { THREE_DIGIT };

    private final Pattern pattern;

    private VersionComparator(final String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public final int compare(final String v1, final String v2) {
        final Matcher m1 = pattern.matcher(Matcher.quoteReplacement(v1));
        final Matcher m2 = pattern.matcher(Matcher.quoteReplacement(v2));

        return (!m1.find() || !m2.find()) ? -1 : compare0(m1, m2);
    }

    protected int compare0(final Matcher m1, final Matcher m2) {
        return 0;
    }
}
