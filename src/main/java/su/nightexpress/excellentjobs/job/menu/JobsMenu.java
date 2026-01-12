package su.nightexpress.excellentjobs.job.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentjobs.JobsAPI;
import su.nightexpress.excellentjobs.JobsPlugin;
import su.nightexpress.excellentjobs.api.booster.MultiplierType;
import su.nightexpress.excellentjobs.config.Config;
import su.nightexpress.excellentjobs.config.Lang;
import su.nightexpress.excellentjobs.data.impl.JobData;
import su.nightexpress.excellentjobs.data.impl.JobLimitData;
import su.nightexpress.excellentjobs.job.impl.Job;
import su.nightexpress.excellentjobs.job.impl.JobState;
import su.nightexpress.excellentjobs.user.JobUser;
import su.nightexpress.excellentjobs.util.JobUtils;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.ui.menu.MenuViewer;
import su.nightexpress.nightcore.ui.menu.data.ConfigBased;
import su.nightexpress.nightcore.ui.menu.data.MenuLoader;
import su.nightexpress.nightcore.ui.menu.item.ItemHandler;
import su.nightexpress.nightcore.ui.menu.type.NormalMenu;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.bukkit.NightItem;

import java.util.*;

import static su.nightexpress.excellentjobs.Placeholders.*;
import static su.nightexpress.nightcore.util.text.tag.Tags.*;

public class JobsMenu extends NormalMenu<JobsPlugin> implements ConfigBased {

    public static final String FILE_NAME = "job_browse.yml";

    private String       jobNameAvailable;
    private List<String> jobLoreAvailable;
    private String       jobNameLockedPerm;
    private List<String> jobLoreLockedPerm;
    private List<String> jobClickPreviewInfo;
    private List<String> jobClickSettingsInfo;
    private Map<JobState, List<String>> jobStateInfo;

    public JobsMenu(@NotNull JobsPlugin plugin) {
        super(plugin, MenuType.GENERIC_9X3, BLACK.wrap(BOLD.wrap("Jobs")));
        this.load(FileConfig.loadOrExtract(plugin, Config.DIR_MENU, FILE_NAME));
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull InventoryView view) {
        Player player = viewer.getPlayer();
        
        // 1. Manually place the 6 jobs in slot 10, 11, 12, 14, 15, 16
        List<Job> jobs = plugin.getJobManager().getJobs().stream()
                .sorted(Comparator.comparing(Job::getName))
                .limit(6)
                .toList();

        int[] slots = new int[]{10, 11, 12, 14, 15, 16};
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            NightItem item = this.replaceJobItem(player, job);
            this.addItem(viewer, item.toMenuItem().setSlots(slots[i]).setPriority(100).setHandler((v, e) -> {
                if (!job.hasPermission(player)) {
                    Lang.ERROR_NO_PERMISSION.getMessage().send(player);
                    return;
                }
                this.plugin.runTaskAtPlayer(player, () -> this.plugin.getJobManager().openJobMenu(player, job));
            }));
        }

        // 2. Close Button (Iron Door) at Slot 22 (Center Bottom Row)
        NightItem backItem = NightItem.fromType(Material.IRON_DOOR)
            .setDisplayName(su.nightexpress.nightcore.util.text.tag.Tags.RED.wrap(su.nightexpress.nightcore.util.text.tag.Tags.BOLD.wrap("Close")));
        this.addItem(viewer, backItem.toMenuItem().setSlots(22).setPriority(10).setHandler(new ItemHandler("close", (v, event) -> v.getPlayer().closeInventory())));
        
        // 3. NO PAGINATION ARROWS ADDED HERE
    }

    @Override
    protected void onReady(@NotNull MenuViewer menuViewer, @NotNull Inventory inventory) {
    }

    private NightItem replaceJobItem(@NotNull Player player, @NotNull Job job) {
        JobUser user = plugin.getUserManager().getOrFetch(player);
        JobData jobData = user.getData(job);
        JobLimitData limitData = jobData.getLimitDataUpdated();
        JobState state = jobData.getState();

        double xpBoost = JobsAPI.getBoostPercent(player, job, MultiplierType.XP);
        double payBoost = JobsAPI.getBoostPercent(player, job, MultiplierType.INCOME);
        double xpMod = jobData.getXPBonus() * 100D;
        double payMod = jobData.getIncomeBonus() * 100D;

        boolean hasAccess = job.hasPermission(player);
        String name = hasAccess ? this.jobNameAvailable : this.jobNameLockedPerm;
        List<String> lore = new ArrayList<>(hasAccess ? this.jobLoreAvailable : this.jobLoreLockedPerm);
        List<String> clickInfo = new ArrayList<>(jobData.isActive() ? this.jobClickSettingsInfo : this.jobClickPreviewInfo);
        List<String> stateInfo = this.jobStateInfo.getOrDefault(state, Collections.emptyList());

        return job.getIcon()
            .hideAllComponents()
            .setDisplayName(name)
            .setLore(lore)
            .replacement(replacer -> replacer
                .replace(GENERIC_XP_BONUS, JobUtils.formatBonus(xpMod + xpBoost))
                .replace(GENERIC_INCOME_BONUS, JobUtils.formatBonus(payMod + payBoost))
                .replace("%state%", stateInfo)
                .replace("%status%", clickInfo)
                .replace(jobData.replacePlaceholders())
                .replace(job.replacePlaceholders())
            );
    }

    @Override
    public void loadConfiguration(@NotNull FileConfig config, @NotNull MenuLoader loader) {
        this.jobNameAvailable = ConfigValue.create("Job.Available.Name", LIGHT_YELLOW.wrap(BOLD.wrap(JOB_NAME))).read(config);
        this.jobLoreAvailable = ConfigValue.create("Job.Available.Lore", Lists.newList("%state%", " ", JOB_DESCRIPTION, " ", LIGHT_YELLOW.wrap(BOLD.wrap("Your Stats:")), "▪ XP: " + JOB_DATA_XP, "▪ Level: " + JOB_DATA_LEVEL, " ", "%status%")).read(config);
        this.jobNameLockedPerm = ConfigValue.create("Job.Locked_Permission.Name", LIGHT_RED.wrap("[Locked]") + " " + LIGHT_GRAY.wrap(JOB_NAME)).read(config);
        this.jobLoreLockedPerm = ConfigValue.create("Job.Locked_Permission.Lore", Lists.newList(LIGHT_GRAY.wrap("Upgrade your /rank to access this job."))).read(config);
        
        this.jobClickPreviewInfo = ConfigValue.create("Job.ClickInfo.Preview", Lists.newList(LIGHT_GREEN.wrap("[▶] Click to preview."))).read(config);
        this.jobClickSettingsInfo = ConfigValue.create("Job.ClickInfo.Settings", Lists.newList(LIGHT_YELLOW.wrap("[▶] Click to open settings."))).read(config);

        this.jobStateInfo = ConfigValue.forMapByEnum("Job.StateInfo", JobState.class, (cfg, path, id) -> cfg.getStringList(path), map -> {
            map.put(JobState.PRIMARY, Lists.newList(LIGHT_GREEN.wrap("✔ Primary job")));
            map.put(JobState.SECONDARY, Lists.newList(LIGHT_GREEN.wrap("✔ Secondary job")));
            map.put(JobState.INACTIVE, Lists.newList(LIGHT_RED.wrap("✘ Not an employee")));
        }).read(config);
    }
}