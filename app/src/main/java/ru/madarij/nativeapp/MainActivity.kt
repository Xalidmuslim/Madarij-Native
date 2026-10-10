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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
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
        // Start bitmap decoding concurrently with composition, never on the UI thread.
        BookImagePreloader.preload(applicationContext)
        setContent { BookApp() }
    }
}

internal fun readerRoute(id:String,paragraph:String?=null,query:String="",top:Boolean=false) =
    "read/${Uri.encode(id)}?paragraph=${Uri.encode(paragraph.orEmpty())}&query=${Uri.encode(query)}&top=$top"

@Composable
private fun BookRouteSurface(
    texture: Int?,
    dark: Boolean,
    motionEnabled: Boolean = true,
    title: String? = null,
    subtitle: String? = null,
    showSearch: Boolean = true,
    showTextSettings: Boolean = true,
    onBack: () -> Unit = {},
    onSearch: () -> Unit = {},
    onTextSettings: () -> Unit = {},
    content: @Composable () -> Unit
) {
    // The paper background remains fully opaque on the first frame.
    // Only the text/controls receive a tiny 130ms opacity polish; no translated
    // text, crossfading of old screens or additional full-screen paper layers.
    var entryShown by remember { mutableStateOf(false) }
    LaunchedEffect(motionEnabled) { entryShown = true }
    val entranceAlpha by animateFloatAsState(
        targetValue = if (!motionEnabled || entryShown) 1f else .88f,
        animationSpec = tween(durationMillis = 130, easing = FastOutSlowInEasing),
        label = "paper-content-entrance"
    )
    Box(
        Modifier.fillMaxSize().background(
            if (dark) BookColors.nightBackground else BookColors.parchment
        )
    ) {
        if (!dark && texture != null) {
            PreloadedBookImage(texture, Modifier.matchParentSize(), ContentScale.FillBounds)
        }
        // Tear down the temporary alpha layer once the transition completes,
        // avoiding extra GPU compositing during reading and scrolling.
        val transientEffect = if (entranceAlpha < .999f)
            Modifier.graphicsLayer { alpha = entranceAlpha }
        else Modifier
        Column(Modifier.fillMaxSize().then(transientEffect)) {
            if (title != null) MadarijTopBar(
                title = title, subtitle = subtitle, canBack = false,
                onBack = onBack, showSearch = showSearch,
                onSearch = onSearch, showTextSettings = showTextSettings,
                onTextSettings = onTextSettings
            )
            Box(Modifier.weight(1f)) { content() }
        }
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
    // Fix: keep the APP-WIDE theme stable while entering/leaving Reader.
    // Only the Reader route uses the selected paperTone. This avoids a complete
    // MaterialTheme recomposition of the nav bar and every preserved screen.
    val colors = remember(settings.theme) {
        bookReaderColors(
            settings.copy(theme = if (settings.theme == "dark") "dark" else "sepia", paperTone = "warm"),
            false
        )
    }
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
        // Selected once for each new app launch, preserved across navigation/rotation.
        val knowledgeReminderIndex = rememberSaveable { KnowledgeReminders.nextForLaunch(context.applicationContext) }
        val knowledgeReminder = KnowledgeReminders.items[knowledgeReminderIndex]
        LaunchedEffect(Unit) {
            // Fresh installation defaults only; existing preferences are retained.
            if (!java.io.File(context.filesDir, "datastore/reading_settings.preferences_pb").exists())
                vm.settings(settings.copy(theme = "sepia", paperTone = "warm", russianFont = "sans", russianSize = 18f, lineHeight = 1.5f))
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

        val tabs = listOf("home" to "Главная", "contents" to "Оглавление", "bookmarks" to "Закладки", "search" to "Поиск", "more" to "Ещё")
        val selectedTab = when { route == "bookmarks" -> "bookmarks"; route == "search" -> "search"; else -> selectedMainTab(route).let { if (it == "study") "more" else it } }
        val primaryBack = primaryBackAction(route)

        BackHandler(enabled = primaryBack != PrimaryBackAction.POP) {
            if (primaryBack == PrimaryBackAction.HOME && !nav.popBackStack("home", false)) {
                nav.navigate("home") { launchSingleTop = true }
            }
        }

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
        val navigate:(String)->Unit = { destination ->
            // Re-selecting the active section must not recompose a fresh screen.
            if (route != destination) nav.navigate(destination) { launchSingleTop = true }
        }

        Box(Modifier.fillMaxSize().background(colors.background)) {
            // Destination already owns an opaque paper layer under the floating bar.
            // The redundant root bitmap used to double full-screen GPU overdraw.
            Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {},
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
                composable("home") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark", motionEnabled = !settings.reducedMotion) { HomeScreen(vm,{id,p -> openReader(id,p,"")},navigate,knowledgeReminder,settings.reducedMotion) } }
                composable("contents") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark", motionEnabled = !settings.reducedMotion, title = "Оглавление", subtitle = "Главы, темы и место чтения", showSearch = true, showTextSettings = true, onBack = { if (!nav.popBackStack()) nav.navigate("home") }, onSearch = { navigate("search") }, onTextSettings = { navigate("settings") }) { ContentsScreen(vm,openFromContents) { chapter -> openReader(chapter,null,"") } } }
                composable("search") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark", motionEnabled = !settings.reducedMotion, title = "Поиск", subtitle = "По всему первому тому", showSearch = false, showTextSettings = false, onBack = { if (!nav.popBackStack()) nav.navigate("home") }, onSearch = { navigate("search") }, onTextSettings = { navigate("settings") }) { SearchScreen(vm,openReader) } }
                composable("bookmarks") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark", motionEnabled = !settings.reducedMotion, title = "Закладки", subtitle = "Сохранённые места", showSearch = true, showTextSettings = true, onBack = { if (!nav.popBackStack()) nav.navigate("home") }, onSearch = { navigate("search") }, onTextSettings = { navigate("settings") }) { BookmarksScreen(vm) {id,p -> openReader(id,p,"")} } }
                composable("notes") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark", motionEnabled = !settings.reducedMotion, title = "Мои заметки", subtitle = "Личные записи", showSearch = true, showTextSettings = true, onBack = { if (!nav.popBackStack()) nav.navigate("home") }, onSearch = { navigate("search") }, onTextSettings = { navigate("settings") }) { NotesScreen(vm) {id,p -> openReader(id,p,"")} } }
                composable("progress") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark", motionEnabled = !settings.reducedMotion, title = "Прогресс", subtitle = "Чтение и усвоение", showSearch = true, showTextSettings = true, onBack = { if (!nav.popBackStack()) nav.navigate("home") }, onSearch = { navigate("search") }, onTextSettings = { navigate("settings") }) { ProgressScreen(vm,{id,p -> openReader(id,p,"")},navigate) } }
                composable("more") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark", motionEnabled = !settings.reducedMotion, title = "Ещё", subtitle = "", showSearch = true, showTextSettings = true, onBack = { if (!nav.popBackStack()) nav.navigate("home") }, onSearch = { navigate("search") }, onTextSettings = { navigate("settings") }) { MoreScreen(navigate) } }
                composable("settings") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark", motionEnabled = !settings.reducedMotion, title = "Настройки", subtitle = "Текст, тема и чтение", showSearch = false, showTextSettings = false, onBack = { if (!nav.popBackStack()) nav.navigate("home") }, onSearch = { navigate("search") }, onTextSettings = { navigate("settings") }) { SettingsPanel(settings,vm::settings) } }
                composable("about") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark", motionEnabled = !settings.reducedMotion, title = "О книге", subtitle = "", showSearch = true, showTextSettings = true, onBack = { if (!nav.popBackStack()) nav.navigate("home") }, onSearch = { navigate("search") }, onTextSettings = { navigate("settings") }) { AboutScreen() } }
                composable("backup") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark", motionEnabled = !settings.reducedMotion, title = "Резервная копия", subtitle = "", showSearch = true, showTextSettings = true, onBack = { if (!nav.popBackStack()) nav.navigate("home") }, onSearch = { navigate("search") }, onTextSettings = { navigate("settings") }) { BackupScreen(vm) } }
                composable("glossary") { BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark", motionEnabled = !settings.reducedMotion, title = "Словарь", subtitle = "Термины и контекст", showSearch = true, showTextSettings = true, onBack = { if (!nav.popBackStack()) nav.navigate("home") }, onSearch = { navigate("search") }, onTextSettings = { navigate("settings") }) { GlossaryScreen(openSource) } }
                composable(
                    "study?chapter={chapter}&review={review}",
                    arguments=listOf(
                        navArgument("chapter") { type=NavType.StringType; defaultValue="" },
                        navArgument("review") { type=NavType.BoolType; defaultValue=false }
                    )
                ) { e ->
                    BookRouteSurface(R.drawable.reference_paper, settings.theme == "dark", motionEnabled = !settings.reducedMotion, title = "Изучение", subtitle = "Проверки, задания и повторение", onBack = { if (!nav.popBackStack()) nav.navigate("home") }, onSearch = { navigate("search") }, onTextSettings = { navigate("settings") }) { StudyScreen(
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
                    if(chapter==null) BookRouteSurface(readerPaperTexture, settings.theme == "dark", motionEnabled = !settings.reducedMotion) { }
                    else BookRouteSurface(readerPaperTexture, settings.theme == "dark", motionEnabled = !settings.reducedMotion) { MaterialTheme(colorScheme = bookReaderColors(settings, false)) { Reader(
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
                    ) } }
                }
            }
            Box(
                Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 15.dp).padding(bottom = 4.dp)
            ) {
MadarijBottomBar(tabs, selectedTab) { destination ->
    when {
        destination == "home" -> {
            // On HOME a repeated tap is a strict no-op; there is no new route,
            // no saved-state reset and no unnecessary first-frame redraw.
            if (route != "home" && !nav.popBackStack("home", false)) {
                nav.navigate("home") {
                    popUpTo(nav.graph.startDestinationId) { inclusive=false }
                    launchSingleTop = true
                }
            }
        }
        destination == route -> Unit
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
