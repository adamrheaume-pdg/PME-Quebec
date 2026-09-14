package quebec.donnees.q360

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LiveDataState(
    val loading: Boolean = true,
    val temperature: String = "—",
    val feelsLike: String = "—",
    val humidity: String = "—",
    val wind: String = "—",
    val gusts: String = "—",
    val pressure: String = "—",
    val precipitation: String = "—",
    val rain: String = "—",
    val snowfall: String = "—",
    val cloudCover: String = "—",
    val aqi: String = "—",
    val pm25: String = "—",
    val pm10: String = "—",
    val co: String = "—",
    val no2: String = "—",
    val ozone: String = "—",
    val so2: String = "—",
    val uv: String = "—",
    val openDataCount: String = "—",
    val hydroUpdate: String = "—",
    val error: String? = null,
    val lastRefresh: String = "—"
)

data class Region(val name: String, val latitude: Double, val longitude: Double)

data class DataModule(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val description: String,
    val interpretation: String,
    val source: String,
    val url: String,
    val status: String = "SOURCE PUBLIQUE"
)

data class RealtimeRow(
    val label: String,
    val value: String,
    val unit: String,
    val info: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Quebec360App() }
    }
}

@Composable
fun Quebec360App() {
    val navy = Color(0xFF03102F)
    val navy2 = Color(0xFF061A4A)
    val electric = Color(0xFF1D8CFF)
    val cyan = Color(0xFF58DCFF)
    val text = Color(0xFFF4F8FF)

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = electric,
            secondary = cyan,
            background = navy,
            surface = navy2,
            onPrimary = Color.White,
            onBackground = text,
            onSurface = text
        )
    ) {
        val regions = remember {
            listOf(
                Region("Bas-Saint-Laurent", 48.4488, -68.5240),
                Region("Saguenay–Lac-Saint-Jean", 48.4281, -71.0685),
                Region("Capitale-Nationale", 46.8139, -71.2080),
                Region("Mauricie", 46.3430, -72.5430),
                Region("Estrie", 45.4042, -71.8929),
                Region("Montréal", 45.5019, -73.5674),
                Region("Outaouais", 45.4765, -75.7013),
                Region("Abitibi-Témiscamingue", 48.2366, -79.0231),
                Region("Côte-Nord", 49.2213, -68.1504),
                Region("Nord-du-Québec", 49.9168, -74.3659),
                Region("Gaspésie–Îles-de-la-Madeleine", 48.8316, -64.4810),
                Region("Chaudière-Appalaches", 46.8033, -71.1779),
                Region("Laval", 45.6066, -73.7124),
                Region("Lanaudière", 46.0230, -73.4390),
                Region("Laurentides", 45.7804, -74.0036),
                Region("Montérégie", 45.5312, -73.5181),
                Region("Centre-du-Québec", 45.8834, -72.4843)
            )
        }
        var region by remember { mutableStateOf(regions.first { it.name == "Capitale-Nationale" }) }
        var state by remember { mutableStateOf(LiveDataState()) }
        var selectedInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
        var expanded by remember { mutableStateOf(false) }
        var refreshKey by remember { mutableIntStateOf(0) }
        val uriHandler = LocalUriHandler.current

        LaunchedEffect(region, refreshKey) {
            state = state.copy(loading = true, error = null)
            state = fetchLiveData(region)
        }

        selectedInfo?.let { info ->
            AlertDialog(
                onDismissRequest = { selectedInfo = null },
                title = { Text(info.first, color = Color.White) },
                text = { Text(info.second, lineHeight = 20.sp, color = Color(0xFFDCE8FF)) },
                confirmButton = {
                    TextButton(onClick = { selectedInfo = null }) { Text("Compris") }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF072C79), navy, Color(0xFF020818))
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Header() }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xB30A1D4E), contentColor = Color.White),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = cyan)
                                Spacer(Modifier.width(8.dp))
                                Text("Région affichée", fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(Modifier.weight(1f))
                                Box {
                                    OutlinedButton(onClick = { expanded = true }) {
                                        Text(region.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        regions.forEach { item ->
                                            DropdownMenuItem(
                                                text = { Text(item.name) },
                                                onClick = {
                                                    region = item
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Données actualisées automatiquement. Appuie sur ⓘ pour comprendre chaque indicateur.",
                                color = Color(0xFFC8D7F2),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                item { SectionTitle("EN DIRECT", "Résumé rapide de la région sélectionnée") }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LiveMetricCard(
                            Icons.Default.Cloud, "Météo", state.temperature, "°C",
                            "Ressenti ${state.feelsLike} °C • Humidité ${state.humidity}% • Vent ${state.wind} km/h",
                            "Température actuelle, ressenti, humidité et vent. Le ressenti combine plusieurs effets atmosphériques et peut différer de la température mesurée.",
                            { selectedInfo = "Interpréter la météo" to it }, state.loading
                        )
                        LiveMetricCard(
                            Icons.Default.Air, "Qualité de l'air", state.aqi, "AQI",
                            "PM2,5 ${state.pm25} • PM10 ${state.pm10} µg/m³",
                            "L'AQI résume plusieurs polluants. Plus l'indice est bas, meilleure est la qualité de l'air. PM2,5 et PM10 désignent des particules fines.",
                            { selectedInfo = "Interpréter la qualité de l'air" to it }, state.loading
                        )
                        LiveMetricCard(
                            Icons.Default.Storage, "Données Québec", state.openDataCount, "jeux",
                            "Nombre de jeux repérés dans le catalogue public CKAN",
                            "Un jeu de données est un ensemble structuré publié par un organisme. Le nombre peut varier avec les ajouts et retraits du portail.",
                            { selectedInfo = "Catalogue Données Québec" to it }, state.loading
                        )
                        LiveMetricCard(
                            Icons.Default.Bolt, "Hydro-Québec – pannes", state.hydroUpdate, "mise à jour",
                            "Horodatage du flux public des interruptions",
                            "Cette heure correspond à la version du flux public d'Hydro-Québec. Elle ne représente pas le nombre de pannes dans la région.",
                            { selectedInfo = "Flux de pannes Hydro-Québec" to it }, state.loading
                        )
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dernière actualisation : ${state.lastRefresh}", fontSize = 12.sp, color = Color(0xFFB5C9EA))
                        Spacer(Modifier.weight(1f))
                        FilledTonalIconButton(onClick = { refreshKey++ }) {
                            Icon(Icons.Default.Refresh, "Actualiser", tint = cyan)
                        }
                    }
                }

                item { SectionTitle("TABLEAU TEMPS RÉEL", "Mesures disponibles et utiles dans un seul tableau") }

                item {
                    RealtimeTable(
                        rows = realtimeRows(state),
                        loading = state.loading,
                        onInfo = { row -> selectedInfo = row.label to row.info }
                    )
                }

                if (state.error != null) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0x553F5F8F), contentColor = Color.White)) {
                            Text(
                                "Certaines sources n'ont pas répondu : ${state.error}",
                                modifier = Modifier.padding(14.dp),
                                fontSize = 12.sp,
                                color = Color(0xFFD6E4FF)
                            )
                        }
                    }
                }

                item { SectionTitle("SOURCES UTILES DU QUÉBEC", "Accès direct aux données publiques et services de référence") }

                items(publicModules()) { module ->
                    ModuleCard(
                        module = module,
                        onInfo = { selectedInfo = module.title to (module.description + "\n\nComment l'interpréter :\n" + module.interpretation) },
                        onOpen = { uriHandler.openUri(module.url) }
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xAA061A4A), contentColor = Color.White)
                    ) {
                        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚜", fontSize = 34.sp, color = cyan)
                            Text("Québec 360 – Données", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text("Un tableau de bord pour comprendre les données utiles du Québec.", color = Color(0xFFC7D8F5), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF0D69ED)) {
            Text("⚜", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 32.sp, color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("QUÉBEC 360", fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = Color.White)
            Text("DONNÉES PUBLIQUES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF63D8FF), letterSpacing = 1.5.sp)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = Color.White)
        Text(subtitle, fontSize = 12.sp, color = Color(0xFFAFC5E8))
    }
}

@Composable
private fun LiveMetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    unit: String,
    secondary: String,
    info: String,
    onInfo: (String) -> Unit,
    loading: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xC6071B49), contentColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color(0x33268DFF)) {
                Icon(icon, null, modifier = Modifier.padding(12.dp).size(28.dp), tint = Color(0xFF63D8FF))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.width(6.dp))
                    InfoPill { onInfo(info) }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(if (loading) "…" else value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(Modifier.width(5.dp))
                    Text(unit, modifier = Modifier.padding(bottom = 4.dp), color = Color(0xFF9CC6FF), fontSize = 12.sp)
                }
                Text(secondary, color = Color(0xFFC9D8EE), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RealtimeTable(rows: List<RealtimeRow>, loading: Boolean, onInfo: (RealtimeRow) -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD0071B49), contentColor = Color.White)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF0C2A65)).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("INDICATEUR", Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF7DDBFF))
                Text("VALEUR", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF7DDBFF))
                Spacer(Modifier.width(34.dp))
            }
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = Color(0x332D6FC1))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(row.label, Modifier.weight(1.5f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                        Text(if (loading) "…" else row.value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                        if (row.unit.isNotBlank()) {
                            Spacer(Modifier.width(3.dp))
                            Text(row.unit, fontSize = 10.sp, color = Color(0xFFA9C5EA), modifier = Modifier.padding(bottom = 1.dp))
                        }
                    }
                    InfoPill { onInfo(row) }
                }
            }
        }
    }
}

