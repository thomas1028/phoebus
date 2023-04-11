/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.sampleview;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.*;
import org.epics.vtype.Alarm;
import org.epics.vtype.VType;
import org.phoebus.archive.vtype.DoubleVTypeFormat;
import org.phoebus.archive.vtype.VTypeFormat;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.ui.pv.SeverityColors;
import org.phoebus.util.time.TimestampFormats;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Panel for inspecting samples of a trace
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SampleView extends VBox
{
    private final Model model;
    private final ComboBox<String> items = new ComboBox<>();
    private final Label sample_count = new Label(Messages.SampleView_Count);
    private final CheckBox alarm_changes_checkbox = new CheckBox("Show only alarm changes"); //TODO: get from Messages
    private final TableView<PlotSampleWrapper> sample_table = new TableView<>();
    private volatile String item_name = null;
    private final ObservableList<PlotSampleWrapper> samples = FXCollections.observableArrayList();
    private final FilteredList<PlotSampleWrapper> filtered_samples = new FilteredList<>(samples);
    private final SortedList<PlotSampleWrapper> sorted_samples = new SortedList<>(filtered_samples);

    private static class SeverityColoredTableCell extends TableCell<PlotSampleWrapper, String>
    {
        @Override
        protected void updateItem(final String item, final boolean empty)
        {
            super.updateItem(item, empty);
            final TableRow<PlotSampleWrapper> row = getTableRow();
            if (empty  ||  row == null  ||  row.getItem() == null)
                setText("");
            else
            {
                setText(item);
                setTextFill(SeverityColors.getTextColor(org.phoebus.core.vtypes.VTypeHelper.getSeverity(row.getItem().getVType())));
            }
        }
    }

    /** @param model Model */
    public SampleView(final Model model)
    {
        this.model = model;

        items.setOnAction(event -> select(items.getSelectionModel().getSelectedItem()));

        final Button refresh = new Button(Messages.SampleView_Refresh);
        refresh.setTooltip(new Tooltip(Messages.SampleView_RefreshTT));
        refresh.setOnAction(event -> update());

        final Label label = new Label(Messages.SampleView_Item);
        final HBox top_row = new HBox(5, label, items, refresh);
        top_row.setAlignment(Pos.CENTER_LEFT);

        final HBox second_row = new HBox(5, sample_count, alarm_changes_checkbox);
        HBox.setMargin(alarm_changes_checkbox, new Insets(0, 0, 0, 10));
        second_row.setAlignment(Pos.CENTER_LEFT);

        alarm_changes_checkbox.setTooltip(new Tooltip("Show only samples with alarm changes")); //TODO: get from Messages
        alarm_changes_checkbox.setOnAction(event -> update());

        // Combo should fill the available space.
        // Tried HBox.setHgrow(items, Priority.ALWAYS) etc.,
        // but always resulted in shrinking the label and button.
        // -> Explicitly compute combo width from available space
        //    minus padding and size of label, button
        items.prefWidthProperty().bind(top_row.widthProperty().subtract(20).subtract(label.widthProperty()).subtract(refresh.widthProperty()));
        items.prefHeightProperty().bind(refresh.heightProperty());

        createSampleTable();

        top_row.setPadding(new Insets(5));
        sample_count.setPadding(new Insets(5));
        sample_table.setPadding(new Insets(0, 5, 5, 5));
        VBox.setVgrow(sample_table, Priority.ALWAYS);
        getChildren().setAll(top_row, second_row, sample_table);

        // TODO Add 'export' to sample view? CSV in a format usable by import

        update();
    }

    private void createSampleTable()
    {
        TableColumn<PlotSampleWrapper, String> col = new TableColumn<>(Messages.TimeColumn);
        final VTypeFormat format = DoubleVTypeFormat.get();
        col.setCellValueFactory(cell -> new SimpleStringProperty(TimestampFormats.FULL_FORMAT.format(org.phoebus.core.vtypes.VTypeHelper.getTimestamp(cell.getValue().getVType()))));
        sample_table.getColumns().add(col);
        sample_table.getSortOrder().add(col);

        col = new TableColumn<>(Messages.ValueColumn);
        col.setCellValueFactory(cell -> new SimpleStringProperty(format.format(cell.getValue().getVType())));
        sample_table.getColumns().add(col);

        col = new TableColumn<>(Messages.SeverityColumn);
        col.setCellFactory(c -> new SeverityColoredTableCell());
        col.setCellValueFactory(cell -> new SimpleStringProperty(org.phoebus.core.vtypes.VTypeHelper.getSeverity(cell.getValue().getVType()).name()));
        sample_table.getColumns().add(col);

        col = new TableColumn<>(Messages.StatusColumn);
        col.setCellValueFactory(cell -> new SimpleStringProperty(VTypeHelper.getMessage(cell.getValue().getVType())));
        sample_table.getColumns().add(col);

        col = new TableColumn<>(Messages.PVColumn);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPVName()));
        sample_table.getColumns().add(col);

        col = new TableColumn<>(Messages.SampleView_Source);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSource()));
        sample_table.getColumns().add(col);

        sample_table.setMaxWidth(Double.MAX_VALUE);
        sample_table.setPlaceholder(new Label(Messages.SampleView_SelectItem));
        sample_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        sample_table.setItems(sorted_samples);
        sorted_samples.comparatorProperty().bind(sample_table.comparatorProperty());
    }

    private void select(final String item_name)
    {
        this.item_name = item_name;

        if (item_name!=null && item_name.equals("All")) {
            Activator.thread_pool.submit(this::getSamplesAll);
        } else {
            Activator.thread_pool.submit(this::getSamples);
        }
    }

    private void update()
    {
        final List<String> model_items = model.getItems().stream().map(ModelItem::getResolvedName).collect(Collectors.toList());
        final List<String> items_without_all = items.getItems().stream()
                .filter(item -> ! item.equals("All")).collect(Collectors.toList()); // for comparing with model_items and updating on change

        if (! model_items.equals(items_without_all))
        {
            items.getItems().setAll( model_items );

            if (! model_items.isEmpty()) {
                items.getItems().add("All");
            }

            if (item_name != null)
                items.getSelectionModel().select(item_name);
        }
        // Update samples off the UI thread
        if (item_name != null && item_name.equals("All")) {
            Activator.thread_pool.submit(this::getSamplesAll);
        } else {
            Activator.thread_pool.submit(this::getSamples);
        }
    }

    private void getSamples()
    {
        final ModelItem item = model.getItem(item_name);
        final ObservableList<PlotSampleWrapper> samples = FXCollections.observableArrayList();
        if (item != null)
        {
                final PlotSamples item_samples = item.getSamples();
                try
                {
                    if (item_samples.getLock().tryLock(2, TimeUnit.SECONDS))
                    {
                        final int N = item_samples.size();
                        for (int i=0; i<N; ++i) {
                            PlotSampleWrapper wrapped_sample = new PlotSampleWrapper(item_samples.get(i), item);
                            samples.add(wrapped_sample);
                        }
                        item_samples.getLock().unlock();
                    }
                }
                catch (Exception ex)
                {
                    Activator.logger.log(Level.WARNING, "Cannot access samples for " + item.getResolvedName(), ex);
                }
        }
        // Update UI
        Platform.runLater(() -> updateSamples(samples));
    }

    private void getSamplesAll() {
        final List<ModelItem> items = model.getItems();
        final ObservableList<PlotSampleWrapper> samples = FXCollections.observableArrayList();

        if (!items.isEmpty()) {
            for (ModelItem item : items) {
                final PlotSamples item_samples = item.getSamples();
                try {
                    if (item_samples.getLock().tryLock(2, TimeUnit.SECONDS)) {
                        final int N = item_samples.size();
                        for (int i = 0; i < N; ++i) {
                            PlotSampleWrapper wrapped_sample = new PlotSampleWrapper(item_samples.get(i), item);
                            samples.add(wrapped_sample);
                        }
                        item_samples.getLock().unlock();
                    }
                } catch (Exception ex) {
                    Activator.logger.log(Level.WARNING, "Cannot access samples for " + item.getResolvedName(), ex);
                }
            }
        }
        // Update UI
        Platform.runLater(() -> updateSamples(samples));
    }

    private void updateSamples(ObservableList<PlotSampleWrapper> samples)
    {

        // check if only viewing alarm changes
        if (alarm_changes_checkbox.isSelected()) {
            this.samples.setAll(showAlarmChanges());    // maybe integrate this into getSamples() and getSamplesAll() to not iterate twice
        } else {
            this.samples.setAll(samples);
        }


        if (Objects.equals(item_name, "All")) {
            sample_table.getColumns().get(4).setVisible(true); // Display the PVitem name (Column 4)
                // Hide samples that are not visible in the plot when viewing all items
            filtered_samples.setPredicate(sample -> sample.getModelItem().isVisible());
            sample_count.setText(Messages.SampleView_Count + " " + samples.size()
                    + " (" + Messages.SampleView_Count_Visible + " " + filtered_samples.size() + ")");
        } else {
            sample_table.getColumns().get(4).setVisible(false);
                // No need to hide if only viewing one item
            filtered_samples.setPredicate(sample -> true);
            sample_count.setText(Messages.SampleView_Count + " " + samples.size());


            if (alarm_changes_checkbox.isSelected()) {
                sample_count.setText(Messages.SampleView_Count + " " + samples.size()
                        + " (" + Messages.SampleView_Count_Visible + " " + filtered_samples.size() + ")");
            }
        }
    }

    private ObservableList<PlotSampleWrapper> showAlarmChanges() {
        final ObservableList<PlotSampleWrapper> new_samples = FXCollections.observableArrayList();
        Alarm alarm = Alarm.none();

        for (PlotSampleWrapper sample : this.samples) {
            Alarm value_alarm = Alarm.alarmOf(sample.getSample().getVType());
            if (! value_alarm.getSeverity().equals(alarm.getSeverity())) {
                alarm = value_alarm;
                new_samples.add(sample);
            }
        }

        return new_samples;
    }

    private void showThresholdCrossings(VType threshold_value) {
        // Goes through the samples if between two samples the threshold is crossed,
        // then the second sample is added to the list (After it passes the threshold)

        // Have to handle most Vtypes seperately. Only the numeric and ENUM types can be compared
        // TODO: strings and arrays not handled yet
    }

        // For also displaying the PVitem name in the list
    private static class PlotSampleWrapper {
            private final PlotSample sample;
            private final ModelItem model_item;

            public PlotSampleWrapper(final PlotSample sample, final ModelItem model_item) {
                this.sample = sample;
                this.model_item = model_item;
            }

            public PlotSample getSample() {
                return sample;
            }

            public ModelItem getModelItem() {
                return model_item;
            }

            public VType getVType() {
                return sample.getVType();
            }

            public String getSource() {
                return sample.getSource();
            }

            public String getPVName() {
                return model_item.getResolvedName();
            }

            @Override
            public String toString() {
                return sample.toString();
            }
        }

}
