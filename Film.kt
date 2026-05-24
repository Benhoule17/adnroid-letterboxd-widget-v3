package com.letterboxd.widget

data class Film(
    val slug: String,
    val posterUrl: String,
    val rating: Float = -1f   // -1 means no rating; favourites always -1
)
