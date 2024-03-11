package com.mrkelpy.bountyseekers.commons.gui;

import com.mrkelpy.bountyseekers.commons.carriers.Benefactor;
import com.mrkelpy.bountyseekers.commons.carriers.Bounty;
import com.mrkelpy.bountyseekers.commons.carriers.SimplePlayer;
import com.mrkelpy.bountyseekers.commons.configuration.InternalConfigs;
import com.mrkelpy.bountyseekers.commons.configuration.ConfigurableTextHandler;
import com.mrkelpy.bountyseekers.commons.configuration.PluginConfiguration;
import com.mrkelpy.bountyseekers.commons.enums.CompatibilityMode;
import com.mrkelpy.bountyseekers.commons.utils.ChatUtils;
import com.mrkelpy.bountyseekers.commons.utils.FileUtils;
import com.mrkelpy.bountyseekers.commons.utils.ItemStackUtils;
import com.mrkelpy.bountyseekers.commons.utils.PluginConstants;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

/**
 * This class creates a GUI capable of raising a player's bounty intuitively.
 */
public class BountyRaiseGUI extends ConfirmationGUI {

    private final Bounty bounty;
    private final Benefactor benefactor;
    private final CompatibilityMode compatibility;

    /**
     * Main constructor for the RewardFilterGUI class.
     */
    public BountyRaiseGUI(SimplePlayer target, Benefactor benefactor, CompatibilityMode compatibility) {
        super(ConfigurableTextHandler.INSTANCE.getValueFormatted("bounty.raise.title", benefactor.getPlayer().getName(), target.getName()), 27, benefactor.getPlayer().getUniqueId());
        this.bounty = new Bounty(target.getUniqueId(), compatibility);
        this.benefactor = benefactor;
        this.compatibility = compatibility;
    }

    /**
     * Open the GUI for the player.
     */
    public void openInventory() {
        this.benefactor.getPlayer().openInventory(this.inventory);
    }

    /**
     * Adds all the rewards currently inside the GUI as rewards inside the bounty.
     *
     * @param player The player that pressed the confirm button.
     */
    @Override
    public void onConfirm(Player player) {

        // Cancels the raising if the player didn't contribute to the bounty
        if (Arrays.stream(this.inventory.getContents()).filter(Objects::nonNull).count() == 2) {
            this.onCancel(player);
            return;
        }

        int rewardLimit = InternalConfigs.INSTANCE.getConfig().getInt("reward-limit");
        ItemStack[] rewardFilter = FileUtils.getRewardFilter(this.compatibility);

        // Adds all the rewards inside the GUI to the bounty, and adds the benefactor.
        for (int i = 0; this.storageSlots > i; i++) {

            // Prevents the player from adding in items that aren't in the filter.
            if (rewardFilter != null && this.inventory.getItem(i) != null && !Arrays.asList(rewardFilter).contains(ItemStackUtils.makePivot(this.inventory.getItem(i)))) {
                continue;
            }

            // Prevents the player from raising the target's bounty over the maximum amount.
            if (this.inventory.getItem(i) != null && this.bounty.getRewards().size() >= rewardLimit && rewardLimit != -1) {

                // Check if the item will be compressed, and if so, if the bounty rewards post-compression will overflow the limit.
                if (!ItemStackUtils.willCompress(this.inventory.getItem(i), this.bounty.getRewards())) continue;

                ArrayList<ItemStack> compressedOverflow = new ArrayList<>(this.bounty.getRewards());
                compressedOverflow.add(this.inventory.getItem(i));

                // If the bounty size doesn't overflow the reward limit, add it.
                if (ItemStackUtils.compress(compressedOverflow).size() <= rewardLimit) {
                    this.bounty.addReward(this.inventory.getItem(i));
                    this.inventory.setItem(i, null);
                }

                continue;
            }

            this.bounty.addReward(this.inventory.getItem(i));
            this.inventory.setItem(i, null);
        }

        // Sends the "items returned" warning message in case there are still items left inside the GUI to be returned to the player.
        if (Arrays.stream(this.inventory.getContents()).filter(Objects::nonNull).count() > 2)
            player.sendMessage(ChatUtils.sendMessage(null, ConfigurableTextHandler.INSTANCE.getValueFormatted("bounty.raise.denied", null, null)));

        // Returns any leftover items to the player.
        for (int i = 0; this.storageSlots + 1 > i; i++) {

            if (this.inventory.getItem(i) == null) continue;
            this.benefactor.getPlayer().getInventory().addItem(this.inventory.getItem(i));
            this.inventory.setItem(i, null);
        }
        this.bounty.save();

        // Announces the bounty raise, in case it was raised, hiding the benefactor if they're anonymous.
        if (this.benefactor.toString() != null && this.bounty.getAdditionCount() > 0)
            Bukkit.broadcastMessage(ChatUtils.sendMessage(null, ConfigurableTextHandler.INSTANCE.getValueFormatted("bounty.raise.loud", this.benefactor.getPlayer().getName(), this.bounty.getTarget())));

        else if (this.benefactor.toString() == null && this.bounty.getAdditionCount() > 0 && !PluginConfiguration.INSTANCE.getConfig().getBoolean("general.commands.truly-silent-raise"))
            Bukkit.broadcastMessage(ChatUtils.sendMessage(null,  ConfigurableTextHandler.INSTANCE.getValueFormatted("bounty.raise.silent", null, this.bounty.getTarget())));

        // Unregisters the event handlers and closes the inventory so no items are returned.
        HandlerList.unregisterAll(this);
        this.benefactor.getPlayer().closeInventory();
    }

