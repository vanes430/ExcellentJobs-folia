package su.nightexpress.excellentjobs.job.menu;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentjobs.JobsPlugin;
import su.nightexpress.nightcore.ui.menu.MenuViewer;
import su.nightexpress.nightcore.ui.menu.item.ItemClick;
import su.nightexpress.nightcore.ui.menu.type.LinkedMenu;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.bukkit.NightItem;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class JobConfirmMenu extends LinkedMenu<JobsPlugin, JobConfirmMenu.ConfirmData> {

    public record ConfirmData(
        @NotNull String title, 
        @NotNull NightItem icon, 
        @NotNull ItemClick onAccept, 
        @NotNull ItemClick onCancel
    ) {}

    public JobConfirmMenu(@NotNull JobsPlugin plugin) {
        super(plugin, MenuType.GENERIC_9X3, BLACK.wrap("Confirmation"));
    }

    @Override
    @NotNull
    protected String getTitle(@NotNull MenuViewer viewer) {
        return this.getLink(viewer).title();
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {
        ConfirmData data = this.getLink(viewer);

        // Central Info Icon
        this.addItem(viewer, data.icon().toMenuItem().setSlots(13).setPriority(10));

        // Confirm Button (Green Terracotta) - Center-Left
        NightItem confirm = NightItem.fromType(Material.LIME_TERRACOTTA)
            .setDisplayName(GREEN.wrap(BOLD.wrap("✔ CONFIRM")))
            .setLore(Lists.newList("", GRAY.wrap("Click to proceed with this action.")));
        this.addItem(viewer, confirm.toMenuItem().setSlots(11).setPriority(10).setHandler(data.onAccept()));

        // Cancel Button (Red Terracotta) - Center-Right
        NightItem cancel = NightItem.fromType(Material.RED_TERRACOTTA)
            .setDisplayName(RED.wrap(BOLD.wrap("✘ CANCEL")))
            .setLore(Lists.newList("", GRAY.wrap("Click to cancel and go back.")));
        this.addItem(viewer, cancel.toMenuItem().setSlots(15).setPriority(10).setHandler(data.onCancel()));
    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {
        // Handled in onPrepare
    }
}
