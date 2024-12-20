package org.akazukin.intellij.background.gui;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.impl.IdeBackgroundUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.akazukin.intellij.background.EditorBackgroundImage;
import org.akazukin.intellij.background.PluginHandler;
import org.akazukin.intellij.background.config.Config;
import org.akazukin.intellij.background.tasks.BackgroundScheduler;
import org.akazukin.intellij.background.tasks.SetRandomBackgroundTask;
import org.akazukin.intellij.background.utils.BundleUtils;
import org.jetbrains.annotations.NotNull;


public final class Settings implements Configurable {
    public static final TimeUnit[] TIME_UNITS = new TimeUnit[]{
        TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS
    };

    private JPanel rootPanel;
    private JCheckBox changeEveryButton;
    private JSpinner intervalSpinner;
    private ComboBox<String> timeUnitBox;
    private JCheckBox synchronizeImageButton;
    private JCheckBox editorButton;
    private JCheckBox frameButton;
    private JCheckBox hierarchicalButton;
    private JSpinner hierarchialSpinner;
    private Panel backgroundsListPanel;

    @Override
    public String getDisplayName() {
        return EditorBackgroundImage.PLUGIN_NAME_SPACE;
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        BackgroundScheduler.shutdown();

        intervalSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 2));
        changeEveryButton.addActionListener(e -> {
            intervalSpinner.setEnabled(changeEveryButton.isSelected());
            timeUnitBox.setEnabled(changeEveryButton.isSelected());
        });

        for (TimeUnit timeUnit : TIME_UNITS) {
            timeUnitBox.addItem(BundleUtils.message("settings.change.timeunit." + timeUnit.name().toLowerCase()));
        }


        editorButton.setText(IdeBundle.message("toggle.editor.and.tools"));
        frameButton.setText(IdeBundle.message("toggle.empty.frame"));


        hierarchicalButton.addActionListener(e -> {
            hierarchialSpinner.setEnabled(hierarchicalButton.isSelected());
        });
        hierarchialSpinner.setModel(new SpinnerNumberModel(3, 1, 10, 1));


        return rootPanel;
    }

    @Override
    public boolean isModified() {
        Config.State state = Config.getInstance();

        List<Pair<File, Boolean>> bgImgs = state.getImages().entrySet().stream()
            .map(e -> Pair.pair(new File(e.getKey()), e.getValue())).toList();

        return state.getIntervalAmount() != ((SpinnerNumberModel) intervalSpinner.getModel()).getNumber().intValue()
            || state.getIntervalUnit() != timeUnitBox.getSelectedIndex()

            || state.isChangeEditor() != editorButton.isSelected()
            || state.isChangeFrame() != frameButton.isSelected()

            || state.isChanges() != changeEveryButton.isSelected()
            || state.isSynchronizeImages() != synchronizeImageButton.isSelected()

            || state.isHierarchicalExplore() != hierarchicalButton.isSelected()
            || state.getHierarchicalDepth() != ((SpinnerNumberModel) hierarchialSpinner.getModel()).getNumber().intValue()

            || !new HashSet<>(bgImgs).containsAll(backgroundsListPanel.getData())
            || !new HashSet<>(backgroundsListPanel.getData()).containsAll(bgImgs);
    }

    @Override
    public void apply() {
        Config.State state = Config.getInstance();

        state.setIntervalAmount(((SpinnerNumberModel) intervalSpinner.getModel()).getNumber().intValue());
        state.setChanges(changeEveryButton.isSelected());

        state.setSynchronizeImages(synchronizeImageButton.isSelected());
        state.setIntervalUnit(timeUnitBox.getSelectedIndex());

        state.setChangeEditor(editorButton.isSelected());
        state.setChangeFrame(frameButton.isSelected());


        state.setHierarchicalExplore(hierarchicalButton.isSelected());
        state.setHierarchicalDepth(((SpinnerNumberModel) hierarchialSpinner.getModel()).getNumber().intValue());


        List<Pair<File, Boolean>> bgImgs = state.getImages().entrySet().stream()
            .map(e -> Pair.pair(new File(e.getKey()), e.getValue())).toList();
        if (!new HashSet<>(bgImgs).containsAll(backgroundsListPanel.getData())
            || !new HashSet<>(backgroundsListPanel.getData()).containsAll(bgImgs)) {
            EditorBackgroundImage.setImageCache(null);
        }
        state.setImages(
            Map.ofEntries(backgroundsListPanel.getData().stream()
                .map(e -> Map.entry(e.first.getAbsolutePath(), e.second))
                .toArray(Map.Entry[]::new))
        );

        intervalSpinner.setEnabled(changeEveryButton.isSelected());
        timeUnitBox.setEnabled(changeEveryButton.isSelected());
    }

    @Override
    public void reset() {
        Config.State state = Config.getInstance();

        changeEveryButton.setSelected(state.isChanges());

        intervalSpinner.setValue(state.getIntervalAmount());
        intervalSpinner.setEnabled(changeEveryButton.isSelected());


        timeUnitBox.setSelectedIndex(state.getIntervalUnit());
        timeUnitBox.setEnabled(changeEveryButton.isSelected());


        editorButton.setSelected(state.isChangeEditor());
        frameButton.setSelected(state.isChangeFrame());


        synchronizeImageButton.setSelected(state.isSynchronizeImages());


        hierarchicalButton.setSelected(state.isHierarchicalExplore());

        hierarchialSpinner.setValue(state.getHierarchicalDepth());
        hierarchialSpinner.setEnabled(hierarchicalButton.isSelected());


        List<Pair<File, Boolean>> bgImgs = new ArrayList<>(state.getImages().entrySet().stream()
            .map(e -> Pair.pair(new File(e.getKey()), e.getValue())).toList());
        backgroundsListPanel.setData(bgImgs);
    }

    @Override
    public void disposeUIResources() {
        if (!PluginHandler.isLoaded()) {
            return;
        }

        if (changeEveryButton.isSelected() && ((SpinnerNumberModel) intervalSpinner.getModel()).getNumber().intValue() > 0) {
            PropertiesComponent props = PropertiesComponent.getInstance();
            if (EditorBackgroundImage.getImageCache() == null
                || (editorButton.isSelected() && !props.isValueSet(IdeBackgroundUtil.EDITOR_PROP))
                || (frameButton.isSelected() && !props.isValueSet(IdeBackgroundUtil.FRAME_PROP))) {
                new SetRandomBackgroundTask().getAsBoolean();
            }
            BackgroundScheduler.schedule();
        }
    }
}
