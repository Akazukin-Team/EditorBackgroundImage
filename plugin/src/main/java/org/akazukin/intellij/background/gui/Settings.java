package org.akazukin.intellij.background.gui;import com.intellij.openapi.options.Configurable;import com.intellij.openapi.options.ConfigurationException;import com.intellij.openapi.ui.ComboBox;import com.intellij.openapi.util.Pair;import java.io.File;import java.util.ArrayList;import java.util.HashSet;import java.util.List;import java.util.Map;import java.util.concurrent.TimeUnit;import javax.swing.JCheckBox;import javax.swing.JComponent;import javax.swing.JPanel;import javax.swing.JSpinner;import javax.swing.SpinnerNumberModel;import org.akazukin.intellij.background.Config;import org.akazukin.intellij.background.EditorBackgroundImage;import org.akazukin.intellij.background.tasks.BackgroundScheduler;import org.jetbrains.annotations.NotNull;import org.jetbrains.annotations.Nullable;public final class Settings implements Configurable {    public static final TimeUnit[] TIME_UNITS = new TimeUnit[]{        TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS    };    private JPanel rootPanel;    private JCheckBox changeEveryButton;    private JSpinner intervalSpinner;    private ComboBox<String> timeUnitBox;    private JCheckBox synchronizeImageButton;    private JCheckBox editorButton;    private JCheckBox frameButton;    private Panel backgroundsListPanel;    @Override    public String getDisplayName() {        return EditorBackgroundImage.PLUGIN_NAME_SPACE;    }    @Nullable    @Override    public String getHelpTopic() {        return null;    }    @NotNull    @Override    public JComponent createComponent() {        BackgroundScheduler.shutdown();        intervalSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 2));        changeEveryButton.addActionListener(e -> {            intervalSpinner.setEnabled(changeEveryButton.isSelected());            timeUnitBox.setEnabled(changeEveryButton.isSelected());        });        for (final TimeUnit timeUnit : TIME_UNITS) {            timeUnitBox.addItem(timeUnit.name());        }        return rootPanel;    }    @Override    public boolean isModified() {        final Config.State state = Config.getInstance();        final List<Pair<File, Boolean>> bgImgs = state.getImages().entrySet().stream()            .map(e -> Pair.pair(new File(e.getKey()), e.getValue())).toList();        return state.getIntervalAmount() != ((SpinnerNumberModel) intervalSpinner.getModel()).getNumber().intValue()            || state.getIntervalUnit() != timeUnitBox.getSelectedIndex()            || state.isChangeEditor() != editorButton.isSelected()            || state.isChangeFrame() != frameButton.isSelected()            || state.isChanges() != changeEveryButton.isSelected()            || state.isSynchronizeImages() != synchronizeImageButton.isSelected()            || !new HashSet<>(bgImgs).containsAll(backgroundsListPanel.getData())            || !new HashSet<>(backgroundsListPanel.getData()).containsAll(bgImgs);    }    @Override    public void apply() throws ConfigurationException {        final Config.State state = Config.getInstance();        state.setIntervalAmount(((SpinnerNumberModel) intervalSpinner.getModel()).getNumber().intValue());        state.setChanges(changeEveryButton.isSelected());        state.setSynchronizeImages(synchronizeImageButton.isSelected());        state.setIntervalUnit(timeUnitBox.getSelectedIndex());        state.setChangeEditor(editorButton.isSelected());        state.setChangeFrame(frameButton.isSelected());        state.setImages(            Map.ofEntries(backgroundsListPanel.getData().stream()                .map(e -> Map.entry(e.first.getAbsolutePath(), e.second))                .toArray(Map.Entry[]::new))        );        intervalSpinner.setEnabled(changeEveryButton.isSelected());        timeUnitBox.setEnabled(changeEveryButton.isSelected());    }    @Override    public void reset() {        final Config.State state = Config.getInstance();        changeEveryButton.setSelected(state.isChanges());        intervalSpinner.setValue(state.getIntervalAmount());        intervalSpinner.setEnabled(changeEveryButton.isSelected());        timeUnitBox.setSelectedIndex(state.getIntervalUnit());        timeUnitBox.setEnabled(changeEveryButton.isSelected());        editorButton.setSelected(state.isChangeEditor());        frameButton.setSelected(state.isChangeFrame());        synchronizeImageButton.setSelected(state.isSynchronizeImages());        final List<Pair<File, Boolean>> bgImgs = new ArrayList<>(state.getImages().entrySet().stream()            .map(e -> Pair.pair(new File(e.getKey()), e.getValue())).toList());        backgroundsListPanel.setData(bgImgs);    }    @Override    public void disposeUIResources() {        final boolean autoChange = changeEveryButton.isSelected();        final int interval = ((SpinnerNumberModel) intervalSpinner.getModel()).getNumber().intValue();        EditorBackgroundImage.setImageCache(null);        if (autoChange && interval > 0) {            BackgroundScheduler.schedule();        } else {            BackgroundScheduler.shutdown();        }    }}