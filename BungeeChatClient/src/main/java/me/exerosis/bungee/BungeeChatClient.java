package me.exerosis.bungee;

import me.exerosis.reflection.Reflect;
import me.exerosis.reflection.sockets.SocketListener;
import net.minecraft.server.v1_8_R1.ChatSerializer;
import net.minecraft.server.v1_8_R1.IChatBaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class BungeeChatClient extends JavaPlugin implements Listener {
    private SocketListener socketListener;
    private Map<Player, LightPlayerInjector> playerInjectors = new HashMap<>();
    private Object lastPacket;
    private boolean enabled = false;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        if (getConfig().getBoolean("enabled"))
            setBCEnabled(true, false);
    }

    @Override
    public void onDisable() {
        setBCEnabled(false, false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (enabled)
            injectPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (playerInjectors.containsKey(event.getPlayer()))
            playerInjectors.get(event.getPlayer()).unInject();
    }

    public void setBCEnabled(boolean enabled, boolean saveConfig) {
        this.enabled = enabled;
        if (saveConfig) {
            getConfig().set("enabled", enabled);
            saveConfig();
        }
        if (!enabled) {
            playerInjectors.forEach((p, i) -> i.unInject());
            playerInjectors.clear();
            socketListener.close();
            Bukkit.getScheduler().cancelAllTasks();
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                Socket socket = new Socket(getConfig().getString("hostname"), getConfig().getInt("port"));
                socketListener = new SocketListener(socket, BungeeChatClient.this::sendChatBase) {
                    public void runNewThread(Runnable runnable) {
                        Bukkit.getScheduler().runTaskAsynchronously(BungeeChatClient.this, runnable);
                    }
                };
            } catch (IOException ignored) {
            }
        });
        PlayerUtil.getOnlinePlayers().forEach(this::injectPlayer);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void injectPlayer(Player player) {
        if (!enabled)
            return;
        playerInjectors.put(player, new LightPlayerInjector(player) {
            @Override
            public Object outgoing(Object packet) {
                if (Reflect.Class("{nms}.PacketPlayOutChat").isInstance(packet)) {
                    if (!packet.equals(lastPacket)) {
                        Object text = Reflect.Class(packet).getField(Reflect.Class("{nms}.IChatBaseComponent"), 0).getValue();
                        socketListener.sendObject(convertText(text, String.class));
                    }
                }
                return packet;
            }
        });
    }

    private void sendChatBase(Object chatBase) {
        try {
            lastPacket = Reflect.Class("{nms}.PacketPlayOutChat").getConstructor(1).newInstance(convertText(chatBase, IChatBaseComponent.class));
            playerInjectors.forEach((p, i) -> i.sendPacket(lastPacket));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private Object convertText(Object text, Class<?> type) {
        return !type.equals(String.class) ? ChatSerializer.a((String) text) : ChatSerializer.a((IChatBaseComponent) text);
    }

    public boolean isBCEnabled() {
        return enabled;
    }
}