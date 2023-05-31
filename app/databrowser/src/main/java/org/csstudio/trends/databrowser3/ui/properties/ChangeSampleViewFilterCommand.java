package org.csstudio.trends.databrowser3.ui.properties;


import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.ui.sampleview.ItemSampleViewFilter;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/**
 * UNdo-able command to change item's display filters in the sample view
 * @author Thomas Lehrach
 */
public class ChangeSampleViewFilterCommand extends UndoableAction {

    final private ModelItem item;
    final private ItemSampleViewFilter old_filter, new_filter; //TODO: possibly change to ENUM

    /** TODO
     *  Differentiate between updating filter threshold value and filter type
     *      - Data type for all filter parameters
     *      - Second consstructor for updating filter threshold value
     *      - New class for updating filter threshold value
     *
     *      Filter is stored in ModelItem
     */

    public ChangeSampleViewFilterCommand(final UndoableActionManager operations_manager,
            final ModelItem item, final ItemSampleViewFilter new_filter) {
        super("Change Sample View Filter"); //TODO: get from Messages
        this.item = item;
        this.old_filter = item.getSampleViewFilter();
        this.new_filter = new_filter;
        operations_manager.execute(this);
    }

    @Override
    public void run() {
        item.setSampleViewFilter(new_filter);
        // Set filter in model item to new
    }

    @Override
    public void undo() {
        item.setSampleViewFilter(old_filter);
        // Set filter in model item to old
    }
}