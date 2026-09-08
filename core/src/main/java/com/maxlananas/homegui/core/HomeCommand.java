package com.maxlananas.homegui.core;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HomeCommand {
    private static final Pattern TELEPORT = Pattern.compile("(?iu)^/?home\\s+(\\S+)\\s*$");

    private HomeCommand() {}

    public static Optional<String> teleportTarget(String command) {
        if (command == null || command.length() > 256) return Optional.empty();
        Matcher matcher = TELEPORT.matcher(command.strip());
        return matcher.matches() ? HomeNames.validate(matcher.group(1)) : Optional.empty();
    }

    public static Optional<String> teleport(String home) {
        return HomeNames.validate(home).map(name -> "home " + name);
    }
}
