package dk.sonderborg.borger

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Elderly
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Faste oplysninger i appen.
 *
 * Kommunens egne sider ændrer adresse jævnligt, så emner linker til en søgning på kommunens
 * hjemmeside i stedet for til faste undersider. Nationale selvbetjeningsløsninger linkes direkte.
 */
object Content {

    const val KOMMUNE_SITE = "https://sonderborgkommune.dk"
    const val DAGSORDENER = "https://dagsordener.sonderborgkommune.dk"

    const val BORGERSERVICE_PHONE = "88 72 64 00"
    const val BORGERSERVICE_EMAIL = "borgerservice@sonderborg.dk"
    const val BORGERSERVICE_ADDRESS = "Borgen, Holger Drachmanns Plads 5, 6400 Sønderborg"

    /** Søg på kommunens hjemmeside (via websøgning, så det virker uanset sidens struktur). */
    fun kommuneSearch(query: String): String =
        "https://www.google.com/search?q=" + Uri.encode("site:sonderborgkommune.dk $query")

    val defaultSources = listOf(
        FeedSource(
            id = "kommune",
            name = "Sønderborg Kommune",
            // Forsiden: appen finder selv sidens RSS-feed, hvis den har et.
            url = KOMMUNE_SITE,
            builtIn = true,
        ),
        FeedSource(
            id = "sonderborgnyt",
            name = "SønderborgNyt",
            url = "https://www.sonderborgnyt.dk/feed/",
            builtIn = true,
        ),
        FeedSource(
            id = "drsyd",
            name = "DR Syd",
            url = "https://www.dr.dk/nyheder/service/feeds/regionale/syd",
            localFilter = true,
            builtIn = true,
        ),
        FeedSource(
            id = "drseneste",
            name = "DR Seneste nyt",
            url = "https://www.dr.dk/nyheder/service/feeds/senestenyt",
            enabled = false,
            builtIn = true,
        ),
    )

    data class Link(val title: String, val url: String, val subtitle: String? = null)

    data class Topic(
        val id: String,
        val title: String,
        val icon: ImageVector,
        val intro: String,
        val tips: List<String>,
        val links: List<Link>,
        val searchTerms: List<String>,
    )

