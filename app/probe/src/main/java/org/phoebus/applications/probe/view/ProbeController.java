package org.phoebus.applications.probe.view;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.applications.probe.Probe;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.ui.pv.SeverityColors;
import org.phoebus.util.time.TimestampFormats;
import org.phoebus.vtype.Alarm;
import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.SimpleValueFormat;
import org.phoebus.vtype.Time;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueFormat;
import org.phoebus.vtype.ValueUtil;

import io.reactivex.disposables.Disposable;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class ProbeController {

    private ValueFormat valueFormat = new SimpleValueFormat(3);

    private boolean editing = false;

    @FXML
    private TextField txtPVName;
    @FXML
    private TextField txtValue;
    @FXML
    private TextField txtAlarm;
    @FXML
    private TextField txtTimeStamp;

    public TextField getPVField() {
        return txtPVName;
    }

    public String getPVName() {
        return txtPVName.getText();
    }

    public void setPVName(String pvName) {
        txtPVName.setText(pvName);
        search();
    }

    private void setEditing(final boolean editing)
    {
        if (editing == this.editing)
            return;
        this.editing = editing;
        if (editing)
            txtValue.setStyle("-fx-control-inner-background: #FFFF00;");
        else
        {
            txtValue.setStyle("");
            // Restore current value
            // (which might soon be replaced by update from PV if we're writing)
            update(pv.read());
        }
    }

    @FXML
    public void initialize() {
        txtPVName.setPromptText("Enter PV Name");

        // Write entered value to PV
        txtValue.setOnKeyPressed(event ->
        {
            switch (event.getCode())
            {
            case ESCAPE:
                setEditing(false);
                break;
            case ENTER:
                final String entered = txtValue.getText();
                setEditing(false);
                // Write entered value to PV.
                // If PV accepts the value, it will send an update
                try
                {
                    pv.write(entered);
                }
                catch (Exception ex)
                {
                    Logger.getLogger(Probe.class.getPackageName())
                    .log(Level.WARNING, "Cannot write '" + entered + "' to PV " + pv.getName(), ex);
                }
                break;
            default:
                setEditing(true);
            }
        });
        txtValue.setOnMouseClicked(event -> setEditing(true));
        txtValue.focusedProperty().addListener((p, old, focus) ->
        {
            if (!focus)
                setEditing(false);
        });

        txtAlarm.setEditable(false);
        txtTimeStamp.setEditable(false);

        // register selection listener
//        SelectionService.getInstance().addListener(new SelectionChangeListener() {
//            @Override
//            public void selectionChanged(Object source, Selection oldValue, Selection newValue) {
//                if (source.equals(txtPVName)) {
//                    System.out.println("I set the selection to : " + newValue);
//                } else {
//                    System.out.println("The new selection is : " + newValue);
//                }
//            }
//        });
    }

    private PV pv;
    private Disposable pv_flow;

    private void update(final VType value)
    {
        Platform.runLater(() ->
        {
            setValue(value);
        });
    }

    @FXML
    private void search() {
        // The PV is different, so disconnect and reset the visuals
        if (pv != null) {
            pv_flow.dispose();
            PVPool.releasePV(pv);
            pv = null;
        }

        // search for pv, unless empty
        if (txtPVName.getText().isEmpty())
            return;

        SelectionService.getInstance().setSelection(txtPVName, Arrays.asList(new ProcessVariable(txtPVName.getText())));

        try
        {
            pv = PVPool.getPV(txtPVName.getText());
            pv_flow = pv.onValueEvent()
                        .throttleLatest(10, TimeUnit.MILLISECONDS)
                        .subscribe(this::update);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void setValue(final VType value) {
        if (editing)
            return;

        String valueString = null;
        if (value != null)
            valueString = valueFormat.format(value);
        if (valueString != null)
            txtValue.setText(valueString);
        else
            txtValue.setText("<null>");
        setTime(ValueUtil.timeOf(value));
        setAlarm(ValueUtil.alarmOf(value, value != null));
    }

    private void setTime(final Time time) {
        if (time != null) {
            txtTimeStamp.setText(TimestampFormats.FULL_FORMAT.format(time.getTimestamp()));
        } else {
            txtTimeStamp.setText(""); //$NON-NLS-1$
        }
    }

    private void setAlarm(final Alarm alarm)
    {
        if (alarm == null  ||  alarm.getAlarmSeverity() == AlarmSeverity.NONE)
            txtAlarm.setText("");
        else
        {
            final Color col = SeverityColors.getTextColor(alarm.getAlarmSeverity());
            txtAlarm.setStyle("-fx-text-fill: rgba(" + (int)(col.getRed()*255) + ',' +
                                                       (int)(col.getGreen()*255) + ',' +
                                                       (int)(col.getBlue()*255) + ',' +
                                                             col.getOpacity()*255 + ");");
            txtAlarm.setText(alarm.getAlarmSeverity() + " - " + alarm.getAlarmName());
        }
    }
}
