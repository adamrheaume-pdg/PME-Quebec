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
    val aqi: String = "—",
    val pm25: String = "—",
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
                Region("Québec", 46.8139, -71.2080),
                Region("Montréal", 45.5019, -73.5674),
                Region("Trois-Rivières", 46.3430, -72.5430),
                Region("Sherbrooke", 45.4042, -71.8929),
                Region("Saguenay", 48.4281, -71.0685),
                Region("Gatineau", 45.4765, -75.7013)
            )
        }
        var region by remember { mutableStateOf(regions.first()) }
        var state by remember { mutableStateOf(LiveDataState()) }
        var selectedInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
        var expanded by remember { mutableStateOf(false) }
        val uriHandler = LocalUriHandler.current

        LaunchedEffect(region) {
            state = state.copy(loading = true, error = null)
            state = fetchLiveData(region)
        }

        if (selectedInfo != null) {
            AlertDialog(
                onDismissRequest = { selectedInfo = null },
                title = { Text(selectedInfo!!.first) },
                text = { Text(selectedInfo!!.second, lineHeight = 20.sp) },
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Header()
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xB30A1D4E)),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = cyan)
                                Spacer(Modifier.width(8.dp))
                                Text("Région affichée", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Box {
                                    OutlinedButton(onClick = { expanded = true }) {
                                        Text(region.name)
                                        Spacer(Modifier.width(6.dp))
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
                                "Les cartes ci-dessous expliquent ce que les chiffres veulent dire. Appuie sur la pastille ⓘ lorsqu'un indicateur est technique.",
                                color = Color(0xFFC8D7F2),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                item {
                    SectionTitle("EN DIRECT", "Données publiques et ouvertes actualisées")
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LiveMetricCard(
                            icon = Icons.Default.Cloud,
                            title = "Météo",
                            value = state.temperature,
                            unit = "°C",
                            secondary = "Ressenti ${state.feelsLike} °C • Humidité ${state.humidity}% • Vent ${state.wind} km/h",
                            info = "La température ressentie tient compte notamment du vent et de l'humidité. L'humidité relative indique la proportion de vapeur d'eau présente dans l'air par rapport au maximum possible à cette température.",
                            onInfo = { selectedInfo = "Interpréter la météo" to it },
                            loading = state.loading
                        )
                        LiveMetricCard(
                            icon = Icons.Default.Air,
                            title = "Qualité de l'air",
                            value = state.aqi,
                            unit = "AQI",
                            secondary = "PM2,5 : ${state.pm25} µg/m³",
                            info = "L'AQI est un indice synthétique de qualité de l'air : plus il est bas, meilleure est la qualité de l'air. Les PM2,5 sont de très fines particules mesurant 2,5 micromètres ou moins.",
                            onInfo = { selectedInfo = "Interpréter l'AQI et les PM2,5" to it },
                            loading = state.loading
                        )
                        LiveMetricCard(
                            icon = Icons.Default.Storage,
                            title = "Données Québec",
                            value = state.openDataCount,
                            unit = "jeux",
                            secondary = "Catalogue CKAN public du Québec",
                            info = "Un jeu de données est un ensemble structuré publié par un organisme. Le portail Données Québec permet aux applications de chercher et lire de nombreux jeux de données par API.",
                            onInfo = { selectedInfo = "Qu'est-ce qu'un jeu de données?" to it },
                            loading = state.loading
                        )
                        LiveMetricCard(
                            icon = Icons.Default.Bolt,
                            title = "Hydro-Québec – pannes",
                            value = state.hydroUpdate,
                            unit = "mise à jour",
                            secondary = "Version du flux public des interruptions",
                            info = "Hydro-Québec publie un flux de données ouvertes sur les pannes en cours et les interruptions planifiées. La valeur affichée correspond à l'horodatage du flux actuellement publié.",
                            onInfo = { selectedInfo = "Flux de pannes Hydro-Québec" to it },
                            loading = state.loading
                        )
                    }
                }

                if (state.error != null) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0x553F5F8F))) {
                            Text(
                                "Certaines données en direct n'ont pas répondu. Les autres sections restent utilisables. ${state.error}",
                                modifier = Modifier.padding(14.dp),
                                fontSize = 12.sp,
                                color = Color(0xFFD6E4FF)
                            )
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dernière actualisation : ${state.lastRefresh}", fontSize = 12.sp, color = Color(0xFF9DB6DE))
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            state = state.copy(loading = true)
                        }) { Icon(Icons.Default.Refresh, "Actualiser", tint = cyan) }
                    }
                }

                item {
                    SectionTitle("EXPLORER LE QUÉBEC", "Sources gouvernementales et données utiles")
                }

                items(publicModules()) { module ->
                    ModuleCard(
                        module = module,
                        onInfo = {
                            selectedInfo = module.title to (module.description + "\n\nComment l'interpréter :\n" + module.interpretation)
                        },
                        onOpen = { uriHandler.openUri(module.url) }
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xAA061A4A))
                    ) {
                        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚜", fontSize = 34.sp, color = cyan)
                            Text("Québec 360 – Données", fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Une seule application pour comprendre les données publiques du Québec.",
                                color = Color(0xFFC7D8F5),
                                fontSize = 13.sp
                            )
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
            Text("⚜", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 32.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("QUÉBEC 360", fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text("DONNÉES PUBLIQUES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF63D8FF), letterSpacing = 1.5.sp)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text(subtitle, fontSize = 12.sp, color = Color(0xFF9DB6DE))
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
        colors = CardDefaults.cardColors(containerColor = Color(0xC6071B49)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color(0x33268DFF)) {
                Icon(icon, null, modifier = Modifier.padding(12.dp).size(28.dp), tint = Color(0xFF63D8FF))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    InfoPill { onInfo(info) }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(if (loading) "…" else value, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(5.dp))
                    Text(unit, modifier = Modifier.padding(bottom = 4.dp), color = Color(0xFF8EB8F4), fontSize = 12.sp)
                }
                Text(secondary, color = Color(0xFFB9CAE8), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun InfoPill(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color(0xFF154C95)
    ) {
        Text("i", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Black, color = Color(0xFF8FE8FF))
    }
}

@Composable
private fun ModuleCard(module: DataModule, onInfo: () -> Unit, onOpen: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xB30A1D4E)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(module.icon, null, tint = Color(0xFF63D8FF), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(module.title, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(module.subtitle, color = Color(0xFFAEC4E8), fontSize = 12.sp)
                }
                InfoPill(onInfo)
            }
            Spacer(Modifier.height(10.dp))
            Text(module.description, color = Color(0xFFD6E3F7), fontSize = 13.sp, lineHeight = 18.sp)
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

private fun publicModules(): List<DataModule> = listOf(
    DataModule(
        "Transport et routes",
        "Travaux, circulation et réseau routier",
        Icons.Default.DirectionsCar,
        "Québec 511 publie de l'information sur l'état du réseau routier, les entraves et les travaux.",
        "Une entrave ne signifie pas nécessairement une fermeture complète. Vérifie le type d'entrave, le sens, la période et les conditions avant d'en tirer une conclusion.",
        "Québec 511",
        "https://www.quebec511.info/"
    ),
    DataModule(
        "Énergie",
        "Pannes et interruptions planifiées",
        Icons.Default.Bolt,
        "Hydro-Québec met à disposition des données ouvertes sur les interruptions de service.",
        "Le nombre de clients touchés est une estimation opérationnelle qui peut évoluer rapidement pendant une panne.",
        "Hydro-Québec – Données ouvertes",
        "https://donnees.hydroquebec.com/"
    ),
    DataModule(
        "Santé",
        "Établissements et indicateurs publics",
        Icons.Default.LocalHospital,
        "Des données publiques permettent de trouver des établissements, services et certains indicateurs du réseau de la santé.",
        "Un indicateur de capacité ou d'attente est une photographie à un moment donné : il ne garantit pas ton temps d'attente réel.",
        "Gouvernement du Québec",
        "https://www.quebec.ca/sante"
    ),
    DataModule(
        "Économie",
        "Emploi, prix, PIB et revenus",
        Icons.Default.TrendingUp,
        "Statistique Canada et l'Institut de la statistique du Québec publient des séries économiques détaillées.",
        "Compare toujours des périodes identiques et distingue les valeurs nominales des valeurs corrigées de l'inflation.",
        "Institut de la statistique du Québec",
        "https://statistique.quebec.ca/"
    ),
    DataModule(
        "Démographie",
        "Population, âge, régions et projections",
        Icons.Default.Groups,
        "Les données démographiques servent à comprendre la taille, la composition et l'évolution de la population.",
        "Une projection n'est pas une prédiction certaine : elle dépend d'hypothèses sur les naissances, les décès et les migrations.",
        "Institut de la statistique du Québec",
        "https://statistique.quebec.ca/"
    ),
    DataModule(
        "Immobilier et logement",
        "Construction, logement et marché",
        Icons.Default.Home,
        "Des organismes publics publient des statistiques sur les mises en chantier, les logements et l'habitation.",
        "Une moyenne provinciale peut masquer de gros écarts entre municipalités. Interprète toujours les chiffres avec la région et la période.",
        "SCHL et données publiques",
        "https://www.cmhc-schl.gc.ca/"
    ),
    DataModule(
        "Politique et élections",
        "Résultats, financement et circonscriptions",
        Icons.Default.HowToVote,
        "Élections Québec publie des résultats officiels, des renseignements sur les circonscriptions et le financement politique.",
        "Un résultat électoral doit être lu avec le nombre de votes valides, le taux de participation et la circonscription concernée.",
        "Élections Québec",
        "https://www.electionsquebec.qc.ca/"
    ),
    DataModule(
        "Environnement",
        "Air, eau, climat et territoire",
        Icons.Default.Forest,
        "Le Québec et le Canada publient de nombreux jeux de données environnementales et géospatiales.",
        "Les mesures environnementales peuvent varier fortement selon l'heure, le lieu et la méthode de mesure. Vérifie toujours l'unité et la station source.",
        "Données Québec",
        "https://www.donneesquebec.ca/"
    ),
    DataModule(
        "Municipal",
        "Services, permis, collectes et infrastructures",
        Icons.Default.LocationCity,
        "Plusieurs municipalités publient leurs propres données ouvertes : collecte, voirie, stationnement, parcs et infrastructures.",
        "La disponibilité varie d'une municipalité à l'autre. Une donnée absente du portail ne veut pas dire que le service n'existe pas.",
        "Portails municipaux et Données Québec",
        "https://www.donneesquebec.ca/"
    )
)

private suspend fun fetchLiveData(region: Region): LiveDataState = withContext(Dispatchers.IO) {
    var error: String? = null
    var temp = "—"
    var feels = "—"
    var humidity = "—"
    var wind = "—"
    var aqi = "—"
    var pm25 = "—"
    var count = "—"
    var hydro = "—"

    try {
        val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=${region.latitude}&longitude=${region.longitude}&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m&timezone=America%2FToronto"
        val current = JSONObject(httpGet(weatherUrl)).getJSONObject("current")
        temp = oneDecimal(current.optDouble("temperature_2m"))
        feels = oneDecimal(current.optDouble("apparent_temperature"))
        humidity = current.optInt("relative_humidity_2m").toString()
        wind = oneDecimal(current.optDouble("wind_speed_10m"))
    } catch (e: Exception) {
        error = "Météo indisponible."
    }

    try {
        val airUrl = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${region.latitude}&longitude=${region.longitude}&current=us_aqi,pm2_5&timezone=America%2FToronto"
        val current = JSONObject(httpGet(airUrl)).getJSONObject("current")
        aqi = current.optInt("us_aqi").toString()
        pm25 = oneDecimal(current.optDouble("pm2_5"))
    } catch (e: Exception) {
        error = listOfNotNull(error, "Qualité de l'air indisponible.").joinToString(" ")
    }

    try {
        val json = JSONObject(httpGet("https://www.donneesquebec.ca/recherche/api/3/action/package_search?rows=0"))
        count = json.getJSONObject("result").optInt("count").toString()
    } catch (e: Exception) {
        error = listOfNotNull(error, "Données Québec indisponible.").joinToString(" ")
    }

    try {
        val raw = httpGet("https://pannes.hydroquebec.com/pannes/donnees/v3_0/bisversion.json")
        val digits = Regex("\\d{14}").find(raw)?.value
        hydro = if (digits != null) {
            "${digits.substring(8, 10)}:${digits.substring(10, 12)}"
        } else "connecté"
    } catch (e: Exception) {
        error = listOfNotNull(error, "Flux Hydro-Québec indisponible.").joinToString(" ")
    }

    LiveDataState(
        loading = false,
        temperature = temp,
        feelsLike = feels,
        humidity = humidity,
        wind = wind,
        aqi = aqi,
        pm25 = pm25,
        openDataCount = count,
        hydroUpdate = hydro,
        error = error,
        lastRefresh = SimpleDateFormat("HH:mm", Locale.CANADA_FRENCH).format(Date())
    )
}

private fun oneDecimal(value: Double): String = if (value.isNaN()) "—" else String.format(Locale.CANADA_FRENCH, "%.1f", value)

private fun httpGet(address: String): String {
    val connection = URL(address).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 9000
    connection.readTimeout = 9000
    connection.setRequestProperty("User-Agent", "Quebec360Data/1.0")
    connection.setRequestProperty("Accept", "application/json")
    return try {
        if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
