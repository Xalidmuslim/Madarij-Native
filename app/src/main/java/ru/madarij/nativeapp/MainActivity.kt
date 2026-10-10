package ru.madarij.nativeapp

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import ru.madarij.nativeapp.data.ReadingSettings
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BookApp() }
    }
}

internal fun readerRoute(id:String,paragraph:String?=null,query:String="",top:Boolean=false) =
    "read/${Uri.encode(id)}?paragraph=${Uri.encode(paragraph.orEmpty())}&query=${Uri.encode(query)}&top=$top"

@Composable
private fun BookRouteSurface(
    texture: Int,
    dark: Boolean,
    content: @Composable () -> Unit
) {
    // The opaque paper is composed first. This prevents the previous screen,
    // especially the library hero image, from showing through during navigation.
    Box(
        Modifier.fillMaxSize().background(
            if (dark) BookColors.nightBackground else BookColors.parchment
        )
    ) {
        if (!dark) {
            Image(
                painter = painterResource(texture),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )
        }
        content()
    }
}

@Composable
fun BookApp(vm:BookViewModel=viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val chapters by vm.chapters.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val isReader = route.startsWith("read/")
    // Reading background is independent of the visual theme used for menu screens.
    val appearance = if (isReader) settings else settings.copy(
        theme = if (settings.theme == "dark") "dark" else "sepia", paperTone = "warm"
    )
    val colors = bookReaderColors(appearance, false)
    val readerPaperTexture = when (settings.paperTone) {
        "sage" -> R.drawable.paper_sage
        "light" -> R.drawable.paper_light
        else -> R.drawable.reference_paper
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MadarijTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(AppRadius.small),
            medium = RoundedCornerShape(AppRadius.medium),
            large = RoundedCornerShape(AppRadius.large),
            extraLarge = RoundedCornerShape(AppRadius.hero)
        )
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onBackground) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            // Fresh installation defaults only; existing preferences are retained.
            if (!java.io.File(context.filesDir, "datastore/reading_settings.preferences_pb").exists())
                vm.settings(settings.copy(theme = "sepia", paperTone = "warm", russianFont = "book", russianSize = 18f, lineHeight = 1.5f))
        }

        // The window background is stable across navigation. Replacing its drawable
        // while opening or closing pages can cause a device-dependent flash.
        LaunchedEffect(settings.theme) {
            (context as? Activity)?.window?.let { window ->
                window.setBackgroundDrawable(ColorDrawable(
                    (if (settings.theme == "dark") BookColors.nightBackground else BookColors.parchment).toArgb()
                ))
                @Suppress("DEPRECATION")
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
            }
        }
        LaunchedEffect(isReader, route == "settings", settings.brightness) {
            (context as? Activity)?.window?.let { window ->
                val attr = window.attributes
                val wanted = if (isReader || route == "settings") settings.brightness else -1f
                if (attr.screenBrightness != wanted) {
                    attr.screenBrightness = wanted
                    window.attributes = attr
                }
            }
        }
        LaunchedEffect(settings.theme, route == "home") {
            (context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = settings.theme != "dark" && route != "home"
                    isAppearanceLightNavigationBars = settings.theme != "dark"
                }
            }
        }

        val title = when {
            route == "contents" -> "Оглавление"
            route == "search" -> "Поиск"
            route == "bookmarks" -> "Закладки"
            route == "notes" -> "Мои заметки"
            route == "settings" -> "Настройки"
            route == "about" -> "О книге"
            route == "progress" -> "Прогресс"
            route == "glossary" -> "Словарь"
            route == "more" -> "Ещё"
            route == "backup" -> "Резервная копия"
            route.startsWith("study") -> "Изучение"
            isReader -> "Степени идущих"
            else -> "Степени идущих"
        }
        val subtitle = when {
            route == "home" -> null
            route == "contents" -> "Главы, темы и место чтения"
            route == "search" -> "По всему первому тому"
            route == "bookmarks" -> "Сохранённые места"
            route == "notes" -> "Личные записи"
            route == "settings" -> "Текст, тема и чтение"
            route == "progress" -> "Чтение и усвоение"
            route == "glossary" -> "Термины и контекст"
            route.startsWith("study") -> "Проверки, задания и повторение"
            isReader -> "Том 1"
            else -> null
        }
        val tabs = listOf("home" to "Главная", "contents" to "Оглавление", "bookmarks" to "Закладки", "search" to "Поиск", "more" to "Ещё")
        val selectedTab = when { route == "bookmarks" -> "bookmarks"; route == "search" -> "search"; else -> selectedMainTab(route).let { if (it == "study") "more" else it } }
        val primaryBack = primaryBackAction(route)

        BackHandler(enabled = primaryBack != PrimaryBackAction.POP) {
            if (primaryBack == PrimaryBackAction.HOME && !nav.popBackStack("home", false)) {
                nav.navigate("home") { launchSingleTop = true }
            }
        }

        val tabRoot = route in setOf("home","contents","bookmarks","notes","progress","glossary","settings","about","backup","more","search") || route.startsWith("study")
        val openReader:(String,String?,String)->Unit = { chapter,p,q -> nav.navigate(readerRoute(chapter,p,q)) }
        val openFromContents:(String,String?)->Unit = { chapter,paragraph ->
            if (paragraph == null) {
                nav.navigate(readerRoute(chapter, top = true))
            } else scope.launch {
                vm.repository.dao.paragraph(paragraph)?.let { nav.navigate(readerRoute(it.chapterId,it.id)) }
            }
        }
        val openSource:(String)->Unit = { id -> scope.launch {
            vm.repository.dao.paragraph(id)?.let { nav.navigate(readerRoute(it.chapterId,it.id)) }
        }}
        val navigate:(String)->Unit = { nav.navigate(it) { launchSingleTop = true } }

        Box(Modifier.fillMaxSize().background(colors.background)) {
            // One stationary book-paper layer behind the status bar and floating navigation.
            // Every destination draws its own OPAQUE page, never previous-route pixels.
            if (settings.theme != "dark") {
                Image(painterResource(R.drawable.reference_paper), null,
                    Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
            }
            Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (!isReader && route != "home") MadarijTopBar(
                    title = title,
                    subtitle = subtitle,
                    canBack = !tabRoot,
                    onBack = { if(!nav.popBackStack()) nav.navigate("home") },
                    showSearch = !isReader && route !in setOf("search", "settings"),
                    onSearch = { navigate("search") },
                    showTextSettings = !isReader && route != "settings" && route != "search",
                    onTextSettings = { navigate("settings") },
                    onBookmarks = { navigate("bookmarks") }
                )
            },
            bottomBar = {}
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Transparent)
                    .clipToBounds()
            ) {
            NavHost(
                navController = nav,
                startDestination = "home",
                modifier = Modifier.fillMaxSize(), // Full-height content sits BEHIND the floating navigation.
                // Keep one stable parchment background throughout navigation; no translated old screen.
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                composable("home") { HomeScreen(vm,{id,p -> openReader(id,p,"")},navigate) }
                composable("contents") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark") { ContentsScreen(vm,openFromContents) { chapter -> openReader(chapter,null,"") } } }
                composable("search") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark") { SearchScreen(vm,openReader) } }
                composable("bookmarks") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark") { BookmarksScreen(vm) {id,p -> openReader(id,p,"")} } }
                composable("notes") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark") { NotesScreen(vm) {id,p -> openReader(id,p,"")} } }
                composable("progress") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark") { ProgressScreen(vm,{id,p -> openReader(id,p,"")},navigate) } }
                composable("more") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark") { MoreScreen(navigate) } }
                composable("settings") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark") { SettingsPanel(settings,vm::settings) } }
                composable("about") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark") { AboutScreen() } }
                composable("backup") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark") { BackupScreen(vm) } }
                composable("glossary") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark") { GlossaryScreen(openSource) } }
                composable(
                    "study?chapter={chapter}&review={review}",
                    arguments=listOf(
                        navArgument("chapter") { type=NavType.StringType; defaultValue="" },
                        navArgument("review") { type=NavType.BoolType; defaultValue=false }
                    )
                ) { e ->
                    BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark") { StudyScreen(
                            vm, openSource,
                            initialChapterId=e.arguments?.getString("chapter")?.takeIf {it.isNotEmpty()},
                            initialTab=if(e.arguments?.getBoolean("review")==true) 3 else if(!e.arguments?.getString("chapter").isNullOrEmpty()) 1 else 0
                        ) }
                }
                composable(
                    "read/{id}?paragraph={paragraph}&query={query}&top={top}",
                    arguments=listOf(
                        navArgument("paragraph") {type=NavType.StringType;defaultValue=""},
                        navArgument("query") {type=NavType.StringType;defaultValue=""},
                        navArgument("top") {type=NavType.BoolType;defaultValue=false}
                    )
                ) { e ->
                    val chapter=chapters.find {it.id==e.arguments?.getString("id")}
                    if(chapter==null) InfoCard("Подготовка книги","Раздел загружается…")
                    else BookRouteSurface(readerPaperTexture, settings.theme == "dark") { Reader(
                        vm, chapter, settings,
                        e.arguments?.getString("paragraph")?.takeIf {it.isNotEmpty()},
                        e.arguments?.getString("query").orEmpty(),
                        forceTop=e.arguments?.getBoolean("top") == true,
                        onStudy={navigate("study?chapter=${Uri.encode(it)}")},
                        onOpenParagraph=openSource,
                        onBack={ nav.popBackStack() },
                        navigate={ id ->
                            nav.navigate(readerRoute(id, top = true)) { popUpTo(e.destination.id) { inclusive=true } }
                        }
                    ) }
                }
            }
            Box(
                Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 15.dp).padding(bottom = 12.dp)
            ) {
MadarijBottomBar(tabs, selectedTab) { destination ->
    when {
        destination == "home" -> nav.navigate("home") {
            popUpTo(nav.graph.startDestinationId) { inclusive=false; saveState=false }
            launchSingleTop=true; restoreState=false
        }
        destination == "contents" && shouldOpenContentsRoot(route) -> {
            if(!nav.popBackStack("contents",false)) nav.navigate("contents") {
                popUpTo(nav.graph.startDestinationId) { inclusive=false; saveState=false }
                launchSingleTop=true; restoreState=false
            }
        }
        destination == "contents" -> Unit
        else -> nav.navigate(destination) {
            popUpTo(nav.graph.startDestinationId) { saveState=true }
            launchSingleTop=true; restoreState=true
        }
    }
}

            }
            }
        }
        }
    }
    }
}
