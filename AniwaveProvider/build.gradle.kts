// use an integer for version numbers
version = 35


cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Watch Aniwave/9anime, also check out Enimax's app/chrome extension, He was a big help for fixing the extension, here is the link https://github.com/enimax-anime/enimax"
    authors = listOf("Stormunblessed, KillerDogeEmpire, Enimax, Chokerman")

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
