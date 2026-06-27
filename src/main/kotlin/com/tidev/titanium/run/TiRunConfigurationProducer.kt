package com.tidev.titanium.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/** Creates a Titanium run configuration from a right-click on `tiapp.xml`. */
class TiRunConfigurationProducer : LazyRunConfigurationProducer<TiRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory =
        ConfigurationTypeUtil.findConfigurationType(TiRunConfigurationType::class.java)
            .configurationFactories.first()

    override fun setupConfigurationFromContext(
        configuration: TiRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val file = context.psiLocation?.containingFile ?: return false
        if (!file.isTiapp()) return false
        val dir = file.virtualFile?.parent?.path ?: return false
        configuration.tiOptions.projectDir = dir
        configuration.name = file.virtualFile?.parent?.name ?: "Titanium"
        return true
    }

    override fun isConfigurationFromContext(
        configuration: TiRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val file = context.psiLocation?.containingFile ?: return false
        if (!file.isTiapp()) return false
        val dir = file.virtualFile?.parent?.path ?: return false
        return configuration.tiOptions.projectDir == dir
    }

    private fun PsiFile.isTiapp(): Boolean = name == "tiapp.xml"
}
