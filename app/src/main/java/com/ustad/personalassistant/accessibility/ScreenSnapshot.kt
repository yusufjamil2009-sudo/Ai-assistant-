package com.ustad.personalassistant.accessibility

data class ScreenSnapshot(
    val packageName: String?,
    val visibleText: List<String>,
    val contentDescriptions: List<String>,
    val clickableElements: List<String>,
    val editableFields: List<String>,
    val buttons: List<String>,
    val scrollableContainers: Int,
    val focusedElement: String?
)
