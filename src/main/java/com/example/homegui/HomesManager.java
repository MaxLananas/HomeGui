package com.example.homegui;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HomesManager {
    
    private static HomesManager instance;
    private final List<String> homes = new ArrayList<>();
    private boolean isWaitingForResponse = false;
    private long lastRequestTime = 0;
    
    // Patterns pour différents formats de réponse serveur
    // Adaptez ces patterns selon votre serveur!
    private static final Pattern[] HOME_PATTERNS = {
        // Format: "Homes: home1, home2, home3"
        Pattern.compile("(?i)homes?\\s*:\\s*(.+)", Pattern.CASE_INSENSITIVE),
        // Format: "Vos homes: home1, home2"
        Pattern.compile("(?i)vos\\s+homes?\\s*:\\s*(.+)", Pattern.CASE_INSENSITIVE),
        // Format: "[Home] Liste: home1 | home2 | home3"
        Pattern.compile("(?i)\\[home\\].*?:\\s*(.+)", Pattern.CASE_INSENSITIVE),
        // Format EssentialsX: "Homes: [home1] [home2]"
        Pattern.compile("(?i)homes?.*?(?:\\[([^\\]]+)\\])+", Pattern.CASE_INSENSITIVE)
    };
    
    private HomesManager() {}
    
    public static HomesManager getInstance() {
        if (instance == null) {
            instance = new HomesManager();
        }
        return instance;
    }
    
    /**
     * Envoie la commande /homes au serveur
     */
    public void requestHomes() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            isWaitingForResponse = true;
            lastRequestTime = System.currentTimeMillis();
            
            // Envoyer la commande au serveur
            client.player.networkHandler.sendChatCommand("homes");
            
            HomeGuiClient.LOGGER.info("Demande de la liste des homes...");
        }
    }
    
    /**
     * Appelé par le mixin quand un message chat est reçu
     */
    public void onChatMessage(String message) {
        // Ignorer si on n'attend pas de réponse ou si trop de temps s'est écoulé
        if (!isWaitingForResponse) return;
        if (System.currentTimeMillis() - lastRequestTime > 5000) {
            isWaitingForResponse = false;
            return;
        }
        
        // Nettoyer le message des codes de couleur
        String cleanMessage = message.replaceAll("§[0-9a-fklmnor]", "").trim();
        
        HomeGuiClient.LOGGER.info("Message reçu: " + cleanMessage);
        
        // Essayer de parser le message
        if (parseHomesMessage(cleanMessage)) {
            isWaitingForResponse = false;
            HomeGuiClient.LOGGER.info("Homes trouvés: " + homes);
        }
    }
    
    /**
     * Parse le message pour extraire les homes
     */
    private boolean parseHomesMessage(String message) {
        // Pattern pour les homes entre crochets [home1] [home2]
        Pattern bracketPattern = Pattern.compile("\\[([^\\]]+)\\]");
        Matcher bracketMatcher = bracketPattern.matcher(message);
        
        List<String> foundHomes = new ArrayList<>();
        
        // D'abord essayer de trouver des homes entre crochets
        while (bracketMatcher.find()) {
            String home = bracketMatcher.group(1).trim();
            if (!home.isEmpty() && !home.equalsIgnoreCase("home")) {
                foundHomes.add(home);
            }
        }
        
        if (!foundHomes.isEmpty()) {
            homes.clear();
            homes.addAll(foundHomes);
            return true;
        }
        
        // Sinon essayer les autres patterns
        for (Pattern pattern : HOME_PATTERNS) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String homesList = matcher.group(1);
                parseHomesList(homesList);
                return !homes.isEmpty();
            }
        }
        
        // Dernier essai: si le message contient "home" et des virgules
        if (message.toLowerCase().contains("home")) {
            String[] parts = message.split("[:,]");
            for (int i = 1; i < parts.length; i++) {
                String home = parts[i].trim()
                    .replaceAll("[\\[\\](){}]", "")
                    .trim();
                if (!home.isEmpty() && home.length() < 30) {
                    if (homes.isEmpty()) homes.clear();
                    homes.add(home);
                }
            }
            return !homes.isEmpty();
        }
        
        return false;
    }
    
    /**
     * Parse une liste de homes séparés par des virgules, espaces, |, etc.
     */
    private void parseHomesList(String homesList) {
        homes.clear();
        
        // Séparer par virgules, |, espaces multiples
        String[] parts = homesList.split("[,|\\s]+");
        
        for (String part : parts) {
            String home = part.trim()
                .replaceAll("[\\[\\](){}]", "") // Retirer crochets/parenthèses
                .trim();
            
            if (!home.isEmpty() && home.length() < 30) {
                homes.add(home);
            }
        }
    }
    
    /**
     * Téléporte le joueur à un home
     */
    public void teleportToHome(String homeName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.networkHandler.sendChatCommand("home " + homeName);
            client.setScreen(null); // Fermer le GUI
        }
    }
    
    /**
     * Retourne la liste des homes
     */
    public List<String> getHomes() {
        return new ArrayList<>(homes);
    }
    
    /**
     * Ajoute un home manuellement (pour debug)
     */
    public void addHome(String home) {
        if (!homes.contains(home)) {
            homes.add(home);
        }
    }
    
    /**
     * Vide la liste des homes
     */
    public void clearHomes() {
        homes.clear();
    }
    
    public boolean isWaiting() {
        return isWaitingForResponse;
    }
}