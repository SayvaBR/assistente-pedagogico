package com.sayvabr.assistentepedagogico.domain

/** UI stages; Fundamental year (1..9) is an attribute of a class, not a fourth stage. */
enum class EducationStage {
    EARLY_CHILDHOOD,
    ELEMENTARY,
    HIGH_SCHOOL,
}

/** Pure policy: never delete or hide existing classes when a subscription expires. */
object FreePlanPolicy {
    const val MAX_ACTIVE_CLASSES = 2

    fun mayCreateActiveClass(activeClasses: Int): Boolean {
        require(activeClasses >= 0) { "Active class count cannot be negative" }
        return activeClasses < MAX_ACTIVE_CLASSES
    }
}
