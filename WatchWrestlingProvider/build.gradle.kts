version = 9


cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Wrestling"
    authors = listOf("KillerDogeEmpire")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 0 // will be 3 if unspecified
    tvTypes = listOf(
        "TvSeries",
        "Movie"
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=watchwrestling.bz/&sz=%size%"
}
