package com.parhar.noor.data.admin

data class CategoryItem(
    val id: String = "",
    val category: String = "",
    val title: String = "",
)

data class Task(
    val category: String = "",
    val name: String = "",
    val points: Int = 0,
)

data class TaskItem(
    val id: String = "",
    val task: Task = Task(),
)
