version = 4


cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Football recordings"
    authors = listOf("KillerDogeEmpire")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "TvSeries",
        "Movie"
    )

    iconUrl = "https://footyfull.com/wp-content/uploads/2021/06/logo2x.png"
}
