package org.csstudio.trends.databrowser3.ui.sampleview;


import javafx.scene.control.MenuItem;
import org.csstudio.trends.databrowser3.ui.Perspective;
import org.phoebus.ui.javafx.ImageCache;

/** Menu item to toggle position of SampleView between bottom_tabs and main view
 * @author Thomas Lehrach
 */
public class ToggleSampleViewPositionMenuItem extends MenuItem
{
    public ToggleSampleViewPositionMenuItem(final Perspective perspective) // FIXME: is that the right way to do that?
    {
        if (perspective.isSampleViewInBottomTabs()) { // TODO: check position
            setText("Move SampleView Up (Replace Plot)"); //TODO: get from messages
            setGraphic(ImageCache.getImageView(SampleView.class, "/icons/up.png"));
            setOnAction(event -> perspective.setSampleviewLocation(false));
        }
        else {
            setGraphic(ImageCache.getImageView(SampleView.class, "/icons/down.png")); //TODO: change
            setText("Move SampleView Down"); //TODO: get from messages
            setOnAction(event -> perspective.setSampleviewLocation(true));
        }
    }
}