    val topics = listOf(
        Topic(
            id = "borgerservice",
            title = "Borgerservice & Digital Post",
            icon = Icons.Outlined.AccountBalance,
            intro = "Flytning, MitID, Digital Post, sygesikringskort og valg af læge klares som regel online. Borgerservice hjælper, hvis du ikke kan selv.",
            tips = listOf(
                "Meld flytning senest 5 dage efter, du er flyttet.",
                "Husk MitID, når du betjener dig selv på borger.dk.",
                "Har du brug for hjælp til det digitale, kan du få hjælp i Borgerservice.",
            ),
            links = listOf(
                Link("borger.dk", "https://www.borger.dk", "Selvbetjening for alle offentlige områder"),
                Link("MitID", "https://www.mitid.dk", "Login, app og hjælp"),
                Link("mit.dk – Digital Post", "https://mit.dk", "Læs post fra det offentlige"),
            ),
            searchTerms = listOf("borgerservice", "flytning", "digital post"),
        ),
        Topic(
            id = "pas",
            title = "Pas, kørekort & ID",
            icon = Icons.Outlined.Badge,
            intro = "Pas og kørekort bestilles i Borgerservice. Book tid på kommunens hjemmeside, og husk billede og gammelt pas/kørekort.",
            tips = listOf(
                "Tjek udløbsdatoen på dit pas i god tid før en rejse.",
                "Du kan bestille digitalt kørekort i Kørekort-appen.",
            ),
            links = listOf(
                Link("Pas og kørekort på borger.dk", "https://www.borger.dk"),
                Link("Book tid i Borgerservice", kommuneSearch("book tid pas kørekort")),
            ),
            searchTerms = listOf("pas", "kørekort", "book tid"),
        ),
        Topic(
            id = "affald",
            title = "Affald & genbrug",
            icon = Icons.Outlined.Delete,
            intro = "Sønderborg Forsyning står for tømning af affaldsbeholdere og driver genbrugspladserne i kommunen.",
            tips = listOf(
                "Se din tømmekalender og bestil storskrald hos Sønderborg Forsyning.",
                "Sorter i de rigtige fraktioner – det sparer både miljø og penge.",
            ),
            links = listOf(
                Link("Sønderborg Forsyning", "https://www.sonfor.dk", "Tømning, genbrugspladser, vand og varme"),
                Link("Affald på kommunens side", kommuneSearch("affald genbrugsplads")),
            ),
            searchTerms = listOf("affald", "genbrugsplads", "storskrald"),
        ),
        Topic(
            id = "born",
            title = "Børn, skole & dagtilbud",
            icon = Icons.Outlined.School,
            intro = "Skriv dit barn op til vuggestue, børnehave og skole, og følg med i hverdagen via Aula.",
            tips = listOf(
                "Pladsanvisningen hjælper med dagpleje, vuggestue og børnehave.",
                "Skoleindskrivning sker typisk om vinteren året før skolestart.",
            ),
            links = listOf(
                Link("Aula", "https://www.aula.dk", "Skole og dagtilbud"),
                Link("Pladsanvisning", kommuneSearch("pladsanvisning")),
                Link("Skoleindskrivning", kommuneSearch("skoleindskrivning")),
            ),
            searchTerms = listOf("dagtilbud", "skole", "SFO"),
        ),
        Topic(
            id = "familie",
            title = "Familie & sociale forhold",
            icon = Icons.Outlined.FamilyRestroom,
            intro = "Hjælp til familier, handicap, misbrug og psykisk sårbarhed. Er du bekymret for et barn, så kontakt kommunen.",
            tips = listOf(
                "Er et barn i akut fare, så ring 112.",
                "Bekymring for et barn kan også meldes anonymt.",
            ),
            links = listOf(
                Link("Underretning om et barn", kommuneSearch("underretning bekymring barn")),
                Link("Familie og børn på borger.dk", "https://www.borger.dk"),
            ),
            searchTerms = listOf("familie", "handicap", "underretning"),
        ),
        Topic(
            id = "sundhed",
            title = "Sundhed",
            icon = Icons.Outlined.LocalHospital,
            intro = "Find din læge, se journal og prøvesvar, og brug kommunens sundhedstilbud som rygestop og genoptræning.",
            tips = listOf(
                "Lægevagten i Region Syddanmark: 70 11 07 07 (uden for lægens åbningstid).",
                "Ved livsfare: ring 112.",
            ),
            links = listOf(
                Link("sundhed.dk", "https://www.sundhed.dk", "Journal, prøvesvar og lægevalg"),
                Link("Kommunens sundhedstilbud", kommuneSearch("sundhedstilbud rygestop genoptræning")),
            ),
            searchTerms = listOf("sundhed", "genoptræning", "rygestop"),
        ),
        Topic(
            id = "aeldre",
            title = "Ældre & pleje",
            icon = Icons.Outlined.Elderly,
            intro = "Hjemmehjælp, hjælpemidler, plejebolig, madservice og aktiviteter for ældre.",
            tips = listOf(
                "Kontakt visitationen, hvis du eller en pårørende får brug for hjælp i hverdagen.",
                "Pension søges på borger.dk.",
            ),
            links = listOf(
                Link("Hjemmehjælp og visitation", kommuneSearch("hjemmehjælp visitation")),
                Link("Hjælpemidler", kommuneSearch("hjælpemidler")),
                Link("Pension på borger.dk", "https://www.borger.dk"),
            ),
            searchTerms = listOf("ældre", "hjemmehjælp", "plejebolig"),
        ),
        Topic(
            id = "job",
            title = "Job, ydelser & uddannelse",
            icon = Icons.Outlined.Work,
            intro = "Jobcenter Sønderborg hjælper ledige og sygemeldte. Ydelser som kontanthjælp og sygedagpenge søges digitalt.",
            tips = listOf(
                "Tilmeld dig som jobsøgende på jobnet.dk første ledighedsdag.",
                "Ledige job i kommunen findes på kommunens hjemmeside.",
            ),
            links = listOf(
                Link("Jobnet", "https://www.jobnet.dk", "Tilmelding og jobsøgning"),
                Link("Jobcenter Sønderborg", kommuneSearch("jobcenter")),
                Link("Ledige stillinger i kommunen", kommuneSearch("ledige stillinger")),
                Link("SU", "https://www.su.dk"),
            ),
            searchTerms = listOf("jobcenter", "kontanthjælp", "sygedagpenge"),
        ),
        Topic(
            id = "bolig",
            title = "Bolig, byggeri & ejendom",
            icon = Icons.Outlined.Home,
            intro = "Byggetilladelser søges digitalt. Kommunen behandler også lokalplaner, ejendomsskat og boligstøtte.",
            tips = listOf(
                "Mange mindre byggerier kræver ikke tilladelse – tjek reglerne først.",
                "Boligstøtte søges hos Udbetaling Danmark via borger.dk.",
            ),
            links = listOf(
                Link("Byg og Miljø", "https://www.bygogmiljoe.dk", "Ansøg om byggetilladelse"),
                Link("Lokalplaner og høringer", kommuneSearch("lokalplan høring")),
                Link("Boligstøtte på borger.dk", "https://www.borger.dk"),
            ),
            searchTerms = listOf("byggetilladelse", "lokalplan", "ejendomsskat"),
        ),
        Topic(
            id = "trafik",
            title = "Trafik, veje & transport",
            icon = Icons.Outlined.DirectionsBus,
            intro = "Planlæg din rejse med bus og tog, og giv kommunen besked om huller i vejen, væltede skilte eller manglende gadelys.",
            tips = listOf(
                "Brug Rejseplanen til bus og tog på Als og i Sønderjylland.",
                "Fejl på veje og stier kan meldes til kommunen.",
            ),
            links = listOf(
                Link("Rejseplanen", "https://www.rejseplanen.dk"),
                Link("Sydtrafik", "https://www.sydtrafik.dk", "Busser i Syddanmark"),
                Link("Meld fejl på vej", kommuneSearch("giv et tip fejl vej")),
            ),
            searchTerms = listOf("vejarbejde", "trafik", "snerydning"),
        ),
        Topic(
            id = "kultur",
            title = "Kultur, fritid & oplevelser",
            icon = Icons.Outlined.Museum,
            intro = "Biblioteker, museer, idræt, foreninger og events i Sønderborg Kommune.",
            tips = listOf(
                "Se events og oplevelser på VisitSønderborg.",
                "Kommunen giver tilskud til foreninger og kulturprojekter.",
            ),
            links = listOf(
                Link("VisitSønderborg", "https://www.visitsonderborg.dk", "Oplevelser og events"),
                Link("Bibliotek", kommuneSearch("bibliotek")),
                Link("Foreninger og tilskud", kommuneSearch("foreninger tilskud")),
            ),
            searchTerms = listOf("kultur", "bibliotek", "fritid"),
        ),
        Topic(
            id = "klima",
            title = "Klima & natur",
            icon = Icons.Outlined.Eco,
            intro = "Sønderborg arbejder mod at blive CO2-neutral gennem ProjectZero. Find også naturområder, strande og stier.",
            tips = listOf(
                "Få råd om energirenovering og grøn varme.",
            ),
            links = listOf(
                Link("ProjectZero", "https://www.projectzero.dk", "Sønderborgs klimaindsats"),
                Link("Natur og friluftsliv", kommuneSearch("natur friluftsliv")),
            ),
            searchTerms = listOf("klima", "natur", "badevand"),
        ),
        Topic(
            id = "politik",
            title = "Politik & byråd",
            icon = Icons.Outlined.Gavel,
            intro = "Følg byrådets og udvalgenes møder. Dagsordener og referater er offentlige, og du kan deltage i høringer.",
            tips = listOf(
                "Byrådsmøder er offentlige – du kan møde op eller følge med online.",
            ),
            links = listOf(
                Link("Dagsordener og referater", DAGSORDENER),
                Link("Høringer", kommuneSearch("høringer")),
            ),
            searchTerms = listOf("byråd", "dagsorden", "høring"),
        ),
        Topic(
            id = "ny",
            title = "Ny i Sønderborg",
            icon = Icons.Outlined.Flight,
            intro = "Er du flyttet hertil – eventuelt fra udlandet? Få hjælp til CPR, skat, sundhed og at falde til.",
            tips = listOf(
                "Newcomer Service hjælper internationale tilflyttere.",
            ),
            links = listOf(
                Link("Newcomer Service Sønderborg", kommuneSearch("newcomer service")),
                Link("Life in Denmark", "https://lifeindenmark.borger.dk", "Guide for internationals"),
                Link("Skat", "https://www.skat.dk"),
            ),
            searchTerms = listOf("tilflytter", "newcomer"),
        ),
    )

    data class Phone(val name: String, val number: String, val note: String? = null)

    val importantNumbers = listOf(
        Phone("Alarm – livsfare", "112", "Politi, brand og ambulance"),
        Phone("Politiet", "114", "Når det ikke haster"),
        Phone("Lægevagten, Region Syddanmark", "70 11 07 07", "Når egen læge har lukket"),
        Phone("Borgerservice Sønderborg", BORGERSERVICE_PHONE, "Kommunens hovednummer"),
        Phone("Giftlinjen", "82 12 12 12"),
        Phone("Spærring af betalingskort", "44 89 29 29"),
        Phone("Børns Vilkår – BørneTelefonen", "116 111"),
        Phone("Livslinien", "70 201 201", "Hvis du har selvmordstanker"),
    )
}