@Composable
private fun InfoPill(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = Color(0xFF1760B7)) {
        Text("i", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Black, color = Color(0xFFA8EFFF))
    }
}

@Composable
private fun ModuleCard(module: DataModule, onInfo: () -> Unit, onOpen: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xB30A1D4E), contentColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(module.icon, null, tint = Color(0xFF63D8FF), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(module.title, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                    Text(module.subtitle, color = Color(0xFFBCD0EC), fontSize = 12.sp)
                }
                InfoPill(onInfo)
            }
            Spacer(Modifier.height(10.dp))
            Text(module.description, color = Color(0xFFE1EAF8), fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50), color = Color(0x3326D980)) {
                    Text(module.status, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = Color(0xFF72F0B4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpen) {
                    Text("Ouvrir la source")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun realtimeRows(s: LiveDataState): List<RealtimeRow> = listOf(
    RealtimeRow("Température", s.temperature, "°C", "Température de l'air mesurée près de la surface."),
    RealtimeRow("Température ressentie", s.feelsLike, "°C", "Estimation de la sensation thermique ressentie par une personne."),
    RealtimeRow("Humidité relative", s.humidity, "%", "Part de vapeur d'eau présente dans l'air par rapport au maximum possible à cette température."),
    RealtimeRow("Vent", s.wind, "km/h", "Vitesse moyenne du vent près de 10 mètres au-dessus du sol."),
    RealtimeRow("Rafales", s.gusts, "km/h", "Pointes de vent brèves, généralement plus fortes que la vitesse moyenne."),
    RealtimeRow("Pression atmosphérique", s.pressure, "hPa", "Pression de l'air à la surface. Une variation rapide peut accompagner un changement météo."),
    RealtimeRow("Précipitations", s.precipitation, "mm", "Quantité totale récente de précipitations, incluant pluie et neige équivalente selon la source."),
    RealtimeRow("Pluie", s.rain, "mm", "Quantité de pluie récente estimée pour le point sélectionné."),
    RealtimeRow("Neige", s.snowfall, "cm", "Quantité récente de neige estimée. Les accumulations au sol peuvent différer."),
    RealtimeRow("Couverture nuageuse", s.cloudCover, "%", "Pourcentage du ciel couvert par les nuages."),
    RealtimeRow("AQI", s.aqi, "indice", "Indice synthétique de qualité de l'air. Plus il est bas, meilleure est la qualité de l'air."),
    RealtimeRow("PM2,5", s.pm25, "µg/m³", "Particules fines de diamètre inférieur ou égal à 2,5 micromètres."),
    RealtimeRow("PM10", s.pm10, "µg/m³", "Particules de diamètre inférieur ou égal à 10 micromètres."),
    RealtimeRow("Monoxyde de carbone", s.co, "µg/m³", "Concentration estimée de CO dans l'air."),
    RealtimeRow("Dioxyde d'azote", s.no2, "µg/m³", "Concentration estimée de NO₂, souvent associée à la combustion et au trafic."),
    RealtimeRow("Ozone", s.ozone, "µg/m³", "Concentration d'ozone près du sol; elle varie notamment avec l'ensoleillement et les polluants précurseurs."),
    RealtimeRow("Dioxyde de soufre", s.so2, "µg/m³", "Concentration estimée de SO₂, un polluant principalement lié à certaines combustions et activités industrielles."),
    RealtimeRow("Indice UV", s.uv, "", "Mesure du rayonnement ultraviolet solaire. Plus l'indice est élevé, plus l'exposition est intense."),
    RealtimeRow("Catalogue Données Québec", s.openDataCount, "jeux", "Nombre de jeux de données répertoriés dans le catalogue CKAN public au moment de l'actualisation."),
    RealtimeRow("Flux Hydro-Québec", s.hydroUpdate, "", "Heure extraite de la version courante du flux public des interruptions d'Hydro-Québec.")
)

private fun publicModules(): List<DataModule> = listOf(
    DataModule("Transport et routes", "Travaux, circulation et état du réseau", Icons.Default.DirectionsCar,
        "Québec 511 fournit de l'information sur l'état du réseau routier, les entraves et les travaux.",
        "Une entrave ne signifie pas toujours une fermeture complète. Vérifie le type, le sens et la période.",
        "Québec 511", "https://www.quebec511.info/"),
    DataModule("Énergie", "Pannes et interruptions", Icons.Default.Bolt,
        "Hydro-Québec publie des informations et des données ouvertes sur les interruptions de service.",
        "Le nombre de clients touchés peut évoluer rapidement pendant une panne.",
        "Hydro-Québec", "https://pannes.hydroquebec.com/"),
    DataModule("Météo et alertes", "Conditions et avertissements", Icons.Default.Thunderstorm,
        "Les données météorologiques et les alertes permettent de suivre les conditions qui affectent les déplacements et les activités extérieures.",
        "Une prévision est une estimation. Pour une décision importante, vérifie aussi les alertes officielles locales.",
        "Environnement Canada", "https://meteo.gc.ca/"),
    DataModule("Feux de forêt", "Incendies, danger et restrictions", Icons.Default.LocalFireDepartment,
        "La SOPFEU publie l'information sur les incendies de forêt, le danger et certaines restrictions.",
        "Le niveau de danger varie selon la météo, l'humidité et la région. Il peut changer rapidement.",
        "SOPFEU", "https://sopfeu.qc.ca/"),
    DataModule("Inondations et sécurité civile", "Crues, avis et événements", Icons.Default.Water,
        "Le gouvernement publie des informations liées aux inondations, à la sécurité civile et aux événements majeurs.",
        "Un niveau de rivière ou un avis doit être interprété selon le lieu précis et l'évolution prévue.",
        "Gouvernement du Québec", "https://www.quebec.ca/securite-situations-urgence"),
    DataModule("Santé", "Établissements et services", Icons.Default.LocalHospital,
        "Les services publics permettent de repérer des établissements, des urgences et certains indicateurs de santé.",
        "Un indicateur d'attente est une photographie à un moment donné et ne garantit pas un temps réel individuel.",
        "Gouvernement du Québec", "https://www.quebec.ca/sante"),
    DataModule("Économie", "Emploi, prix, PIB et revenus", Icons.Default.TrendingUp,
        "L'Institut de la statistique du Québec publie de nombreuses séries économiques et régionales.",
        "Compare des périodes identiques et distingue les valeurs nominales des valeurs réelles corrigées de l'inflation.",
        "Institut de la statistique du Québec", "https://statistique.quebec.ca/"),
    DataModule("Démographie", "Population, âge et projections", Icons.Default.Groups,
        "Les données démographiques servent à comprendre la taille, la composition et l'évolution de la population.",
        "Une projection dépend d'hypothèses sur les naissances, les décès et les migrations.",
        "Institut de la statistique du Québec", "https://statistique.quebec.ca/"),
    DataModule("Immobilier et logement", "Construction et habitation", Icons.Default.Home,
        "Des organismes publics publient des statistiques sur les mises en chantier, le logement et l'habitation.",
        "Une moyenne provinciale peut masquer de gros écarts entre municipalités et quartiers.",
        "SCHL", "https://www.cmhc-schl.gc.ca/"),
    DataModule("Politique et élections", "Résultats, financement et circonscriptions", Icons.Default.HowToVote,
        "Élections Québec publie les résultats officiels, les circonscriptions et le financement politique.",
        "Un résultat électoral se lit avec le nombre de votes, la participation et la circonscription concernée.",
        "Élections Québec", "https://www.electionsquebec.qc.ca/"),
    DataModule("Environnement", "Air, eau, climat et territoire", Icons.Default.Forest,
        "Le Québec publie de nombreux jeux de données environnementales et géospatiales.",
        "Les mesures varient selon l'heure, le lieu, l'unité et la station de mesure.",
        "Données Québec", "https://www.donneesquebec.ca/"),
    DataModule("Transport collectif", "Horaires et données GTFS", Icons.Default.DirectionsBus,
        "Plusieurs sociétés de transport rendent disponibles leurs horaires et données GTFS.",
        "Les horaires planifiés ne représentent pas toujours la position réelle d'un véhicule.",
        "Données Québec / organismes de transport", "https://www.donneesquebec.ca/"),
    DataModule("Municipal", "Services, permis et infrastructures", Icons.Default.LocationCity,
        "Plusieurs municipalités publient des données ouvertes sur la voirie, les collectes, les parcs, le stationnement et les infrastructures.",
        "La disponibilité varie d'une municipalité à l'autre.",
        "Portails municipaux et Données Québec", "https://www.donneesquebec.ca/"),
    DataModule("Agriculture", "Cultures, territoires et statistiques", Icons.Default.Agriculture,
        "Des données publiques couvrent l'agriculture, les terres, les productions et certains indicateurs régionaux.",
        "Les données peuvent être saisonnières et publiées à des fréquences différentes selon les programmes.",
        "Gouvernement du Québec", "https://www.quebec.ca/agriculture-environnement-et-ressources-naturelles/agriculture"),
    DataModule("Culture et patrimoine", "Lieux, événements et patrimoine", Icons.Default.Museum,
        "Des données publiques permettent de repérer des lieux culturels, patrimoniaux et touristiques.",
        "Les horaires et événements peuvent changer; la source officielle du lieu demeure la référence.",
        "Gouvernement du Québec", "https://www.quebec.ca/culture")
)

private suspend fun fetchLiveData(region: Region): LiveDataState = withContext(Dispatchers.IO) {
    val errors = mutableListOf<String>()
    var temp = "—"; var feels = "—"; var humidity = "—"; var wind = "—"; var gusts = "—"
    var pressure = "—"; var precip = "—"; var rain = "—"; var snow = "—"; var clouds = "—"
    var aqi = "—"; var pm25 = "—"; var pm10 = "—"; var co = "—"; var no2 = "—"; var ozone = "—"; var so2 = "—"; var uv = "—"
    var count = "—"; var hydro = "—"

    try {
        val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=${region.latitude}&longitude=${region.longitude}&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,wind_gusts_10m,surface_pressure,precipitation,rain,snowfall,cloud_cover&timezone=America%2FToronto"
        val current = JSONObject(httpGet(weatherUrl)).getJSONObject("current")
        temp = oneDecimal(current.optDouble("temperature_2m"))
        feels = oneDecimal(current.optDouble("apparent_temperature"))
        humidity = current.optInt("relative_humidity_2m").toString()
        wind = oneDecimal(current.optDouble("wind_speed_10m"))
        gusts = oneDecimal(current.optDouble("wind_gusts_10m"))
        pressure = oneDecimal(current.optDouble("surface_pressure"))
        precip = oneDecimal(current.optDouble("precipitation"))
        rain = oneDecimal(current.optDouble("rain"))
        snow = oneDecimal(current.optDouble("snowfall"))
        clouds = current.optInt("cloud_cover").toString()
    } catch (e: Exception) { errors.add("météo") }

    try {
        val airUrl = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${region.latitude}&longitude=${region.longitude}&current=us_aqi,pm2_5,pm10,carbon_monoxide,nitrogen_dioxide,ozone,sulphur_dioxide,uv_index&timezone=America%2FToronto"
        val current = JSONObject(httpGet(airUrl)).getJSONObject("current")
        aqi = current.optInt("us_aqi").toString()
        pm25 = oneDecimal(current.optDouble("pm2_5"))
        pm10 = oneDecimal(current.optDouble("pm10"))
        co = oneDecimal(current.optDouble("carbon_monoxide"))
        no2 = oneDecimal(current.optDouble("nitrogen_dioxide"))
        ozone = oneDecimal(current.optDouble("ozone"))
        so2 = oneDecimal(current.optDouble("sulphur_dioxide"))
        uv = oneDecimal(current.optDouble("uv_index"))
    } catch (e: Exception) { errors.add("qualité de l'air") }

    try {
        val json = JSONObject(httpGet("https://www.donneesquebec.ca/recherche/api/3/action/package_search?rows=0"))
        count = json.getJSONObject("result").optInt("count").toString()
    } catch (e: Exception) { errors.add("Données Québec") }

    try {
        val raw = httpGet("https://pannes.hydroquebec.com/pannes/donnees/v3_0/bisversion.json")
        val digits = Regex("\\d{14}").find(raw)?.value
        hydro = if (digits != null) "${digits.substring(8, 10)}:${digits.substring(10, 12)}" else "connecté"
    } catch (e: Exception) { errors.add("Hydro-Québec") }

    LiveDataState(
        loading = false,
        temperature = temp,
        feelsLike = feels,
        humidity = humidity,
        wind = wind,
        gusts = gusts,
        pressure = pressure,
        precipitation = precip,
        rain = rain,
        snowfall = snow,
        cloudCover = clouds,
        aqi = aqi,
        pm25 = pm25,
        pm10 = pm10,
        co = co,
        no2 = no2,
        ozone = ozone,
        so2 = so2,
        uv = uv,
        openDataCount = count,
        hydroUpdate = hydro,
        error = if (errors.isEmpty()) null else errors.joinToString(", "),
        lastRefresh = SimpleDateFormat("HH:mm", Locale.CANADA_FRENCH).format(Date())
    )
}

private fun oneDecimal(value: Double): String = if (value.isNaN()) "—" else String.format(Locale.CANADA_FRENCH, "%.1f", value)

private fun httpGet(address: String): String {
    val connection = URL(address).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 9000
    connection.readTimeout = 9000
    connection.setRequestProperty("User-Agent", "Quebec360Data/1.2")
    connection.setRequestProperty("Accept", "application/json,text/plain,*/*")
    return try {
        if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
