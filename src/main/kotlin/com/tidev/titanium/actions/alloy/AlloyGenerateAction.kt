package com.tidev.titanium.actions.alloy

import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.tidev.titanium.TiIcons
import com.tidev.titanium.cli.AlloyCli
import com.tidev.titanium.project.TiProjectService
import com.tidev.titanium.util.TiNotifications
import com.tidev.titanium.util.TiVfs

/** What to generate: the positional CLI args and an optional file to open afterwards. */
data class GenSpec(val positional: List<String>, val openRelativePath: String?)

/**
 * Base action for `alloy generate <component> …`. Concrete subclasses fix the component and,
 * for models, override the prompt to collect adapter + schema.
 */
abstract class AlloyGenerateAction(protected val component: AlloyCli.Component) : AnAction(), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible =
            project != null && TiProjectService.getInstance(project).primary()?.isAlloy == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val tiProject = TiProjectService.getInstance(project).primary() ?: return
        val spec = prompt(project) ?: return
        runGenerate(project, tiProject.appDir, spec)
    }

    /** Default: prompt for a single name. Overridden by the model action. */
    protected open fun prompt(project: Project): GenSpec? {
        val name = Messages.showInputDialog(
            project,
            "${component.label} name:",
            "New Alloy ${component.label}",
            TiIcons.Titanium,
            "",
            NameValidator,
        )?.trim().orEmpty()
        if (name.isBlank()) return null
        return GenSpec(listOf(name), openPathFor(name))
    }

    protected fun openPathFor(name: String): String? = when (component) {
        AlloyCli.Component.CONTROLLER -> "controllers/$name.js"
        AlloyCli.Component.VIEW -> "views/$name.xml"
        AlloyCli.Component.STYLE -> "styles/$name.tss"
        AlloyCli.Component.MODEL -> "models/$name.js"
        AlloyCli.Component.WIDGET -> "widgets/$name/controllers/widget.js"
        AlloyCli.Component.MIGRATION -> null
    }

    private fun runGenerate(project: Project, appDir: String, spec: GenSpec) {
        object : Task.Backgroundable(project, "Generating Alloy ${component.label}", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val cmd = AlloyCli.generate(component, spec.positional, appDir)
                val result = try {
                    CapturingProcessHandler(cmd).runProcess(60_000)
                } catch (t: Throwable) {
                    TiNotifications.error(project, "Could not launch `alloy`. Is it installed? (npm i -g alloy)")
                    return
                }
                if (result.exitCode == 0) {
                    TiVfs.refresh(appDir)
                    spec.openRelativePath?.let { TiVfs.refreshAndOpen(project, "$appDir/$it") }
                    TiNotifications.info(project, "Generated Alloy ${component.label}: ${spec.positional.first()}")
                } else {
                    TiNotifications.warn(
                        project,
                        "alloy generate failed (exit ${result.exitCode}). ${result.stderr.take(200)}",
                    )
                }
            }
        }.queue()
    }

    protected object NameValidator : InputValidator {
        override fun checkInput(input: String?): Boolean = !input.isNullOrBlank()
        override fun canClose(input: String?): Boolean = checkInput(input)
    }
}

class AlloyGenerateControllerAction : AlloyGenerateAction(AlloyCli.Component.CONTROLLER)
class AlloyGenerateViewAction : AlloyGenerateAction(AlloyCli.Component.VIEW)
class AlloyGenerateStyleAction : AlloyGenerateAction(AlloyCli.Component.STYLE)
class AlloyGenerateMigrationAction : AlloyGenerateAction(AlloyCli.Component.MIGRATION)
class AlloyGenerateWidgetAction : AlloyGenerateAction(AlloyCli.Component.WIDGET)

/** Model generation needs an adapter (sql/properties) and an optional schema. */
class AlloyGenerateModelAction : AlloyGenerateAction(AlloyCli.Component.MODEL) {
    override fun prompt(project: Project): GenSpec? {
        val dialog = AlloyModelDialog(project)
        if (!dialog.showAndGet()) return null
        val positional = buildList {
            add(dialog.modelName)
            add(dialog.adapter)
            addAll(dialog.schemaTokens)
        }
        return GenSpec(positional, openPathFor(dialog.modelName))
    }
}
