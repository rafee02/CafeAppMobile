package com.example.cafeapp

data class MenuItem(
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageName: String = ""  // String to store image name, e.g., "espresso"
)
