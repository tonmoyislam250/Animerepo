// use an integer for version numbers
version = 37


cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Watch Aniwave/9anime, I have had reports saying homepage doesn't work the first time but retrying should fix it"
    authors = listOf("Horis, Stormunblessed, KillerDogeEmpire, Enimax, Chokerman")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "Anime",
        "OVA",
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=aniwave.to&sz=%size%"
}