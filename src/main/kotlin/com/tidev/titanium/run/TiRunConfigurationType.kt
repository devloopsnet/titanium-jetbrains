package com.tidev.titanium.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.tidev.titanium.TiIcons
import com.tidev.titanium.TitaniumBundle

/** Registers the "Titanium" run configuration type. */
class TiRunConfigurationType : ConfigurationTypeBase(
    ID,
    TitaniumBundle.message("run.config.name"),
    TitaniumBundle.message("run.config.description"),
    TiIcons.Titanium,
) {
    init {
        addFactory(TiRunConfigurationFactory(this))
    }

    companion object {
        const val ID = "TitaniumBuildConfiguration"
    }
}

/** Factory producing [TiRunConfiguration] instances bound to [TiRunConfigurationOptions]. */
class TiRunConfigurationFactory(type: TiRunConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String = "Titanium"

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        TiRunConfiguration(project, this, "Titanium")

    override fun getOptionsClass(): Class<TiRunConfigurationOptions> = TiRunConfigurationOptions::class.java
}
