package com.writingapp.ui.editor

data class DocumentTemplate(
    val name: String,
    val description: String,
    val content: String
)

object DocumentTemplates {
    val blank = DocumentTemplate(
        name = "Blank",
        description = "Start with an empty document",
        content = ""
    )

    val note = DocumentTemplate(
        name = "Quick Note",
        description = "A simple note with date",
        content = """# Note - ${java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}

## Key Points

- 

## Details



## Action Items

- [ ] 
"""
    )

    val novel = DocumentTemplate(
        name = "Novel Chapter",
        description = "Chapter template for fiction writing",
        content = """# Chapter ${1}

## Outline

- 

## Scene 1



## Scene 2



## Notes

- 
"""
    )

    val meeting = DocumentTemplate(
        name = "Meeting Notes",
        description = "Structured meeting minutes",
        content = """# Meeting: ${java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}

## Attendees

- 

## Agenda

1. 
2. 
3. 

## Discussion



## Action Items

| Task | Owner | Due Date |
|------|-------|----------|
|      |       |          |
"""
    )

    val journal = DocumentTemplate(
        name = "Journal Entry",
        description = "Daily journal entry",
        content = """# ${java.text.SimpleDateFormat("EEEE, MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}

## Today's Highlights

- 

## Gratitude

1. 
2. 
3. 

## Reflections



## Tomorrow

- 
"""
    )

    val all = listOf(blank, note, novel, meeting, journal)
}
