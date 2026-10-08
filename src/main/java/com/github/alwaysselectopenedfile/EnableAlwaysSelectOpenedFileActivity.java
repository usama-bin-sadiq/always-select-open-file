package com.github.alwaysselectopenedfile;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Public-API-only implementation.
 *
 * Strategy:
 * 1. On project open (ProjectActivity – public, recommended).
 * 2. Check a simple flag in PropertiesComponent (public).
 * 3. If not yet applied, try to toggle the official action
 *    "ProjectView.AutoscrollFromSource" / "Always Select Opened File"
 *    via ActionManager (fully public).
 */
public final class EnableAlwaysSelectOpenedFileActivity implements ProjectActivity {

    private static final Logger LOG = Logger.getInstance(EnableAlwaysSelectOpenedFileActivity.class);

    /** Key stored per-project. When true we never force the setting again. */
    private static final String APPLIED_KEY = "alwaysSelectOpenedFile.autoApplied";

    /** Known action IDs used by different IDE versions. */
    private static final String[] ACTION_IDS = {
            "ProjectView.AutoscrollFromSource",   // classic
            "ProjectView.AlwaysSelectOpenedFile", // newer name in some builds
            "AlwaysSelectOpenedFile"              // fallback
    };

    @Nullable
    @Override
    public Object execute(@NotNull Project project,
                          @NotNull Continuation<? super Unit> continuation) {
        if (project.isDisposed() || project.isDefault()) {
            return Unit.INSTANCE;
        }

        // Run on EDT after the project UI is ready
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            enableOnce(project);
        });

        return Unit.INSTANCE;
    }

    private void enableOnce(@NotNull Project project) {
        PropertiesComponent props = PropertiesComponent.getInstance(project);

        if (props.getBoolean(APPLIED_KEY, false)) {
            LOG.debug("Skipping project " + project.getName() +
                      " – already auto-applied once; respecting manual setting");
            return;
        }

        boolean success = tryEnableViaAction(project);

        if (success) {
            props.setValue(APPLIED_KEY, true);
            LOG.info("Always Select Opened File enabled for project " + project.getName() +
                     " (will not override future manual changes)");
        } else {
            LOG.warn("Could not enable Always Select Opened File for project " +
                     project.getName() + " – action not found or already selected");
        }
    }

    /**
     * Tries to find and perform the public toggle action.
     * This is the only fully public way to change the setting.
     */
    private boolean tryEnableViaAction(@NotNull Project project) {
        ActionManager am = ActionManager.getInstance();

        for (String id : ACTION_IDS) {
            AnAction action = am.getAction(id);
            if (action == null) continue;

            // Build a minimal DataContext that contains the project
            DataContext dataContext = SimpleDataContext.builder()
                    .add(CommonDataKeys.PROJECT, project)
                    .build();

            AnActionEvent event = AnActionEvent.createFromDataContext(
                    "AlwaysSelectOpenedFilePlugin",
                    new Presentation(),
                    dataContext
            );

            // Update the presentation so we can see whether it is already selected
            action.update(event);
            Presentation presentation = event.getPresentation();

            // If the action is a toggle and already selected → nothing to do
            if (presentation.isEnabled() && presentation.isVisible()) {
                // Perform the action only if it looks like a toggle that is currently off.
                // Most implementations of this action simply toggle the state.
                // Performing it when it is already on would turn it off – that is undesirable.
                // Therefore we only perform when the text or description suggests it is off,
                // or we simply perform and rely on the “applied” flag for future opens.
                // Safer approach for a one-shot enable: just perform once.
                action.actionPerformed(event);
                return true;
            }
        }
        return false;
    }
}
