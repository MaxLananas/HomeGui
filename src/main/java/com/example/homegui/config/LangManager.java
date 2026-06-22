package com.example.homegui.config;

public class LangManager {

    public enum Language {
        ENGLISH("en", "English"),
        FRENCH("fr", "Français");

        public final String code;
        public final String displayName;

        Language(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
    }

    private static LangManager instance;
    private Language currentLang = Language.ENGLISH;

    private LangManager() {}

    public static LangManager getInstance() {
        if (instance == null) instance = new LangManager();
        return instance;
    }

    public Language getCurrentLang()       { return currentLang; }

    public void setLanguage(Language lang) {
        currentLang = lang;
        ModConfig.getInstance().setLanguage(lang.code);
    }

    public void toggleLanguage() {
        setLanguage(currentLang == Language.ENGLISH ? Language.FRENCH : Language.ENGLISH);
    }

    public void loadFromConfig() {
        currentLang = ModConfig.getInstance().getLanguage().equals("fr")
                ? Language.FRENCH : Language.ENGLISH;
    }

    public String get(String key) {
        boolean fr = currentLang == Language.FRENCH;
        return switch (key) {
            case "title.homes"         -> fr ? "MES HOMES"           : "MY HOMES";
            case "title.stats"         -> fr ? "STATISTIQUES"        : "STATISTICS";
            case "title.history"       -> fr ? "HISTORIQUE RECENT"   : "RECENT HISTORY";
            case "button.refresh"      -> fr ? "Actu."               : "Refresh";
            case "button.recent"       -> fr ? "Recents"             : "Recent";
            case "button.close"        -> fr ? "Fermer"              : "Close";
            case "button.back"         -> fr ? "Retour"              : "Back";
            case "button.clear"        -> fr ? "Effacer"             : "Clear";
            case "message.no_homes"    -> fr ? "Aucun home trouve"   : "No homes found";
            case "message.no_history"  -> fr ? "Aucun historique"    : "No history";
            case "message.no_results"  -> fr ? "Aucun resultat pour" : "No results for";
            case "message.loading"     -> fr ? "Chargement..."       : "Loading...";
            case "message.click_to_tp" -> fr ? "Clic pour TP"        : "Click to TP";
            case "stats.total_homes"   -> fr ? "homes"               : "homes";
            case "stats.favorites"     -> fr ? "favoris"             : "favorites";
            case "stats.total_tp"      -> fr ? "teleports"           : "teleports";
            case "stats.top_homes"     -> "TOP 5 HOMES";
            case "stats.visits"        -> fr ? "visite"              : "visit";
            case "stats.visits_plural" -> fr ? "visites"             : "visits";
            case "favorite.right_click"-> fr ? "Clic droit = favori" : "Right click = fav";
            default                    -> key;
        };
    }
}