    /**
     * Returns the benefactor's inventory as of before the GUI was opened.
     *
     * @param player The player that pressed the cancel button.
     */
    @Override
    public void onCancel(Player player) {
        // Unregisters the event handlers so there's no recursion
        HandlerList.unregisterAll(this);

        // Iterate through the GUI inventory items
        for (int i = 0; i < this.inventory.getSize(); i++) {
            ItemStack item = this.inventory.getItem(i);

            // Skip null or air items
            if (item == null || item.getType() == Material.AIR) continue;

            // Check if the item is not wool (specifically RED_WOOL or LIME_WOOL used for GUI)
            if (item.getType() != Material.LIME_WOOL && item.getType() != Material.RED_WOOL) {
                // Logic to return non-wool items to the player's inventory
                // This example simply tries to add the item back to the player's inventory directly

                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                // Handle any overflow (e.g., items that couldn't fit in the player's inventory)
                for (ItemStack overflowItem : overflow.values()) {
                    player.getWorld().dropItem(player.getLocation(), overflowItem);
                }

                // Remove the item from the GUI inventory after processing
                this.inventory.setItem(i, new ItemStack(Material.AIR));
            }
        }

        // Closing the GUI inventory after processing items
        if (this.benefactor.getPlayer().getOpenInventory().getType() == InventoryType.CHEST) {
            this.benefactor.getPlayer().closeInventory();
        }

        // Update the player's inventory to reflect any changes
        player.updateInventory();
    }


    /**
     * Count closing the inventory as a cancel.
     *
     * @param event InventoryCloseEvent
     */
    @Override
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        if (event.getInventory().equals(this.inventory) && this.userUUID.equals(event.getPlayer().getUniqueId()))
            ItemStackUtils.scheduleRunnable(() -> this.onCancel((Player) event.getPlayer()), 1L);

        super.onInventoryClose(event);
    }

    /**
     * While this GUI is opened, the benefactor can't pick up any items, to prevent losses.
     *
     * @param event The event that is being handled.
     */
    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {

        if (event.getPlayer() == this.benefactor.getPlayer())
            event.setCancelled(true);
    }

    /**
     * While this GUI is opened, the benefactor can't drop any items, to prevent dupes.
     * @param event PlayerDropItemEvent
     */
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {

        if (event.getPlayer().getUniqueId() == this.benefactor.getPlayer().getUniqueId()) {

            event.setCancelled(true);
            ItemStackUtils.scheduleRunnable(() -> event.getPlayer().updateInventory(), 1L);
        }
    }
}

