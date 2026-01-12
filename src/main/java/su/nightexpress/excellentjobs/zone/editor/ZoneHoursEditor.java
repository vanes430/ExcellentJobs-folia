package su.nightexpress.excellentjobs.zone.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentjobs.JobsPlugin;
import su.nightexpress.excellentjobs.config.Lang;
import su.nightexpress.excellentjobs.util.Hours;
import su.nightexpress.excellentjobs.zone.ZoneManager;
import su.nightexpress.excellentjobs.zone.impl.Zone;
import su.nightexpress.nightcore.ui.dialog.Dialog;
import su.nightexpress.nightcore.ui.menu.MenuViewer;
import su.nightexpress.nightcore.ui.menu.data.Filled;
import su.nightexpress.nightcore.ui.menu.data.MenuFiller;
import su.nightexpress.nightcore.ui.menu.item.MenuItem;
import su.nightexpress.nightcore.ui.menu.type.LinkedMenu;
import su.nightexpress.nightcore.util.StringUtil;
import su.nightexpress.nightcore.util.bukkit.NightItem;

import java.time.DayOfWeek;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static su.nightexpress.excellentjobs.Placeholders.*;

@SuppressWarnings("UnstableApiUsage")
public class ZoneHoursEditor extends LinkedMenu<JobsPlugin, Zone> implements Filled<DayOfWeek> {

    private final ZoneManager manager;

    public ZoneHoursEditor(@NotNull JobsPlugin plugin, @NotNull ZoneManager manager) {
        super(plugin, MenuType.GENERIC_9X4, Lang.EDITOR_TITLE_ZONE_HOURS.getString());
        this.manager = manager;

        this.addItem(MenuItem.buildReturn(this, 31, (viewer, event) -> {
            this.runNextTick(() -> this.manager.openEditor(viewer.getPlayer(), this.getLink(viewer)));
        }));
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {
        this.autoFill(viewer);
    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {

    }

    @Override
    @NotNull
    public MenuFiller<DayOfWeek> createFiller(@NotNull MenuViewer viewer) {
        var autoFill = MenuFiller.builder(this);

        Player player = viewer.getPlayer();
        Zone zone = this.getLink(player);

        autoFill.setSlots(IntStream.range(10, 18).toArray());
        autoFill.setItems(Stream.of(DayOfWeek.values()).toList());
        autoFill.setItemCreator(day -> {
            Hours hours = zone.getHours(day);

            return NightItem.fromType(Material.CLOCK)
                .setEnchantGlint(hours != null)
                .setHideComponents(true)
                .localized(Lang.EDITOR_ZONE_TIME_OBJECT)
                .replacement(replacer -> replacer
                    .replace(GENERIC_NAME, StringUtil.capitalizeUnderscored(day.name()))
                    .replace(GENERIC_VALUE, Lang.goodEntry(hours == null ? Lang.OTHER_NONE.getString() : hours.format()))
                );
        });
        autoFill.setItemClick(day -> (viewer1, event) -> {
            if (event.isRightClick()) {
                zone.getHoursByDayMap().remove(day);
                zone.save();
                this.runNextTick(() -> this.flush(viewer1));
                return;
            }

            if (event.isLeftClick()) {
                this.handleInput(Dialog.builder(viewer1, Lang.EDITOR_GENERIC_ENTER_TIMES, input -> {
                    Hours hours = Hours.parse(input.getTextRaw());
                    if (hours != null) {
                        zone.getHoursByDayMap().put(day, hours);
                        zone.save();
                    }
                    return true;
                }));
            }
        });

        return autoFill.build();
    }
}
