package com.maxlananas.homegui.config;

import java.util.*;

public class LangManager {

    private static final String[] KEYS = {
        "title.homes", "title.stats", "title.history",
        "button.refresh", "button.recent", "button.close", "button.back",
        "button.clear", "button.settings", "button.export", "button.import",
        "message.no_homes", "message.no_history", "message.no_results",
        "message.loading", "message.click_to_tp", "message.fav_tip",
        "message.exported", "message.imported", "message.import_error",
        "stats.total_homes", "stats.favorites", "stats.total_tp",
        "stats.top_homes", "stats.visits", "stats.visits_plural", "stats.no_data",
        "hint.search", "hint.create_home",
        "settings.language", "settings.sort", "settings.view", "settings.compact",
        "sort.default", "sort.alphabetical", "sort.most_used", "sort.recent", "sort.favorites_first",
        "view.list", "view.grid",
        "settings.transparent"
    };

    private static final Map<String, String[]> LANGS = new HashMap<>();

    private static String[] lang(String... v) { return v; }

    static {
        LANGS.put("en", lang(
            "MY HOMES","STATISTICS","RECENT HISTORY",
            "Refresh","Recent","Close","Back","Clear","Settings","Export","Import",
            "No homes found","No history yet","No results for","Loading…",
            "Click = TP","★ Right click = fav","Exported to config dir","Imported %d homes","Import failed",
            "homes","favorites","teleports","TOP 5 HOMES","visit","visits","No data available",
            "Search…","Use /sethome <name>",
            "Language","Sort","View","Compact",
            "Default","Alphabetical","Most used","Recent","Favorites first",
            "List","Grid",
            "Transparent"
        ));
        LANGS.put("fr", lang(
            "MES HOMES","STATISTIQUES","HISTORIQUE RÉCENT",
            "Actualiser","Récents","Fermer","Retour","Effacer","Paramètres","Exporter","Importer",
            "Aucun home trouvé","Aucun historique","Aucun résultat pour","Chargement…",
            "Clic = TP","★ Clic droit = favori","Exporté dans le dossier config","%d homes importés","Erreur d'import",
            "homes","favoris","téléports","TOP 5 HOMES","visite","visites","Aucune donnée",
            "Rechercher…","Tapez /sethome <nom>",
            "Langue","Tri","Vue","Compact",
            "Défaut","Alphabétique","Plus utilisés","Récents","Favoris d'abord",
            "Liste","Grille",
            "Transparent"
        ));
        LANGS.put("es", lang(
            "MIS HOMES","ESTADÍSTICAS","HISTORIAL RECIENTE",
            "Actualizar","Recientes","Cerrar","Volver","Limpiar","Ajustes","Exportar","Importar",
            "No se encontraron homes","Sin historial","Sin resultados para","Cargando…",
            "Clic = TP","★ Clic derecho = favorito","Exportado al directorio de config","%d homes importados","Error de importación",
            "homes","favoritos","teletransportes","TOP 5 HOMES","visita","visitas","Sin datos",
            "Buscar…","Usa /sethome <nombre>",
            "Idioma","Orden","Vista","Compacto",
            "Por defecto","Alfabético","Más usados","Recientes","Favoritos primero",
            "Lista","Cuadrícula"
        ));
        LANGS.put("de", lang(
            "MEINE HOMES","STATISTIKEN","LETZTE HISTORIE",
            "Aktualisieren","Letzte","Schließen","Zurück","Löschen","Einstellungen","Exportieren","Importieren",
            "Keine Homes gefunden","Keine Historie","Keine Ergebnisse für","Laden…",
            "Klick = TP","★ Rechtsklick = Favorit","In Konfigordner exportiert","%d Homes importiert","Importfehler",
            "Homes","Favoriten","Teleports","TOP 5 HOMES","Besuch","Besuche","Keine Daten",
            "Suchen…","Nutze /sethome <name>",
            "Sprache","Sortierung","Ansicht","Kompakt",
            "Standard","Alphabetisch","Meistgenutzt","Neueste","Favoriten zuerst",
            "Liste","Raster"
        ));
        LANGS.put("pt", lang(
            "MEUS HOMES","ESTATÍSTICAS","HISTÓRICO RECENTE",
            "Atualizar","Recentes","Fechar","Voltar","Limpar","Configurações","Exportar","Importar",
            "Nenhum home encontrado","Sem histórico","Sem resultados para","Carregando…",
            "Clique = TP","★ Clique direito = favorito","Exportado para a pasta de config","%d homes importados","Erro de importação",
            "homes","favoritos","teleportes","TOP 5 HOMES","visita","visitas","Sem dados",
            "Pesquisar…","Use /sethome <nome>",
            "Idioma","Ordenar","Visualização","Compacto",
            "Padrão","Alfabético","Mais usados","Recentes","Favoritos primeiro",
            "Lista","Grade"
        ));
        LANGS.put("it", lang(
            "I MIEI HOME","STATISTICHE","CRONOLOGIA RECENTE",
            "Aggiorna","Recenti","Chiudi","Indietro","Cancella","Impostazioni","Esporta","Importa",
            "Nessun home trovato","Nessuna cronologia","Nessun risultato per","Caricamento…",
            "Click = TP","★ Click destro = preferito","Esportato nella cartella config","%d home importati","Errore importazione",
            "home","preferiti","teletrasporti","TOP 5 HOMES","visita","visite","Nessun dato",
            "Cerca…","Usa /sethome <nome>",
            "Lingua","Ordine","Vista","Compatto",
            "Predefinito","Alfabetico","Più usati","Recenti","Preferiti prima",
            "Lista","Griglia"
        ));
        LANGS.put("nl", lang(
            "MIJN HOMES","STATISTIEKEN","RECENTE GESCHIEDENIS",
            "Vernieuwen","Recent","Sluiten","Terug","Wissen","Instellingen","Exporteren","Importeren",
            "Geen homes gevonden","Geen geschiedenis","Geen resultaten voor","Laden…",
            "Klik = TP","★ Rechtsklik = favoriet","Geëxporteerd naar config map","%d homes geïmporteerd","Import fout",
            "homes","favorieten","teleports","TOP 5 HOMES","bezoek","bezoeken","Geen gegevens",
            "Zoeken…","Gebruik /sethome <naam>",
            "Taal","Sorteren","Weergave","Compact",
            "Standaard","Alfabetisch","Meest gebruikt","Recent","Favorieten eerst",
            "Lijst","Raster"
        ));
        LANGS.put("pl", lang(
            "MOJE HOMEY","STATYSTYKI","OSTATNIA HISTORIA",
            "Odśwież","Ostatnie","Zamknij","Wstecz","Wyczyść","Ustawienia","Eksportuj","Importuj",
            "Nie znaleziono homeów","Brak historii","Brak wyników dla","Ładowanie…",
            "Klik = TP","★ PPM = ulubiony","Wyeksportowano do katalogu config","Zaimportowano %d homeów","Błąd importu",
            "homey","ulubione","teleporty","TOP 5 HOMES","wizyta","wizyty","Brak danych",
            "Szukaj…","Użyj /sethome <nazwa>",
            "Język","Sortowanie","Widok","Kompaktowy",
            "Domyślne","Alfabetycznie","Najczęściej","Ostatnio","Ulubione najpierw",
            "Lista","Siatka"
        ));
        LANGS.put("ru", lang(
            "МОИ ХОУМЫ","СТАТИСТИКА","НЕДАВНЯЯ ИСТОРИЯ",
            "Обновить","Недавние","Закрыть","Назад","Очистить","Настройки","Экспорт","Импорт",
            "Хоумы не найдены","Нет истории","Нет результатов для","Загрузка…",
            "Клик = ТП","★ ПКМ = избранное","Экспортировано в папку config","Импортировано %d хоумов","Ошибка импорта",
            "хоумы","избранные","телепорты","ТОП 5 ХОУМОВ","визит","визитов","Нет данных",
            "Поиск…","Используйте /sethome <имя>",
            "Язык","Сортировка","Вид","Компактный",
            "По умолчанию","По алфавиту","Частые","Недавние","Избранные сначала",
            "Список","Сетка"
        ));
        LANGS.put("ja", lang(
            "マイホーム","統計","最近の履歴",
            "更新","最近","閉じる","戻る","クリア","設定","エクスポート","インポート",
            "ホームが見つかりません","履歴なし","結果なし","読み込み中…",
            "クリック = TP","★ 右クリック = お気に入り","configフォルダにエクスポート","%d ホームをインポート","インポートエラー",
            "ホーム","お気に入り","テレポート","トップ5ホーム","訪問","訪問回","データなし",
            "検索…","/sethome <名前> を使用",
            "言語","並び替え","表示","コンパクト",
            "デフォルト","アルファベット順","使用頻度順","最近","お気に入り優先",
            "リスト","グリッド"
        ));
        LANGS.put("zh_cn", lang(
            "我的家","统计","最近历史",
            "刷新","最近","关闭","返回","清除","设置","导出","导入",
            "未找到家","暂无历史","无结果","加载中…",
            "点击传送","★ 右键收藏","已导出到配置目录","已导入 %d 个家","导入失败",
            "家","收藏","传送","热门5个家","次访问","次","暂无数据",
            "搜索…","使用 /sethome <名称>",
            "语言","排序","视图","紧凑",
            "默认","字母顺序","最常用","最近","收藏优先",
            "列表","网格"
        ));
        LANGS.put("ko", lang(
            "내 홈","통계","최근 기록",
            "새로고침","최근","닫기","뒤로","지우기","설정","내보내기","가져오기",
            "홈을 찾을 수 없음","기록 없음","결과 없음","로딩 중…",
            "클릭 = TP","★ 우클릭 = 즐겨찾기","설정 폴더로 내보내기","%d개 홈 가져오기","가져오기 오류",
            "홈","즐겨찾기","텔레포트","인기 홈 TOP 5","방문","회","데이터 없음",
            "검색…","/sethome <이름> 사용",
            "언어","정렬","보기","컴팩트",
            "기본","알파벳순","자주 사용","최근","즐겨찾기 우선",
            "목록","격자"
        ));
        LANGS.put("tr", lang(
            "EVLERİM","İSTATİSTİKLER","SON GEÇMİŞ",
            "Yenile","Son","Kapat","Geri","Temizle","Ayarlar","Dışa Aktar","İçe Aktar",
            "Ev bulunamadı","Geçmiş yok","Sonuç yok","Yükleniyor…",
            "Tıkla = TP","★ Sağ tık = favori","Config klasörüne aktarıldı","%d ev içe aktarıldı","İçe aktarma hatası",
            "ev","favoriler","ışınlanma","EN İYİ 5 EV","ziyaret","ziyaret","Veri yok",
            "Ara…","/sethome <isim> kullan",
            "Dil","Sıralama","Görünüm","Kompakt",
            "Varsayılan","Alfabetik","En çok kullanılan","Son","Favoriler önce",
            "Liste","Izgara"
        ));
        LANGS.put("cs", lang(
            "MÉ DOMOVY","STATISTIKY","NEDÁVNÁ HISTORIE",
            "Obnovit","Nedávné","Zavřít","Zpět","Vymazat","Nastavení","Export","Import",
            "Žádné domovy","Žádná historie","Žádné výsledky pro","Načítání…",
            "Klik = TP","★ Pravý klik = oblíbené","Exportováno do config složky","Importováno %d domovů","Chyba importu",
            "domovy","oblíbené","teleporty","TOP 5 DOMOVŮ","návštěva","návštěv","Žádná data",
            "Hledat…","Použij /sethome <jméno>",
            "Jazyk","Řazení","Zobrazení","Kompaktní",
            "Výchozí","Abecedně","Nejpoužívanější","Nedávné","Oblíbené první",
            "Seznam","Mřížka"
        ));
        LANGS.put("sv", lang(
            "MINA HEM","STATISTIK","SENASTE HISTORIK",
            "Uppdatera","Senaste","Stäng","Tillbaka","Rensa","Inställningar","Exportera","Importera",
            "Inga hittade","Ingen historik","Inga resultat för","Laddar…",
            "Klick = TP","★ Högerklick = favorit","Exporterad till config-mapp","Importerade %d hem","Importfel",
            "hem","favoriter","teleporteringar","TOPP 5 HEM","besök","besök","Inga data",
            "Sök…","Använd /sethome <namn>",
            "Språk","Sortering","Vy","Kompakt",
            "Standard","Alfabetiskt","Mest använda","Senaste","Favoriter först",
            "Lista","Rutnät"
        ));
        LANGS.put("ar", lang(
            "منزلي","إحصائيات","التاريخ الأخير",
            "تحديث","الأخيرة","إغلاق","رجوع","مسح","إعدادات","تصدير","استيراد",
            "لم يتم العثور على منازل","لا يوجد تاريخ","لا نتائج لـ","جاري التحميل…",
            "انقر = نقلك","★ نقر يمين = مفضلة","تم التصدير إلى مجلد الإعدادات","تم استيراد %d منازل","خطأ في الاستيراد",
            "منازل","مفضلة","نقل","أفضل 5 منازل","زيارة","زيارات","لا توجد بيانات",
            "بحث…","استخدم /sethome <اسم>",
            "اللغة","فرز","عرض","مضغوط",
            "افتراضي","أبجدي","الأكثر استخداماً","الأحدث","المفضلة أولاً",
            "قائمة","شبكة"
        ));
        LANGS.put("uk", lang(
            "МОЇ ХОУМИ","СТАТИСТИКА","ОСТАННЯ ІСТОРІЯ",
            "Оновити","Останні","Закрити","Назад","Очистити","Налаштування","Експорт","Імпорт",
            "Хоуми не знайдено","Немає історії","Немає результатів для","Завантаження…",
            "Клік = ТП","★ ПКМ = обране","Експортовано до папки config","Імпортовано %d хоумів","Помилка імпорту",
            "хоуми","обрані","телепорти","ТОП 5 ХОУМІВ","візит","візитів","Немає даних",
            "Пошук…","Використовуйте /sethome <ім'я>",
            "Мова","Сортування","Вигляд","Компактний",
            "За замовчуванням","За алфавітом","Часті","Останні","Обрані спочатку",
            "Список","Сітка"
        ));
    }

