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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
fun BookApp(vm:BookViewModel=viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val chapters by vm.chapters.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val isReader = route.startsWith("read/")
    val colors = bookReaderColors(settings, false)

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
        var readerForward by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            // Fresh installation defaults only; existing preferences are retained.
            if (!java.io.File(context.filesDir, "datastore/reading_settings.preferences_pb").exists())
                vm.settings(settings.copy(theme = "sepia", russianFont = "book", russianSize = 18f, lineHeight = 1.5f))
        }

        SideEffect {
            (context as? Activity)?.window?.let { window ->
                val attr = window.attributes
                attr.screenBrightness = if (isReader || route == "settings") settings.brightness else -1f
                window.attributes = attr
                // Keep the native window behind Compose the same colour as the active theme.
                // During reader-to-reader slide transitions this prevents a black rectangle/frame
                // from showing through the moving composables on some Android devices.
                window.setBackgroundDrawable(ColorDrawable((if (route == "home") BookColors.leather else colors.background).toArgb()))
                @Suppress("DEPRECATION")
                window.navigationBarColor = BookColors.leather.toArgb()
                if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = settings.theme != "dark" && route != "home"
                    isAppearanceLightNavigationBars = false
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
        val reduceMotion = settings.reducedMotion || !android.animation.ValueAnimator.areAnimatorsEnabled()

        Box(Modifier.fillMaxSize().background(colors.background)) {
            // One continuous paper image behind all non-home pages and behind the status bar.
            // Never reuse the home illustration as a navigation backdrop.
            if (route != "home" && settings.theme != "dark") {
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
            bottomBar = {
                Column(Modifier.background(BookColors.leather).navigationBarsPadding()) {
                    MaterialTheme(colorScheme = colors) {
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
                modifier = Modifier.fillMaxSize().background(Color.Transparent),
                // Keep one stable parchment background throughout navigation; no translated old screen.
                enterTransition = { if (reduceMotion) EnterTransition.None else fadeIn(tween(135)) },
                // Do not keep the previous screen's pixels behind the next one.
                exitTransition = { if (reduceMotion) ExitTransition.None else fadeOut(tween(1)) },
                popEnterTransition = { if (reduceMotion) EnterTransition.None else fadeIn(tween(135)) },
                popExitTransition = { if (reduceMotion) ExitTransition.None else fadeOut(tween(1)) }
            ) {
                composable("home") { HomeScreen(vm,{id,p -> openReader(id,p,"")},navigate) }
                composable("contents") { ContentsScreen(vm,openFromContents) { chapter -> openReader(chapter,null,"") } }
                composable("search") { SearchScreen(vm,openReader) }
                composable("bookmarks") { BookmarksScreen(vm) {id,p -> openReader(id,p,"")} }
                composable("notes") { NotesScreen(vm) {id,p -> openReader(id,p,"")} }
                composable("progress") { ProgressScreen(vm,{id,p -> openReader(id,p,"")},navigate) }
                composable("more") { MoreScreen(navigate) }
                composable("settings") { SettingsPanel(settings,vm::settings) }
                composable("about") { AboutScreen() }
                composable("backup") { BackupScreen(vm) }
                composable("glossary") { GlossaryScreen(openSource) }
                composable(
                    "study?chapter={chapter}&review={review}",
                    arguments=listOf(
                        navArgument("chapter") { type=NavType.StringType; defaultValue="" },
                        navArgument("review") { type=NavType.BoolType; defaultValue=false }
                    )
                ) { e ->
                    StudyScreen(
                        vm, openSource,
                        initialChapterId=e.arguments?.getString("chapter")?.takeIf {it.isNotEmpty()},
                        initialTab=if(e.arguments?.getBoolean("review")==true) 3 else if(!e.arguments?.getString("chapter").isNullOrEmpty()) 1 else 0
                    )
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
                    else Reader(
                        vm, chapter, settings,
                        e.arguments?.getString("paragraph")?.takeIf {it.isNotEmpty()},
                        e.arguments?.getString("query").orEmpty(),
                        forceTop=e.arguments?.getBoolean("top") == true,
                        onStudy={navigate("study?chapter=${Uri.encode(it)}")},
                        onOpenParagraph=openSource,
                        onBack={ nav.popBackStack() },
                        navigate={ id ->
                            val fromIndex = chapters.indexOfFirst { it.id == chapter.id }
                            val toIndex = chapters.indexOfFirst { it.id == id }
                            if (fromIndex >= 0 && toIndex >= 0) readerForward = toIndex >= fromIndex
                            nav.navigate(readerRoute(id, top = true)) { popUpTo(e.destination.id) { inclusive=true } }
                        }
                    )
                }
            }
            }
        }
        }
    }
    }
}
