package eu.kanade.tachiyomi.extension.th.speedmanga

import eu.kanade.tachiyomi.source.model.Filter

val orderOptions = arrayOf(
    Pair("Default", ""),
    Pair("Popular", "popular"),
    Pair("Latest Update", "update"),
    Pair("New Added", "latest"),
    Pair("A-Z", "title"),
    Pair("Z-A", "titlereverse"),
)

val statusOptions = arrayOf(
    Pair("All", ""),
    Pair("Ongoing", "ongoing"),
    Pair("Completed", "completed"),
    Pair("Hiatus", "hiatus"),
)

val typeOptions = arrayOf(
    Pair("All", ""),
    Pair("Manga", "manga"),
    Pair("Manhwa", "manhwa"),
    Pair("Manhua", "manhua"),
    Pair("Comic", "comic"),
    Pair("Novel", "novel"),
)

class OrderFilter :
    Filter.Select<String>(
        "Order By",
        orderOptions.map { it.first }.toTypedArray(),
    )

class StatusFilter :
    Filter.Select<String>(
        "Status",
        statusOptions.map { it.first }.toTypedArray(),
    )

class TypeFilter :
    Filter.Select<String>(
        "Type",
        typeOptions.map { it.first }.toTypedArray(),
    )

class Genre(name: String, val value: String) : Filter.CheckBox(name)

class GenreFilter :
    Filter.Group<Genre>(
        "Genres",
        listOf(
            Genre("Action", "13"),
            Genre("Adventure", "19"),
            Genre("Comedy", "26"),
            Genre("Drama", "5"),
            Genre("Ecchi", "123"),
            Genre("Fantasy", "14"),
            Genre("Harem", "29"),
            Genre("Historical", "15"),
            Genre("Horror", "6"),
            Genre("Isekai", "127"),
            Genre("Josei", "27"),
            Genre("Manhua", "7"),
            Genre("Manhwa", "475"),
            Genre("Martial Arts", "16"),
            Genre("Mature", "8"),
            Genre("Mecha", "516"),
            Genre("Mystery", "9"),
            Genre("One Shot", "582"),
            Genre("Psychological", "11"),
            Genre("Romance", "20"),
            Genre("School Life", "84"),
            Genre("Sci-fi", "35"),
            Genre("Seinen", "10"),
            Genre("Shoujo", "31"),
            Genre("Shounen", "17"),
            Genre("Slice of Life", "97"),
            Genre("Smut", "77"),
            Genre("Sports", "258"),
            Genre("Supernatural", "56"),
            Genre("Tragedy", "100"),
            Genre("Yaoi", "365"),
            Genre("Yuri", "53"),
        ),
    )
