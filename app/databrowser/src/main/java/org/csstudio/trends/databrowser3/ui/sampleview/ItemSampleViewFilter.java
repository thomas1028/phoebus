package org.csstudio.trends.databrowser3.ui.sampleview;

public class ItemSampleViewFilter {
    public enum FilterType {
        NO_FILTER("None"), //TODO: get from Messages
        ALARM_UP("Alarm up"), //TODO: get from Messages
        ALARM_CHANGES("Alarm change"), //TODO: get from Messages
        THRESHOLD_UP("Threshold up"), //TODO: get from Messages
        THRESHOLD_CHANGES("Threshold change"); //TODO: get from Messages

        private String label;

        private FilterType(String label) {
            this.label = label;
        }

        /** @return Array of display names for all Filter types */
        public static String[] getDisplayNames() {
            final FilterType[] types = FilterType.values();
            final String[] names = new String[types.length];
            for (int i=0; i<names.length; ++i)
                names[i] = types[i].label;
            return names;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    private FilterType state;
    private double filterValue;

    public ItemSampleViewFilter() {
        this.state = FilterType.NO_FILTER;
        this.filterValue = 0.0f;
    }

    public ItemSampleViewFilter(ItemSampleViewFilter source) {
        this.state = source.getFilterType();
        this.filterValue = source.getFilterValue();
    }

    public FilterType getFilterType() {
        return state;
    }

    public void setFilterType(FilterType state) {
        this.state = state;
    }

    public double getFilterValue() {
        return filterValue;
    }

    public void setFilterValue(double filterValue) {
        this.filterValue = filterValue;
    }
}
