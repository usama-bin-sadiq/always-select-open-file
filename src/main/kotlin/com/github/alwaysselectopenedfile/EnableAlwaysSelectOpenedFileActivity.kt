package com.github.alwaysselectopenedfile

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ToggleOptionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.WindowManager

/**
 * Turns on the Project View option "Always Select Opened File" for a project once it has opened.
 *
 * The option itself is owned by `com.intellij.ide.projectView.impl`, which is not public API and
 * must not be referenced (see the Internal API Migration guide). It is therefore driven through the
 * action the platform registers for it instead: [ToggleOptionAction] is public API, and its
 * `setSelected` is the supported entry point that persists the choice for the project.
 */
class EnableAlwaysSelectOpenedFileActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (project.isDisposed) return

        // The Project View is Swing UI, so hand off to the EDT before touching the action.
        ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                enableAlwaysSelectOpenedFile(project)
            }
        }
    }

    private fun enableAlwaysSelectOpenedFile(project: Project) {
        // Not every IntelliJ-based product ships the Project View, in which case there is
        // nothing to enable.
        val action = ActionManager.getInstance().getAction(ACTION_ID) as? ToggleOptionAction ?: return
        val frame = WindowManager.getInstance().getFrame(project) ?: return

        // A ToggleOptionAction resolves its option from the project carried by the event's data
        // context. That context has to come from the platform: DataContext itself is
        // @ApiStatus.Internal and @ApiStatus.NonExtendable, so it must not be implemented here.
        val dataContext = DataManager.getInstance().getDataContext(frame.contentPane)
        if (dataContext.getData(CommonDataKeys.PROJECT) == null) {
            LOG.warn("Cannot enable '$ACTION_ID': the data context has no project; leaving it unchanged")
            return
        }

        val event = AnActionEvent.createEvent(
            dataContext,
            Presentation(),
            PLACE,
            ActionUiKind.NONE,
            null,
        )

        // Only ever turn the option on. Leaving it alone when already enabled keeps this
        // idempotent, so reopening a project does not disturb an existing choice.
        if (!action.isSelected(event)) {
            action.setSelected(event, true)
        }
    }

    private companion object {
        /** Action registered for the "Always Select Opened File" Project View option. */
        const val ACTION_ID = "ProjectView.AutoscrollFromSource"
        const val PLACE = "AlwaysSelectOpenedFileActivity"

        val LOG = Logger.getInstance(EnableAlwaysSelectOpenedFileActivity::class.java)
    }
}