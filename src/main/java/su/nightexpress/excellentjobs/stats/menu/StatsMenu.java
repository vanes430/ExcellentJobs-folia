package su.nightexpress.excellentjobs.stats.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.economybridge.EconomyBridge;
import su.nightexpress.economybridge.api.Currency;
import su.nightexpress.excellentjobs.JobsPlugin;
import su.nightexpress.excellentjobs.config.Config;
import su.nightexpress.excellentjobs.config.Lang;
import su.nightexpress.excellentjobs.user.JobUser;
import su.nightexpress.excellentjobs.job.impl.Job;
import su.nightexpress.excellentjobs.job.impl.JobObjective;
import su.nightexpress.excellentjobs.stats.impl.DayStats;
import su.nightexpress.excellentjobs.stats.impl.JobStats;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.ui.menu.MenuViewer;
import su.nightexpress.nightcore.ui.menu.data.ConfigBased;
import su.nightexpress.nightcore.ui.menu.data.MenuLoader;
import su.nightexpress.nightcore.ui.menu.item.ItemHandler;
import su.nightexpress.nightcore.ui.menu.type.LinkedMenu;
import su.nightexpress.nightcore.util.ItemReplacer;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.bukkit.NightItem;

import java.util.ArrayList;
import java.util.List;

import static su.nightexpress.excellentjobs.Placeholders.*;
import static su.nightexpress.nightcore.util.text.tag.Tags.*;

public class StatsMenu extends LinkedMenu<JobsPlugin, Job> implements ConfigBased {

    public static final String FILE_NAME = "job_stats.yml";

    private static final String CURRENCIES = "%currency%";
    private static final String OBJECTIVES = "%objectives%";

    private List<StatsEntry> entries;
    private String       entryName;
    private List<String> entryLore;
    private String       currencyEntry;
    private String       objectiveEntry;
    private String       nothingEntry;

    public StatsMenu(@NotNull JobsPlugin plugin) {
        super(plugin, MenuType.GENERIC_9X6, BLACK.wrap("Job Stats"));
        this.load(FileConfig.loadOrExtract(plugin, Config.DIR_MENU, FILE_NAME));
    }

