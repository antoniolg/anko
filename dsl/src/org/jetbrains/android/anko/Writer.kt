/*
 * Copyright 2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.android.anko

import org.jetbrains.android.anko
import org.jetbrains.android.anko.config
import java.io.PrintWriter
import java.io.File
import java.util.ArrayList
import org.jetbrains.android.anko.config.AnkoFile.*
import org.jetbrains.android.anko.config.AnkoFile
import org.jetbrains.android.anko.config.ConfigurationTune.*
import org.jetbrains.android.anko.config.Props

class Writer(private val renderer: Renderer) {

    val config = renderer.config

    fun write() {
        for (file in AnkoFile.values()) {
            when (file) {
                AnkoFile.VIEWS -> { if (config[VIEWS] || config[HELPER_CONSTRUCTORS]) writeViews() }
                AnkoFile.INTERFACE_WORKAROUNDS -> { if (config[INTERFACE_WORKAROUNDS]) writeInterfaceWorkarounds() }

                AnkoFile.LAYOUTS -> { if (config[LAYOUTS]) writeLayouts() }
                AnkoFile.LISTENERS -> { if (config[LISTENERS]) writeListeners() }
                AnkoFile.PROPERTIES -> { if (config[PROPERTIES]) writeProperties() }
                AnkoFile.SERVICES -> { if (config[SERVICES]) writeServices() }
                AnkoFile.SQL_PARSER_HELPERS -> { if (config[SQL_PARSER_HELPERS]) writeSqlParserHelpers() }

                else -> if (config[file]) writeStatic(file)
            }
        }
    }

    fun writeInterfaceWorkarounds() {
        val imports = "package ${config.outputPackage}.workarounds;"
        val contents = listOf(renderer.interfaceWorkarounds)
        writeToFile(config.getOutputFile(anko.config.AnkoFile.INTERFACE_WORKAROUNDS), contents, imports, false)
    }

    private fun writeLayouts() {
        val imports = Props.imports["layouts"] ?: ""
        writeToFile(config.getOutputFile(anko.config.AnkoFile.LAYOUTS), renderer.layouts, imports)
    }

    private fun writeListeners() {
        val allListeners = ArrayList<String>()

        array(
            SIMPLE_LISTENERS to renderer.simpleListeners,
            COMPLEX_LISTENER_CLASSES to renderer.complexListenerClasses,
            COMPLEX_LISTENER_SETTERS to renderer.complexListenerSetters
        ).forEach { if (config[it.first]) allListeners.addAll(it.second) }

        if (allListeners.isNotEmpty())
            writeToFile(config.getOutputFile(anko.config.AnkoFile.LISTENERS), allListeners)
    }

    private fun writeProperties() {
        writeToFile(config.getOutputFile(anko.config.AnkoFile.PROPERTIES), renderer.properties)
    }

    private fun writeServices() {
        val imports = Props.imports["services"] ?: ""
        writeToFile(config.getOutputFile(anko.config.AnkoFile.SERVICES), renderer.services, imports)
    }

    private fun writeSqlParserHelpers() {
        val imports = Props.imports["sqliteparserhelpers"] ?: ""
        writeToFile(config.getOutputFile(anko.config.AnkoFile.SQL_PARSER_HELPERS), renderer.sqLiteParserHelpers, imports, false)
    }

    private fun writeViews() {
        val allViews = arrayListOf<String>()

        if (config[VIEWS]) {
            allViews.addAll(renderer.views)
            allViews.addAll(renderer.viewGroups)
        }

        if (config[HELPER_CONSTRUCTORS])
            allViews.addAll(renderer.helperConstructors)

        val imports = Props.imports["views"] ?: ""
        writeToFile(config.getOutputFile(anko.config.AnkoFile.VIEWS), allViews, imports)
    }

    private fun writeStatic(subsystem: config.AnkoFile) {
        val file = File("dsl/static/src/${subsystem.filename}")
        val contents = file.readLines()
        writeToFile(config.getOutputFile(subsystem), contents, "", false)
    }

    private fun writeToFile(file: File, text: Collection<String>, imports: String = "", generatePackage: Boolean = true) {
        val dir = file.getParentFile()
        if (!dir.exists())
            dir.mkdirs()

        val writer = PrintWriter(file)
        if (config.generatePackage && generatePackage) {
            writer.println("package ${config.outputPackage}\n")
        }
        if (config.generateImports && imports.isNotEmpty()) {
            writer.println(imports)
            writer.println()
        }
        for (line in text) {
            writer.println(line)
        }
        writer.close()
    }

}