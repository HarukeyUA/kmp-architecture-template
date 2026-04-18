package org.example.project

import org.gradle.api.initialization.ProjectDescriptor

/** Every leaf (childless) descendant, excluding the root project itself. */
internal fun ProjectDescriptor.leafDescendants(): List<ProjectDescriptor> {
    val leaves = mutableListOf<ProjectDescriptor>()
    fun visit(d: ProjectDescriptor) {
        if (d.children.isEmpty()) {
            if (d.parent != null) leaves += d
        } else {
            d.children.forEach(::visit)
        }
    }
    visit(this)
    return leaves
}
