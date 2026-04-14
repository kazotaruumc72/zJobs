package fr.maxlego08.jobs.zcore.utils;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.component.PaperComponent;
import fr.maxlego08.jobs.zcore.enums.Message;
import fr.maxlego08.jobs.zcore.enums.MessageType;
import fr.maxlego08.jobs.zcore.utils.nms.NmsVersion;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allows you to manage messages sent to players and the console.
 * Provides various utility methods for sending and formatting messages.
 * Extends {@link LocationUtils}.
 *
 * @see LocationUtils
 */
public abstract class MessageUtils extends LocationUtils {

    private final static int CENTER_PX = 154;
    private static final long MILLIS_PER_TICK = 50L;
    private static final PaperComponent PAPER_COMPONENT = new PaperComponent();

    /**
     * Sends a message without prefix to the specified command sender.
     * Supports MiniMessage formatting.
     *
     * @param player  the command sender to send the message to.
     * @param message the message to send.
     * @param args    the arguments for the message.
     */
    protected void messageWO(CommandSender player, Message message, Object... args) {
        player.sendMessage(PAPER_COMPONENT.getComponent(getMessage(message, args)));
    }

    /**
     * Sends a message without prefix to the specified command sender.
     * Supports MiniMessage formatting.
     *
     * @param player  the command sender to send the message to.
     * @param message the message to send.
     * @param args    the arguments for the message.
     */
    protected void messageWO(CommandSender player, String message, Object... args) {
        player.sendMessage(PAPER_COMPONENT.getComponent(getMessage(message, args)));
    }

    /**
     * Sends a message to the specified command sender using MiniMessage formatting.
     *
     * @param sender  the command sender to send the message to.
     * @param message the message to send (already formatted with prefix if needed).
     */
    private void sendComponent(CommandSender sender, String message) {
        sender.sendMessage(PAPER_COMPONENT.getComponent(message));
    }

    /**
     * Sends a chat message to the specified player using MiniMessage formatting.
     *
     * @param player  the player to send the message to.
     * @param message the message to send.
     * @param args    the arguments for the message.
     */
    private void sendTchatMessage(Player player, Message message, Object... args) {
        if (message.getMessages().size() > 1) {
            message.getMessages().forEach(msg -> sendComponent(player, this.papi(getMessage(msg, args), player)));
        } else {
            sendComponent(player, this.papi((message.getType() == MessageType.WITHOUT_PREFIX ? "" : Message.PREFIX.msg()) + getMessage(message, args), player));
        }
    }

    /**
     * Allows you to send a message to a command sender.
     *
     * @param plugin  the plugin instance.
     * @param sender  the user who sent the command.
     * @param message the message - using the Message enum for simplified message management.
     * @param args    the arguments - the arguments work in pairs, you must put for example %test% and then the value.
     */
    protected void message(JobsPlugin plugin, CommandSender sender, Message message, Object... args) {

        if (sender instanceof ConsoleCommandSender) {
            if (!message.getMessages().isEmpty()) {
                message.getMessages().forEach(msg -> sendComponent(sender, getMessage(msg, args)));
            } else {
                sendComponent(sender, Message.PREFIX.msg() + getMessage(message, args));
            }
        } else {
            Player player = (Player) sender;
            switch (message.getType()) {
                case CENTER -> {
                    var paperComponent = plugin.getPaperComponent();
                    if (!message.getMessages().isEmpty()) {
                        message.getMessages().forEach(msg -> sender.sendMessage(paperComponent.getComponent(this.getCenteredMessage(this.papi(getMessage(msg, args), player)))));
                    } else {
                        sender.sendMessage(paperComponent.getComponent(this.getCenteredMessage(this.papi(getMessage(message, args), player))));
                    }
                }
                case ACTION -> this.actionMessage(player, message, args);
                case TCHAT_AND_ACTION -> {
                    this.actionMessage(player, message, args);
                    sendTchatMessage(player, message, args);
                }
                case TCHAT, WITHOUT_PREFIX -> sendTchatMessage(player, message, args);
                case TITLE -> {
                    String title = message.getTitle();
                    String subTitle = message.getSubTitle();
                    int fadeInTime = message.getStart();
                    int showTime = message.getTime();
                    int fadeOutTime = message.getEnd();
                    this.title(player, this.papi(this.getMessage(title, args), player), this.papi(this.getMessage(subTitle, args), player), fadeInTime, showTime, fadeOutTime);
                }
                default -> {
                }
            }
        }
    }

