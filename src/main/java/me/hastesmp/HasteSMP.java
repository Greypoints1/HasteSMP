package me.hastesmp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class HasteSMP extends JavaPlugin implements Listener {

    private static final double MIN_SPEED = 3.5;
    private static final double MAX_SPEED = 4.5;
    private static final double DEFAULT_SPEED = 4.0;
    private static final double SPEED_STEP = 0.1;

    private NamespacedKey speedKey;
    private NamespacedKey rewardKey;

    @Override
    public void onEnable() {
        speedKey = new NamespacedKey(this, "attack_speed");
        rewardKey = new NamespacedKey(this, "attack_speed_reward");

        Bukkit.getPluginManager().registerEvents(this, this);

        // Apply saved values to anyone online during a plugin reload.
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyAttackSpeed(player);
        }

        getLogger().info("HasteSMP enabled.");
    }

    private double getSpeed(Player player) {
        return player.getPersistentDataContainer().getOrDefault(
                speedKey, PersistentDataType.DOUBLE, DEFAULT_SPEED
        );
    }

    private void setSpeed(Player player, double speed) {
        double rounded = Math.round(speed * 10.0) / 10.0;
        double clamped = Math.max(MIN_SPEED, Math.min(MAX_SPEED, rounded));

        player.getPersistentDataContainer().set(
                speedKey, PersistentDataType.DOUBLE, clamped
        );

        applyAttackSpeed(player);
    }

    private void applyAttackSpeed(Player player) {
        double speed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, getSpeed(player)));

        // Keep stored data valid if it was edited or came from an older version.
        player.getPersistentDataContainer().set(
                speedKey, PersistentDataType.DOUBLE, speed
        );

        var attribute = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attribute != null) {
            attribute.setBaseValue(speed);
        }
    }

    private ItemStack createReward() {
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = star.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Attack Speed Increase");
        meta.setLore(List.of(
                ChatColor.GRAY + "Right-click to gain",
                ChatColor.GREEN + "+0.1 Attack Speed"
        ));

        meta.getPersistentDataContainer().set(
                rewardKey, PersistentDataType.BYTE, (byte) 1
        );

        star.setItemMeta(meta);
        return star;
    }

    private boolean isReward(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) {
            return false;
        }

        Byte marker = item.getItemMeta().getPersistentDataContainer().get(
                rewardKey, PersistentDataType.BYTE
        );
        return marker != null && marker == (byte) 1;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        double victimSpeed = getSpeed(victim);
        if (victimSpeed > MIN_SPEED) {
            setSpeed(victim, victimSpeed - SPEED_STEP);
            victim.sendMessage(ChatColor.RED + "You died and lost 0.1 attack speed!");
        } else {
            setSpeed(victim, MIN_SPEED);
            victim.sendMessage(ChatColor.RED + "You're already at minimum attack speed!");
        }

        // Only player kills against victims above minimum speed create a reward.
        // Players already at 3.5 attack speed do not drop a Nether Star.
        if (killer != null && !killer.equals(victim) && victimSpeed > MIN_SPEED) {
            event.getDrops().add(createReward());
            killer.sendMessage(ChatColor.GOLD
                    + "A Nether Star has dropped! Collect it to gain attack speed.");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        ItemStack item = event.getItem();
        if (!isReward(item)) return;

        // Cancel the normal Nether Star interaction, including at the speed cap.
        event.setCancelled(true);

        Player player = event.getPlayer();
        double speed = getSpeed(player);

        if (speed >= MAX_SPEED) {
            player.sendMessage(ChatColor.RED + "You're already at maximum attack speed!");
            return;
        }

        setSpeed(player, speed + SPEED_STEP);
        player.sendMessage(ChatColor.GREEN + "You gained +0.1 attack speed!");

        // Consume exactly one tagged reward.
        item.setAmount(item.getAmount() - 1);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        applyAttackSpeed(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                applyAttackSpeed(player);
            }
        }, 1L);
    }
}