    private static LangManager instance;
    private String currentCode = "en";

    private LangManager() {}

    public static LangManager getInstance() {
        if (instance == null) instance = new LangManager();
        return instance;
    }

    public String getCurrentCode()         { return currentCode; }
    public String getCurrentDisplayName()  { return getLanguageName(currentCode); }

    public void setLanguage(String code) {
        currentCode = LANGS.containsKey(code) ? code : "en";
        ModConfig.getInstance().setLanguage(currentCode);
    }

    public void loadFromConfig() {
        String cfg = ModConfig.getInstance().getLanguage();
        currentCode = LANGS.containsKey(cfg) ? cfg : "en";
    }

    public String get(String key) {
        String[] translations = LANGS.get(currentCode);
        if (translations == null) translations = LANGS.get("en");
        String[] en = LANGS.get("en");
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(key)) {
                if (i < translations.length) return translations[i];
                return (i < en.length) ? en[i] : key;   // fall back to English, not the raw key
            }
        }
        return key;
    }

    public List<String> getAvailableLanguages() {
        return new ArrayList<>(LANGS.keySet());
    }

    public void cycleLanguage() {
        List<String> codes = getAvailableLanguages();
        int idx = codes.indexOf(currentCode);
        setLanguage(codes.get((idx + 1) % codes.size()));
    }

    public static String getLanguageName(String code) {
        return switch (code) {
            case "en" -> "English";
            case "fr" -> "Français";
            case "es" -> "Español";
            case "de" -> "Deutsch";
            case "pt" -> "Português";
            case "it" -> "Italiano";
            case "nl" -> "Nederlands";
            case "pl" -> "Polski";
            case "ru" -> "Русский";
            case "ja" -> "日本語";
            case "zh_cn" -> "简体中文";
            case "ko" -> "한국어";
            case "tr" -> "Türkçe";
            case "cs" -> "Čeština";
            case "sv" -> "Svenska";
            case "ar" -> "العربية";
            case "uk" -> "Українська";
            default -> code;
        };
    }
}
