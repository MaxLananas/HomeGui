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
        if (instance == null) {
            instance = new LangManager();
        }
        return instance;
    }
    
    public Language getCurrentLang() {
        return currentLang;
    }
    
    public void setLanguage(Language lang) {
        this.currentLang = lang;
        ModConfig.getInstance().setLanguage(lang.code);
    }
    
    public void toggleLanguage() {
        if (currentLang == Language.ENGLISH) {
            setLanguage(Language.FRENCH);
        } else {
            setLanguage(Language.ENGLISH);
        }
    }
    
    public void loadFromConfig() {
        String code = ModConfig.getInstance().getLanguage();
        if (code.equals("fr")) {
            currentLang = Language.FRENCH;
        } else {
            currentLang = Language.ENGLISH;
        }
    }
    
    // ═══════════════════════════════════════════
    // TRADUCTIONS
    // ═══════════════════════════════════════════
    
    public String get(String key) {
        boolean fr = currentLang == Language.FRENCH;
        
        return switch (key) {
            // Titres
            case "title.homes" -> fr ? "MES HOMES" : "MY HOMES";
            case "title.stats" -> fr ? "STATISTIQUES" : "STATISTICS";
            case "title.history" -> fr ? "HISTORIQUE RECENT" : "RECENT HISTORY";
            
            // Boutons
            case "button.refresh" -> fr ? "Actualiser" : "Refresh";
            case "button.recent" -> fr ? "Recents" : "Recent";
            case "button.close" -> fr ? "Fermer" : "Close";
            case "button.back" -> fr ? "Retour" : "Back";
            case "button.clear" -> fr ? "Effacer" : "Clear";
            
            // Messages
            case "message.no_homes" -> fr ? "Aucun home trouve" : "No homes found";
            case "message.no_history" -> fr ? "Aucun historique" : "No history";
            case "message.no_results" -> fr ? "Aucun resultat pour" : "No results for";
            case "message.loading" -> fr ? "Chargement" : "Loading";
            case "message.sethome" -> fr ? "/sethome <nom>" : "/sethome <name>";
            case "message.tp_to" -> fr ? "TP vers" : "TP to";
            case "message.refreshing" -> fr ? "Actualisation..." : "Refreshing...";
            case "message.click_to_tp" -> fr ? "Clic pour TP" : "Click to TP";
            
            // Stats
            case "stats.total_homes" -> fr ? "homes enregistres" : "registered homes";
            case "stats.favorites" -> fr ? "favoris" : "favorites";
            case "stats.total_tp" -> fr ? "teleportations" : "teleportations";
            case "stats.top_homes" -> fr ? "TOP 5 HOMES" : "TOP 5 HOMES";
            case "stats.visits" -> fr ? "visite" : "visit";
            case "stats.visits_plural" -> fr ? "visites" : "visits";
            
            // Favoris
            case "favorite.added" -> fr ? "Ajoute aux favoris" : "Added to favorites";
            case "favorite.removed" -> fr ? "Retire des favoris" : "Removed from favorites";
            case "favorite.right_click" -> fr ? "Clic droit = favori" : "Right click = favorite";
            case "favorite.is_favorite" -> fr ? "Favori" : "Favorite";
            
            // Thèmes
            case "theme.label" -> fr ? "Theme" : "Theme";
            case "theme.violet" -> fr ? "Violet" : "Violet";
            case "theme.ocean" -> fr ? "Ocean" : "Ocean";
            case "theme.forest" -> fr ? "Foret" : "Forest";
            case "theme.sunset" -> fr ? "Crepuscule" : "Sunset";
            case "theme.cherry" -> fr ? "Cerise" : "Cherry";
            
            // Modes
            case "mode.compact" -> fr ? "Mode compact" : "Compact mode";
            case "mode.grid" -> fr ? "Mode grille" : "Grid mode";
            
            // Crédits
            case "credits" -> fr ? "Cree par" : "Made by";
            
            // Langue
            case "language.changed" -> fr ? "Langue: Francais" : "Language: English";
            
            // Historique
            case "history.clear_confirm" -> fr ? "Historique efface" : "History cleared";
            case "history.ago" -> fr ? "il y a" : "ago";
            
            default -> key;
        };
    }
}