    @Override
    @NotNull
    protected String getTitle(@NotNull MenuViewer viewer) {
        return this.getLink(viewer).replacePlaceholders().apply(this.title);
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {
        Player player = viewer.getPlayer();
        Job job = this.getLink(player);
        JobUser user = plugin.getUserManager().getOrFetch(player);
        JobStats stats = user.getStats(job);

        // Border Decoration (Top and Bottom Rows)
        NightItem filler = NightItem.fromType(Material.BLACK_STAINED_GLASS_PANE).setHideTooltip(true);
        for (int i : new int[]{0,1,2,3,4,5,6,7,8,45,46,47,48,50,51,52,53}) {
            this.addItem(viewer, filler.toMenuItem().setSlots(i).setPriority(-1));
        }

        // Stats Entries
        this.entries.forEach(entry -> {
            ItemStack itemStack = entry.getItemStack();
            int minDays = entry.minDays;
            int maxDays = entry.maxDays;

            DayStats dayStats = minDays < 0 && maxDays < 0 ? stats.getAllTimeStats() : stats.getStatsForDays(minDays, maxDays);

            List<String> currencyAmounts = new ArrayList<>();
            List<String> objectiveAmounts = new ArrayList<>();

            for (Currency currency : EconomyBridge.getCurrencies()) {
                double amount = dayStats.getCurrency(currency);
                if (amount == 0D) continue;
                currencyAmounts.add(currency.replacePlaceholders().apply(this.currencyEntry.replace(GENERIC_AMOUNT, currency.format(amount))));
            }
            if (currencyAmounts.isEmpty()) currencyAmounts.add(this.nothingEntry);

            for (JobObjective objective : job.getObjectives()) {
                int amount = dayStats.getObjectives(objective);
                if (amount == 0) continue;
                objectiveAmounts.add(this.objectiveEntry.replace(OBJECTIVE_NAME, objective.getDisplayName()).replace(GENERIC_AMOUNT, NumberUtil.format(amount)));
            }
            if (objectiveAmounts.isEmpty()) objectiveAmounts.add(this.nothingEntry);

            ItemReplacer.create(itemStack).hideFlags().trimmed()
                .setDisplayName(this.entryName)
                .setLore(this.entryLore)
                .replace(GENERIC_NAME, entry.name)
                .replace(CURRENCIES, currencyAmounts)
                .replace(OBJECTIVES, objectiveAmounts)
                .writeMeta();

            this.addItem(viewer, NightItem.fromItemStack(itemStack).toMenuItem().setSlots(entry.slot).setPriority(10));
        });

        // Return Button
        NightItem backItem = NightItem.fromType(Material.IRON_DOOR)
            .setDisplayName(su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.YELLOW.wrap(su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.BOLD.wrap("Return")));
        this.addItem(viewer, backItem.toMenuItem().setSlots(49).setPriority(10).setHandler(new ItemHandler("return", (v, e) -> {
            this.plugin.runTaskAtPlayer(player, () -> plugin.getJobManager().openJobMenu(player, job));
        })));
    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {
    }

    @Override
    public void loadConfiguration(@NotNull FileConfig config, @NotNull MenuLoader loader) {
        this.entries = new ArrayList<>();
        if (!config.contains("Stats.Entries")) {
            this.entries.add(new StatsEntry("All Time", -1, -1, 4, new ItemStack(Material.NETHER_STAR)));
            this.entries.add(new StatsEntry("Today", 0, 0, 19, new ItemStack(Material.CLOCK)));
            this.entries.add(new StatsEntry("Yesterday", 1, 1, 21, new ItemStack(Material.COMPASS)));
            this.entries.add(new StatsEntry("Week", 7, -1, 23, new ItemStack(Material.MAP)));
            this.entries.add(new StatsEntry("Month" , 30, -1, 25, new ItemStack(Material.BOOK)));
        } else {
            config.getSection("Stats.Entries").forEach(sId -> {
                this.entries.add(StatsEntry.read(config, "Stats.Entries." + sId));
            });
        }

        this.entryName = ConfigValue.create("Stats.Entry.Name", LIGHT_CYAN.wrap(BOLD.wrap("Stats: ")) + LIGHT_GRAY.wrap(GENERIC_NAME)).read(config);
        this.entryLore = ConfigValue.create("Stats.Entry.Lore", Lists.newList(" ", GREEN.wrap("Earnings:"), CURRENCIES, "", ORANGE.wrap("Objectives:"), OBJECTIVES)).read(config);
        this.currencyEntry = ConfigValue.create("Stats.Currency.Entry", GREEN.wrap("● ") + LIGHT_GRAY.wrap(CURRENCY_NAME + ": ") + GENERIC_AMOUNT).read(config);
        this.objectiveEntry = ConfigValue.create("Stats.Objective.Entry", ORANGE.wrap("● ") + LIGHT_GRAY.wrap(OBJECTIVE_NAME + ": ") + GENERIC_AMOUNT).read(config);
        this.nothingEntry = ConfigValue.create("Stats.Nothing", LIGHT_GRAY.wrap(RED.wrap("✘") + " No data collected.")).read(config);
    }

    private record StatsEntry(String name, int minDays, int maxDays, int slot, ItemStack itemStack) {
        public static StatsEntry read(@NotNull FileConfig config, @NotNull String path) {
            int minDays = config.getInt(path + ".MinDays", -1);
            int maxDays = config.getInt(path + ".MaxDays", -1);
            int slot = config.getInt(path + ".Slot", -1);
            String name = config.getString(path + ".Name", minDays + " Days");
            Material mat = Material.getMaterial(config.getString(path + ".Material", "PAPER"));
            return new StatsEntry(name, minDays, maxDays, slot, new ItemStack(mat == null ? Material.PAPER : mat));
        }

        public ItemStack getItemStack() {
            return new ItemStack(this.itemStack);
        }
    }
}