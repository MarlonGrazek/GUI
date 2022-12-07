package com.marlongrazek.gui;

import com.marlongrazek.builder.ItemStackBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.Consumer;

public class GUI {

    private final Plugin plugin;
    private final Player player;
    private final Events events = new Events();

    private final List<Page> history = new ArrayList<>();

    public GUI(Plugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open(Page page) {

        // HISTORY
        if (!history.isEmpty()) {
            Page p = history.get(history.size() - 1);
            if (p.getCloseAction() != null) p.getCloseAction().accept(player);
            HandlerList.unregisterAll(events);
            player.closeInventory();
        }
        else history.add(null);
        if(history.get(history.size() - 1) != null && !history.get(history.size() - 1).equals(page)) history.add(page);

        // OPEN PAGE
        if (page.getOpenAction() != null) page.getOpenAction().accept(player);
        Bukkit.getPluginManager().registerEvents(events, plugin);
        player.openInventory(page.inventory);
    }

    public void openPageFromHistory(int index) {

        GUI.Page page = history.get(index);

        // PAGE IS NULL
        if (page == null) {
            player.closeInventory();
            return;
        }

        for (int i = 0; i < index; i++) history.remove(history.size() - 1);
        open(page);
    }

    public void close() {

        Page page = history.get(history.size() - 1);

        // CLOSE PAGE
        if (page.getCloseAction() != null) page.getCloseAction().accept(player);
        HandlerList.unregisterAll(events);
        player.closeInventory();

        history.clear();
    }

    public class Events implements Listener {

        @EventHandler
        public void onClose(InventoryCloseEvent e) {

            Player player = (Player) e.getPlayer();
            Page page = history.get(history.size() - 1);

            // PREVENT CLOSE
            if (page.preventClose) {
                player.openInventory(page.getInventory());
                return;
            }

            if (page.getCloseAction() != null) page.getCloseAction().accept(player);
            HandlerList.unregisterAll(events);
        }

        @EventHandler
        public void onClick(InventoryClickEvent e) {

            Page page = history.get(history.size() - 1);

            if (e.getInventory() != page.getInventory()) return;
            if (e.getView().getTopInventory() != e.getClickedInventory()) return;

            for(Item item : page.getItems().values()) {
                if(item == null || e.getCurrentItem() == null) continue;
                if(!e.getCurrentItem().equals(item.toItemStack())) continue;
                e.setCancelled(true);
                if(item.getClickAction() != null) item.getClickAction().accept(e.getClick());
                break;
            }
        }
    }

    public static class Page {

        private String title;
        private int size;
        private final Map<Integer, Item> items = new HashMap<>();
        private boolean preventClose = false;
        private Consumer<Player> openAction;
        private Consumer<Player> closeAction;

        private Inventory inventory;

        public Page() {
            createInventory();
        }

        public Page(String title, int size) {
            this.title = title;
            this.size = size;
            createInventory();
        }

        private void createInventory() {
            inventory = Bukkit.createInventory(null, this.size, this.title);
        }

        public void update() {
            inventory.clear();
            items.keySet().forEach(slot -> inventory.setItem(slot, items.get(slot).toItemStack()));
        }

        public Inventory getInventory() {
            return inventory;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public void setItem(Item item, int slot) {
            this.items.put(slot, item);
            update();
        }

        public void addItem(Item item) {
            for (int i = 0; i < this.items.size(); i++) {
                if (!this.items.containsKey(i)) {
                    items.put(i, item);
                    break;
                }
            }
            update();
        }

        public void clear() {
            this.items.clear();
        }

        public String getTitle() {
            return this.title;
        }

        public int getSize() {
            return this.size;
        }

        public Map<Integer, Item> getItems() {
            return this.items;
        }

        public Item getItem(int slot) {
            return this.items.get(slot);
        }

        public void preventClose(boolean preventClose) {
            this.preventClose = preventClose;
        }

        public boolean preventClose() {
            return this.preventClose;
        }

        public void onOpen(Consumer<Player> openAction) {
            this.openAction = openAction;
        }

        public void onClose(Consumer<Player> closeAction) {
            this.closeAction = closeAction;
        }

        public Consumer<Player> getOpenAction() {
            return this.openAction;
        }

        public Consumer<Player> getCloseAction() {
            return this.closeAction;
        }
    }

    public static class Item {

        private int amount = 1;
        private String name;
        private Material material;
        private Map<Enchantment, Integer> enchantments = new HashMap<>();
        private List<ItemFlag> itemFlags = new ArrayList<>();
        private List<String> lore = new ArrayList<>();
        private ItemMeta meta;
        private Consumer<ClickType> clickAction;

        public Item() {
        }

        public Item(String name) {
            this.name = name;
        }

        public Item(Material material) {
            this.material = material;
        }

        public Item(String name, Material material) {
            this.name = name;
            this.material = material;
        }

        public void addEnchantment(Enchantment enchantment, Integer level) {
            enchantments.put(enchantment, level);
        }

        public void addGlow() {
            addEnchantment(Enchantment.ARROW_DAMAGE, 1);
            addItemFlag(ItemFlag.HIDE_ENCHANTS);
        }

        public void addItemFlag(ItemFlag itemFlag) {
            itemFlags.add(itemFlag);
        }

        public void addLoreLines(String... lines) {
            Collections.addAll(lore, lines);
        }

        public void clearLore() {
            this.lore.clear();
        }

        public static Item fromItemStack(ItemStack itemStack) {
            ItemMeta itemMeta;
            Material material;
            Item item = new Item();

            if (itemStack.getItemMeta() != null) {
                itemMeta = itemStack.getItemMeta();
                material = itemStack.getType();
                item.setItemMeta(itemMeta);
                item.setMaterial(material);
            }
            return item;
        }

        public int getAmount() {
            return amount;
        }

        public Consumer<ClickType> getClickAction() {
            return this.clickAction;
        }

        public Map<Enchantment, Integer> getEnchantments() {
            return enchantments;
        }

        public List<ItemFlag> getItemFlags() {
            return itemFlags;
        }

        public ItemMeta getItemMeta() {
            return this.meta;
        }

        public List<String> getLore() {
            return this.lore;
        }

        public Material getMaterial() {
            return material;
        }

        public String getName() {
            return name;
        }

        public void onClick(Consumer<ClickType> clickAction) {
            this.clickAction = clickAction;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public void setEnchantments(HashMap<Enchantment, Integer> enchantments) {
            this.enchantments = enchantments;
        }

        public void setItemFlags(List<ItemFlag> itemFlags) {
            this.itemFlags = itemFlags;
        }

        public void setItemMeta(ItemMeta meta) {
            this.meta = meta;
        }

        public void setLore(List<String> lore) {
            this.lore = lore;
        }

        public void setLore(String... lore) {
            this.lore = Arrays.asList(lore);
        }

        public void setLoreLine(String line, int index) {
            this.lore.set(index, line);
        }

        public void setMaterial(Material material) {
            this.material = material;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ItemStack toItemStack() {

            ItemStackBuilder itemStack = new ItemStackBuilder(this.material);

            if (meta != null) itemStack.setItemMeta(this.meta);
            itemStack.setName(this.name);
            itemStack.setLore(new ArrayList<>(this.lore));
            itemStack.setItemFlags(new ArrayList<>(itemFlags));
            itemStack.setEnchantments(new HashMap<>(enchantments));
            itemStack.setAmount(amount);
            return itemStack.toItemStack();
        }
    }

    public static class Section {

    }
}