    /**
     * Sends an action bar message to the specified player using MiniMessage formatting.
     *
     * @param player  the player to send the message to.
     * @param message the message to send.
     * @param args    the arguments for the message.
     */
    protected void actionMessage(Player player, Message message, Object... args) {
        player.sendActionBar(PAPER_COMPONENT.getComponent(this.papi(getMessage(message, args), player)));
    }

    /**
     * Gets the formatted message with arguments replaced.
     *
     * @param message the message to format.
     * @param args    the arguments for the message.
     * @return the formatted message.
     */
    protected String getMessage(Message message, Object... args) {
        return getMessage(message.getMessage(), args);
    }

    /**
     * Gets the formatted message with arguments replaced.
     *
     * @param message the message to format.
     * @param args    the arguments for the message.
     * @return the formatted message.
     */
    protected String getMessage(String message, Object... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Number of invalid arguments. Arguments must be in pairs.");
        }

        for (int i = 0; i < args.length; i += 2) {
            if (args[i] == null || args[i + 1] == null) {
                throw new IllegalArgumentException("Keys and replacement values must not be null.");
            }
            message = message.replace(args[i].toString(), args[i + 1].toString());
        }
        return message;
    }

    /**
     * Gets a class from the net.minecraft.server package.
     *
     * @param name the name of the class.
     * @return the class object, or null if not found.
     */
    protected final Class<?> getNMSClass(String name) {
        try {
            return Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + "." + name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Sends a title to the player using MiniMessage formatting.
     *
     * @param player      the player to send the title to.
     * @param title       the title text.
     * @param subtitle    the subtitle text.
     * @param fadeInTime  the fade-in time in ticks.
     * @param showTime    the showtime in ticks.
     * @param fadeOutTime the fade-out time in ticks.
     */
    protected void title(Player player, String title, String subtitle, int fadeInTime, int showTime, int fadeOutTime) {
        player.showTitle(Title.title(
                PAPER_COMPONENT.getComponent(title),
                PAPER_COMPONENT.getComponent(subtitle),
                Title.Times.times(
                        Duration.ofMillis(fadeInTime * MILLIS_PER_TICK),
                        Duration.ofMillis(showTime * MILLIS_PER_TICK),
                        Duration.ofMillis(fadeOutTime * MILLIS_PER_TICK)
                )
        ));
    }

    /**
     * Gets a centered message.
     * Supports MiniMessage formatting by stripping tags for width calculation.
     *
     * @param message the message to center.
     * @return the centered message in MiniMessage format with prepended spaces.
     */
    protected String getCenteredMessage(String message) {
        if (message == null || message.equals("")) {
            return "";
        }

        // Convert legacy codes to MiniMessage format and get plain text for width calculation
        String miniMessageFormatted = PAPER_COMPONENT.colorMiniMessage(message);
        String plainText = PAPER_COMPONENT.getPlainText(message);

        int messagePxSize = 0;

        for (char c : plainText.toCharArray()) {
            DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
            messagePxSize += dFI.getLength();
            messagePxSize++;
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PX - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }
        return sb + miniMessageFormatted;
    }

    /**
     * Translates alternate color codes in the message string.
     *
     * @param message the message to color.
     * @return the colored message.
     */
    protected String color(String message) {
        if (message == null) {
            return null;
        }
        if (NmsVersion.nmsVersion.isHexVersion()) {
            Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
            Matcher matcher = pattern.matcher(message);
            while (matcher.find()) {
                String color = message.substring(matcher.start(), matcher.end());
                message = message.replace(color, String.valueOf(net.md_5.bungee.api.ChatColor.of(color)));
                matcher = pattern.matcher(message);
            }
        }
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}
