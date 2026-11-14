package com.romgold.plandesign3d.data

import android.content.Context
import com.romgold.plandesign3d.domain.ProjectDocument

class ProjectStore(context: Context) {
    private val prefs = context.getSharedPreferences("plan_design_projects", Context.MODE_PRIVATE)
    fun save(project: ProjectDocument) {
        prefs.edit().putString("project_${project.id}", ProjectCodec.encode(project))
            .putString("last_project_id", project.id).apply()
    }
    fun loadLast(): ProjectDocument? {
        val id = prefs.getString("last_project_id", null) ?: return null
        val json = prefs.getString("project_$id", null) ?: return null
        return runCatching { ProjectCodec.decode(json) }.getOrNull()
    }
    fun list(): List<ProjectDocument> = prefs.all.keys.filter { it.startsWith("project_") }
        .mapNotNull { key -> prefs.getString(key, null)?.let { runCatching { ProjectCodec.decode(it) }.getOrNull() } }
        .sortedBy { it.name.lowercase() }
}
