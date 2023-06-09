package com.jfrog.conan.clionplugin.conan.extensions


import com.intellij.openapi.project.Project
import com.jetbrains.cidr.cpp.cmake.CMakeRunnerStep
import com.jetbrains.cidr.cpp.cmake.CMakeRunnerStep.Parameters
import com.jetbrains.cidr.cpp.cmake.CMakeSettings
import com.jetbrains.rd.util.string.printToString

class MyCMakeRunnerStep : CMakeRunnerStep {
        override fun beforeGeneration(project: Project, parameters: Parameters) {
                val profileName = parameters.getUserData(Parameters.PROFILE_NAME)
                val cmakeSettings = CMakeSettings.getInstance(project)
                val profile = cmakeSettings.profiles.find { it.name == profileName }
                val compiler = profile?.toolchainName
                val configuration = profile?.buildType
                println("configuration------->>> ${compiler}, ${configuration}, ${profile?.generationOptions}")
                println(profile.printToString())

                // inject parameters:
                // val modifiedParameters = parameters.withParameters(parameters.parameters + "--my-argument")


        }

        override fun modifyParameters(project: Project, parameters: Parameters): Parameters {
                // Modifica los parámetros de CMake según sea necesario
                // Puedes acceder a los parámetros y al proyecto para realizar las modificaciones requeridas
                println("modifyParameters")
                val profileName = parameters.getUserData(Parameters.PROFILE_NAME)
                val cmakeSettings = CMakeSettings.getInstance(project)
                val profile = cmakeSettings.profiles.find { it.name == profileName }
                val compiler = profile?.toolchainName
                val configuration = profile?.buildType
                println(profile.printToString())
                println("configuration------->>> ${compiler}, ${configuration}, ${profile?.generationOptions}")
                // inject parameters:
                // val modifiedParameters = parameters.withParameters(parameters.parameters + "--my-argument")
                return parameters
        }
}