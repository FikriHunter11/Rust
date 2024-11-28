package org.fikrihunter11.rustdemo;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class RustDemo extends JavaPlugin implements Listener {

    private final HashMap<UUID, StructureType> currentStructure = new HashMap<>();
    private final HashMap<UUID, Boolean> buildingMode = new HashMap<>();
    private final HashMap<UUID, BukkitRunnable> previewTasks = new HashMap<>();


    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("RustDemo enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("RustDemo disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("building")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                giveBuildingPlan(player);
                return true;
            } else {
                sender.sendMessage("This command can only be used by players!");
                return true;
            }
        }
        return false;
    }

    private void giveBuildingPlan(Player player) {
        ItemStack buildingPlan = new ItemStack(Material.PAPER);
        ItemMeta meta = buildingPlan.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Building Plan");
            buildingPlan.setItemMeta(meta);
        }
        player.getInventory().addItem(buildingPlan);
        player.sendMessage("§aYou received a Building Plan!");
    }

    // Handle right-clicking to open the GUI (only right-click with Building Plan)
    @EventHandler
    public void onPlayerRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.PAPER && item.getItemMeta() != null && "§6Building Plan".equals(item.getItemMeta().getDisplayName()) && event.getAction().toString().contains("RIGHT_CLICK")) {
            event.setCancelled(true);  // Prevent any other action
            openBuildingMenu(player);
        }
    }

    // Handle left-clicking to place the structure (only left-click with Building Plan)
    @EventHandler
    public void onPlayerLeftClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Check if the player has the Building Plan paper
        if (item.getType() == Material.PAPER && item.getItemMeta() != null && "§6Building Plan".equals(item.getItemMeta().getDisplayName()) && event.getAction().toString().contains("LEFT_CLICK")) {

            UUID playerId = player.getUniqueId();
            if (!buildingMode.getOrDefault(playerId, false)) return;

            StructureType structureType = currentStructure.get(playerId);
            if (structureType == null) {
                player.sendMessage("§cNo structure selected!");
                return;
            }

            Block targetBlock = player.getTargetBlockExact(10);
            if (targetBlock == null) return;

            Location baseLocation = targetBlock.getLocation().add(0, 1, 0);
            boolean canPlace = isPlacementValid(baseLocation, structureType);

            if (canPlace) {
                // Permanently place the structure blocks
                placeStructure(baseLocation, structureType);
                player.sendMessage("§aStructure placed!");
            } else {
                player.sendMessage("§cCannot place structure here!");
            }

            event.setCancelled(true); // Prevent default interaction behavior
        }
    }

    private void openBuildingMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, "Building Menu");

        gui.setItem(0, createGuiItem(Material.STONE, "§aFoundation"));
        gui.setItem(1, createGuiItem(Material.BRICKS, "§aWall"));
        gui.setItem(2, createGuiItem(Material.OAK_PLANKS, "§aFloor"));
        gui.setItem(3, createGuiItem(Material.LADDER, "§aStairs"));
        gui.setItem(4, createGuiItem(Material.GLASS, "§aWindow"));

        player.openInventory(gui);
    }

    private ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Building Menu")) {
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getItemMeta() == null) return;

            String name = clicked.getItemMeta().getDisplayName();
            switch (name) {
                case "§aFoundation":
                    player.sendMessage("§aSelected: Foundation");
                    currentStructure.put(player.getUniqueId(), StructureType.FOUNDATION);
                    break;
                case "§aWall":
                    player.sendMessage("§aSelected: Wall");
                    currentStructure.put(player.getUniqueId(), StructureType.WALL);
                    break;
                case "§aFloor":
                    player.sendMessage("§aSelected: Floor");
                    currentStructure.put(player.getUniqueId(), StructureType.FLOOR);
                    break;
                case "§aStairs":
                    player.sendMessage("§aSelected: Stairs");
                    currentStructure.put(player.getUniqueId(), StructureType.STAIRS);
                    break;
                case "§aWindow":
                    player.sendMessage("§aSelected: Window");
                    currentStructure.put(player.getUniqueId(), StructureType.WINDOW);
                    break;
            }
            player.closeInventory();
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (buildingMode.getOrDefault(playerId, false)) {
            buildingMode.put(playerId, false);
            player.sendMessage("§cBuilding mode disabled.");

            // Stop the preview task
            if (previewTasks.containsKey(playerId)) {
                previewTasks.get(playerId).cancel();
                previewTasks.remove(playerId);
            }
        } else {
            buildingMode.put(playerId, true);
            player.sendMessage("§aBuilding mode enabled. Right-click to preview, left-click to place.");
        }
        event.setCancelled(true);
    }

    private boolean isPlacementValid(Location baseLocation, StructureType structureType) {
        int width = structureType.getWidth();
        int height = structureType.getHeight();
        int depth = structureType.getDepth();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    Location loc = baseLocation.clone().add(x, y, z);
                    if (!loc.getBlock().isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void showPreview(Player player, Location baseLocation, StructureType structureType, boolean canPlace) {
        Material previewMaterial = canPlace ? Material.LIME_STAINED_GLASS : Material.RED_STAINED_GLASS;

        for (int x = 0; x < structureType.getWidth(); x++) {
            for (int y = 0; y < structureType.getHeight(); y++) {
                for (int z = 0; z < structureType.getDepth(); z++) {
                    Location loc = baseLocation.clone().add(x, y, z);
                    // Use player.sendBlockChange to display the preview
                    player.sendBlockChange(loc, previewMaterial.createBlockData());
                }
            }
        }
    }

    // Place the structure blocks permanently
    private void placeStructure(Location baseLocation, StructureType structureType) {
        for (int x = 0; x < structureType.getWidth(); x++) {
            for (int y = 0; y < structureType.getHeight(); y++) {
                for (int z = 0; z < structureType.getDepth(); z++) {
                    Location loc = baseLocation.clone().add(x, y, z);
                    loc.getBlock().setType(structureType.getMaterial());
                }
            }
        }
    }
}
