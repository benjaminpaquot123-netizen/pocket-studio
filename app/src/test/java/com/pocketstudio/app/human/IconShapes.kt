package com.pocketstudio.app.human

/**
 * What an icon looks like, for the screen in text (audit « banc crédible » 2, R1): its glyph when it has one (⋮, ▶, ✕),
 * else its shape in a few French words in brackets (« (micro) », « (poubelle) »), never what it is for nor TalkBack's
 * name. Keyed by the last part of the ImageVector's name (« Filled.MoreVert » → « MoreVert »): Material's icons, the
 * app's own vectors (« Pocket.Magnet », « wave_Saw »). BenchSelfTest#everyIconOfTheAppHasItsShape keeps it whole.
 */
internal object IconShapes {
    /** An icon the table does not know: its shape is not said (the image shows it). */
    const val UNKNOWN = "(forme non décrite)"

    fun of(vectorName: String?): String {
        val key = vectorName?.substringAfterLast('.')?.takeIf { it.isNotBlank() } ?: return UNKNOWN
        return SHAPES[key] ?: UNKNOWN
    }

    val SHAPES: Map<String, String> = mapOf(
        // Glyphs: what is drawn has a sign of its own.
        "Add" to "+", "Remove" to "−", "Close" to "✕", "Clear" to "✕", "Check" to "✓",
        "MoreVert" to "⋮", "MoreHoriz" to "⋯", "PlayArrow" to "▶", "Stop" to "■", "Pause" to "❚❚",
        "FiberManualRecord" to "●", "TripOrigin" to "○", "Star" to "★", "StarBorder" to "☆",
        "ArrowBack" to "←", "ArrowForward" to "→", "ArrowUpward" to "↑", "ArrowDownward" to "↓",
        "ArrowDropDown" to "▾", "ExpandMore" to "⌄", "ExpandLess" to "⌃", "KeyboardArrowDown" to "⌄", "KeyboardArrowUp" to "⌃",
        "KeyboardArrowRight" to "›", "ChevronRight" to "›", "KeyboardDoubleArrowRight" to "»",
        "FirstPage" to "|‹", "LastPage" to "›|", "Undo" to "↶", "Redo" to "↷", "Refresh" to "↻", "Replay" to "↺",
        "SwapVert" to "⇅", "UnfoldMore" to "⇕", "UnfoldLess" to "⇳", "MusicNote" to "♪", "ContentCut" to "✂",
        "Backspace" to "⌫", "Warning" to "⚠", "CheckBox" to "☑", "CheckBoxOutlineBlank" to "☐",
        "AddCircle" to "⊕", "Adjust" to "◎", "PowerSettingsNew" to "⏻",
        // Shapes: a few words for what is drawn.
        "CheckCircle" to "(rond coché)", "StopCircle" to "(carré dans un rond)", "Info" to "(i dans un rond)",
        "HelpOutline" to "(? dans un rond)", "Mic" to "(micro)", "RecordVoiceOver" to "(tête qui parle)",
        "Delete" to "(poubelle)", "DeleteForever" to "(poubelle et croix)", "DeleteSweep" to "(poubelle et traits)",
        "Share" to "(trois points reliés)", "Settings" to "(roue dentée)", "Search" to "(loupe)", "ZoomIn" to "(loupe +)",
        "ZoomOut" to "(loupe −)", "ZoomOutMap" to "(flèches vers les coins)", "Edit" to "(crayon)", "Brush" to "(pinceau)",
        "Folder" to "(dossier)", "FolderOpen" to "(dossier ouvert)", "FileOpen" to "(feuille et flèche)",
        "Archive" to "(boîte, flèche vers le bas)", "Unarchive" to "(boîte, flèche vers le haut)",
        "Headphones" to "(casque)", "Speaker" to "(enceinte)", "VolumeUp" to "(haut-parleur et ondes)",
        "VolumeDown" to "(haut-parleur et une onde)", "VolumeOff" to "(haut-parleur barré)", "Piano" to "(touches de piano)",
        "LibraryMusic" to "(notes sur une pile)", "QueueMusic" to "(lignes et note)", "Album" to "(disque)",
        "GraphicEq" to "(barres de son)", "Equalizer" to "(barres verticales)", "Tune" to "(curseurs)",
        "Waves" to "(vagues)", "Air" to "(souffle)", "AutoAwesome" to "(étincelles)", "Bolt" to "(éclair)",
        "FlashOn" to "(éclair)", "BlurOn" to "(points en rond)", "Grain" to "(points épars)", "Texture" to "(hachures)",
        "Repeat" to "(flèches en boucle)", "Loop" to "(flèches en rond)", "RestartAlt" to "(flèche en rond)",
        "History" to "(horloge et flèche)", "Timer" to "(chronomètre)", "Speed" to "(compteur de vitesse)",
        "ContentCopy" to "(deux feuilles)", "Home" to "(maison)", "Lock" to "(cadenas)", "LockOpen" to "(cadenas ouvert)",
        "Visibility" to "(œil)", "Apps" to "(neuf points)", "GridView" to "(quatre carrés)", "ViewDay" to "(bandes empilées)",
        "ViewStream" to "(bandes)", "List" to "(liste)", "Checklist" to "(liste cochée)", "MenuBook" to "(livre ouvert)",
        "School" to "(chapeau de diplômé)", "Flag" to "(drapeau)", "EmojiEvents" to "(coupe)", "Celebration" to "(cotillons)",
        "Casino" to "(dé)", "Campaign" to "(mégaphone)", "Notifications" to "(cloche)", "NewReleases" to "(étoile à pointes)",
        "Person" to "(silhouette)", "FrontHand" to "(main)", "TouchApp" to "(doigt qui touche)", "Vibration" to "(téléphone qui vibre)",
        "PhotoCamera" to "(appareil photo)", "Image" to "(image)", "AddPhotoAlternate" to "(image et +)", "Movie" to "(clap de cinéma)",
        "Cloud" to "(nuage)", "CloudDownload" to "(nuage et flèche)", "Public" to "(globe)", "Explore" to "(boussole)",
        "Forest" to "(sapins)", "NightsStay" to "(lune)", "Hub" to "(rond relié à des points)", "Memory" to "(puce)",
        "Bluetooth" to "(signe Bluetooth)", "BugReport" to "(insecte)", "BookmarkAdd" to "(marque-page et +)",
        "ShowChart" to "(courbe)", "TrendingUp" to "(courbe qui monte)", "Timeline" to "(ligne brisée et points)",
        "ToggleOn" to "(interrupteur)", "SelectAll" to "(carré en pointillé)", "Fullscreen" to "(quatre coins)",
        "FullscreenExit" to "(quatre coins rentrés)",
        // The app's own vectors.
        "StartHere" to "(barre, onglet à gauche)", "EndHere" to "(barre, onglet à droite)", "Magnet" to "(aimant)",
        "wave_Saw" to "(dents de scie)", "wave_Square" to "(créneaux)", "wave_Triangle" to "(zigzag)",
    )
}
