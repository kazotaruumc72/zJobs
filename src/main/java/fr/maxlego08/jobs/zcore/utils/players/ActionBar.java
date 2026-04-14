package fr.maxlego08.jobs.zcore.utils.players;

import fr.maxlego08.jobs.zcore.utils.nms.NmsVersion;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility class for sending action bar messages to players.
 * Supports both modern Spigot API and legacy NMS packet-based approaches.
 */
public class ActionBar {

    /** No-argument constructor. */
    private ActionBar() {
    }

    private static Class<?> craftPlayerClass;
    private static Class<?> packetClass;
    private static Method getHandleMethod;
    private static Field playerConnectionField;
    private static Constructor<?> constructorPacket;
    private static Constructor<?> constructorComponent;

    static {
        String nmsVersionAsString = Bukkit.getServer().getClass().getPackage().getName();
        nmsVersionAsString = nmsVersionAsString.substring(nmsVersionAsString.lastIndexOf(".") + 1);

        try {
            craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + nmsVersionAsString + ".entity.CraftPlayer");
            Class<?> packetPlayOutChatClass = Class.forName("net.minecraft.server." + nmsVersionAsString + ".PacketPlayOutChat");
            packetClass = Class.forName("net.minecraft.server." + nmsVersionAsString + ".Packet");
            Class<?> iChatBaseComponentClass = Class.forName("net.minecraft.server." + nmsVersionAsString + ".IChatBaseComponent");

            getHandleMethod = craftPlayerClass.getMethod("getHandle");
            playerConnectionField = getHandleMethod.getReturnType().getField("playerConnection");

            Class<?> chatComponentTextClass = Class.forName("net.minecraft.server." + nmsVersionAsString + ".ChatComponentText");

            constructorComponent = chatComponentTextClass.getConstructor(String.class);
            constructorPacket = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, Byte.TYPE);
        } catch (Exception ignored) {
        }
    }

    /**
     * Sends an action bar message to the specified player.
     *
     * @param player  the player to send the action bar message to.
     * @param message the message to display in the action bar.
     */
    public static void sendActionBar(Player player, String message) {

        if (!player.isOnline()) {
            return;
        }

        if (NmsVersion.nmsVersion.getVersion() >= NmsVersion.V_1_10.getVersion()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(TextComponent.fromLegacyText(message)));
            return;
        }

        try {
            Object craftPlayer = craftPlayerClass.cast(player);
            Object packet = constructorComponent.newInstance(message);
            Object packetContent = constructorPacket.newInstance(packet, (byte) 2);
            Object serverPlayer = getHandleMethod.invoke(craftPlayer);
            packet = playerConnectionField.get(serverPlayer);
            Method packetMethod = packet.getClass().getDeclaredMethod("sendPacket", packetClass);
            packetMethod.invoke(packet, packetContent);
        } catch (Exception error) {
            error.printStackTrace();
        }
    }
}