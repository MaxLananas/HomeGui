package com.maxlananas.homegui.core;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds and recognises the only two commands HomeGui deals with.
 *
 * <p>Both directions go through {@link HomeNames}, so neither a name typed by the
 * player nor a name scraped out of chat can ever reach {@code sendCommand} carrying
 * extra arguments, a newline or a second command.
 */
public final class HomeCommand {

    /** Command used to ask a server for its home list. */
    public static final String LIST_COMMAND = "homes";

    private static final Pattern TELEPORT = Pattern.compile("(?iu)^/?home\\s+(\\S+)\\s*$");

    private HomeCommand() {}

    /**
     * Extracts the target of a {@code /home <name>} command that the client is about
     * to send, so that statistics follow real teleports instead of button clicks.
     *
     * @return the validated target, or empty when the text is not exactly one
     *         complete teleport command for a safe home name.
     */
    public static Optional<String> teleportTarget(String command) {
        if (command == null || command.length() > HomeNames.MAX_LENGTH + 16) return Optional.empty();
        Matcher matcher = TELEPORT.matcher(command.strip());
        return matcher.matches() ? HomeNames.validate(matcher.group(1)) : Optional.empty();
    }

    /** True when the text is exactly one complete {@code /home <name>} command. */
    public static boolean isTeleport(String command) {
        return teleportTarget(command).isPresent();
    }

    /**
     * Builds the outgoing command for a home name.
     *
     * @return {@code home <name>} without a leading slash (the form client
     *         {@code sendCommand} expects), or empty when the name is not safe.
     */
    public static Optional<String> teleport(String home) {
        return HomeNames.validate(home).map(name -> "home " + name);
    }
}
