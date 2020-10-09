package com.minecraft.ultikits.ultitools;

import com.minecraft.ultikits.api.VersionWrapper;
import com.minecraft.ultikits.beans.CheckResponse;
import com.minecraft.ultikits.checker.Metrics;
import com.minecraft.ultikits.checker.prochecker.ProChecker;
import com.minecraft.ultikits.checker.updatechecker.ConfigFileChecker;
import com.minecraft.ultikits.checker.updatechecker.VersionChecker;
import com.minecraft.ultikits.commands.*;
import com.minecraft.ultikits.config.ConfigController;
import com.minecraft.ultikits.inventoryapi.PageRegister;
import com.minecraft.ultikits.listener.*;
import com.minecraft.ultikits.multiversions.VersionAdaptor;
import com.minecraft.ultikits.register.CommandRegister;
import com.minecraft.ultikits.tasks.*;
import com.minecraft.ultikits.utils.database.DatabaseUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.minecraft.ultikits.listener.LoginListener.*;
import static com.minecraft.ultikits.utils.database.DatabasePlayerTools.getIsLogin;

public final class UltiTools extends JavaPlugin {

    private static UltiTools plugin;
    public static boolean isPAPILoaded;
    private static Economy econ = null;
    private static Boolean isVaultInstalled;
    public static boolean isDatabaseEnabled;
    public static boolean isProVersion;
    private static PageRegister pageRegister;
    public static boolean isGroupManagerEnabled;
    public static VersionWrapper versionAdaptor = new VersionAdaptor().match();

    private boolean setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEcon() {
        return econ;
    }

    public static Boolean getIsVaultInstalled() {
        return isVaultInstalled;
    }

    public static Boolean getIsUltiEconomyInstalled() {
        return UltiTools.getInstance().getServer().getPluginManager().getPlugin("UltiEconomy") != null;
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (isDatabaseEnabled) {
            String table = "userinfo";
            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[UltiTools] 正在初始化数据库...");
            if (DatabaseUtils.createTable(table, new String[]{"username", "password", "whitelisted", "banned"})) {
                getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[UltiTools] 接入数据库成功！");
            } else {
                getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[UltiTools] 接入数据库失败！");
                getConfig().set("enableDataBase", false);
                saveConfig();
                reloadConfig();
            }
        }
    }

    @Override
    public void onEnable() {
        plugin = this;
        startBStates();
        pageRegister = new PageRegister(plugin);
        if (getConfig().getBoolean("enable_pro")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (UltiTools.getInstance().getConfig().getBoolean("enable_pro")) {
                        try {
                            CheckResponse res = ProChecker.run();
                            if (res.code.equals("200")) {
                                UltiTools.isProVersion = true;
                                UltiTools.getInstance().getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "[UltiTools] Pro版验证成功！");
                            } else {
                                UltiTools.getInstance().getServer().getConsoleSender().sendMessage(ChatColor.RED + "[UltiTools] Pro版验证失败, 启用免费版！");
                            }
                            UltiTools.getInstance().getServer().getConsoleSender().sendMessage(ChatColor.RED + "[UltiTools] " + res.msg);
                        } catch (Exception e) {
                            UltiTools.getInstance().getServer().getConsoleSender().sendMessage(ChatColor.RED + "[UltiTools] Pro版验证失败, 启用免费版！");
                        }
                    }
                }
            }.runTaskAsynchronously(plugin);
        }
        File folder = new File(String.valueOf(getDataFolder()));
        List<File> folders = new ArrayList<>();
        folders.add(new File(getDataFolder() + "/playerData"));
        folders.add(new File(getDataFolder() + "/chestData"));
        folders.add(new File(getDataFolder() + "/loginData"));
        folders.add(new File(getDataFolder() + "/emailData"));
        folders.add(new File(getDataFolder() + "/permission"));
        folders.add(new File(getDataFolder() + "/sidebar"));
        folders.add(new File(getDataFolder() + "/kitData"));
        File config_file = new File(getDataFolder(), "config.yml");
        if (!folder.exists() || !config_file.exists()) {
            saveDefaultConfig();
        }

        new BukkitRunnable(){

            @Override
            public void run() {
                makedirs(folders);
                ConfigController.initFiles();
                ConfigFileChecker.reviewMainConfigFile();
            }
        }.runTaskAsynchronously(plugin);


        isVaultInstalled = setupVault();

        isDatabaseEnabled = getConfig().getBoolean("enableDataBase");

        isPAPILoaded = getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;

        isGroupManagerEnabled = getServer().getPluginManager().getPlugin("GroupManager") != null;

        if (!isPAPILoaded) {
            getLogger().warning("[UltiTools] 未找到PAPI前置插件，查找其他可行依赖中...");
            if (!(getIsUltiEconomyInstalled() && isVaultInstalled)) {
                getLogger().warning("[UltiTools] 未找到经济前置插件，关闭中...");
                getLogger().warning("[UltiTools] 至少需要Vault或者UltiEconomy, 或者安装PAPI才能运行");
                getServer().getPluginManager().disablePlugin(this);
            }
            if (getServer().getPluginManager().getPlugin("UltiLevel") == null) {
                getLogger().warning("[UltiTools] 未找到UltiLevel等级插件，关闭计分板等级相关显示！");
            }
        }

        //加载世界
        if (this.getConfig().getBoolean("enable_multiworlds")) {
            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[UltiTools] 正在加载世界中...");
            File worldFile = new File(getDataFolder(), "worlds.yml");
            YamlConfiguration worldConfig = YamlConfiguration.loadConfiguration(worldFile);
            List<String> worlds = worldConfig.getStringList("worlds");
            for (String eachWorld : worlds) {
                getServer().createWorld(new WorldCreator(eachWorld));
            }
            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[UltiTools] 世界加载成功！");
        }

        //Objects.requireNonNull(this.getCommand("ultitools")).setExecutor(new ToolsCommands());
        if (this.getConfig().getBoolean("enable_email")) {
            CommandRegister.registerCommand(plugin, new EmailCommands(), "ultikits.tools.email", "邮件系统", "email");
        }
        if (this.getConfig().getBoolean("enable_home")) {
            CommandRegister.registerCommand(plugin, new HomeCommands(), "ultikits.tools.home", "回到某个家", "home");
            CommandRegister.registerCommand(plugin, new SetHomeCommands(), "ultikits.tools.sethome", "设置家", "sethome");
            CommandRegister.registerCommand(plugin, new DeleteHomeCommands(), "ultikits.tools.delhome", "删除家", "delhome");
            CommandRegister.registerCommand(plugin, new HomeListCommands(), "ultikits.tools.homelist", "查看家列表", "homelist");
            getServer().getPluginManager().registerEvents(new HomeCommands(), this);
        }
        if (this.getConfig().getBoolean("enable_white_list")) {
            CommandRegister.registerCommand(plugin, new WhitelistCommands(), "ultikits.tools.whitelist", "白名单命令", "wl");
            Bukkit.getPluginManager().registerEvents(new WhitelistListener(), this);
        }
        if (this.getConfig().getBoolean("enable_scoreboard")) {
            CommandRegister.registerCommand(plugin, new SbCommands(), "ultikits.tools.scoreboard", "侧边栏开关", "sb");
            new SideBarTask().runTaskTimer(this, 0, 20L);
        }
        if (this.getConfig().getBoolean("enable_lock")) {
            CommandRegister.registerCommand(plugin, new UnlockCommands(), "ultikits.tools.lock", "上锁箱子", "unlock");
            CommandRegister.registerCommand(plugin, new LockCommands(), "ultikits.tools.unlock", "解锁箱子", "lock");
            Bukkit.getPluginManager().registerEvents(new ChestLockListener(), this);
        }
        if (this.getConfig().getBoolean("enable_remote_chest")) {
            CommandRegister.registerCommand(plugin, new RemoteBagCommands(), "ultikits.tools.bag", "远程背包", "bag");
            CommandRegister.registerCommand(plugin, new RemoteBagConsoleCommands(), "ultikits.tools.admin", "远程背包后台命令", "createbag");
        }
        if (this.getConfig().getBoolean("enable_multiworlds")) {
            CommandRegister.registerCommand(plugin, new MultiWorldsCommands(), "ultikits.tools.mw", "多世界系统", "mw");
        }
        if (this.getConfig().getBoolean("enable_kits")) {
            CommandRegister.registerCommand(plugin, new KitsCommands(), "ultikits.tools.kits", "礼包系统", "kits");
        }
        if (this.getConfig().getBoolean("enable_cleaner")) {
            CommandRegister.registerCommand(plugin, new CleanerCommands(), "ultikits.tools.clean", "清理系统", "clean");
        }
        if (this.getConfig().getBoolean("enable_permission")) {
            CommandRegister.registerCommand(plugin, new PermissionCommands(), "ultikits.tools.permission", "权限系统", "pers");
            getServer().getPluginManager().registerEvents(new PermissionAddOnJoinListener(), this);
        }


        //注册监听器
        if (getConfig().getBoolean("enable_onjoin")) {
            Bukkit.getPluginManager().registerEvents(new JoinListener(), this);
        }
        if (getConfig().getBoolean("enable_chat")) {
            getServer().getPluginManager().registerEvents(new ChatListener(), this);
        }
        if (getConfig().getBoolean("enable_login")) {
            getServer().getPluginManager().registerEvents(new LoginListener(), this);
            getServer().getPluginManager().registerEvents(new LoginGUIListener(), this);
            getServer().getPluginManager().registerEvents(new ValidationPageListener(), this);
            checkPlayerAlreadyLogin();
            CommandRegister.registerCommand(plugin, new LoginRegisterCommands(), "ultikits.tools.login", "登陆系统", "reg", "regs", "re");
        }

        //注册任务
        if (this.getConfig().getBoolean("enable_name_prefix")) {
            new NamePrefixSuffixTask().runTaskTimer(this, 0, 20L);
        }
        if (this.getConfig().getBoolean("enable_cleaner")) {
            new CleanerTask().runTaskTimerAsynchronously(this, 10 * 20L, 10 * 20L);
            new UnloadChunksTask().runTaskTimer(this, 0L, 60 * 20L);
        }
        if (getConfig().getBoolean("enable_pro")) {
            new ProCheckerTask().runTaskTimerAsynchronously(this, 12000L, 12000L);
        }

        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[UltiTools] 基础插件已加载！");
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[UltiTools] 作者：wisdomme");

        //检查更新
        if (getConfig().getBoolean("enable_version_check")) {
            VersionChecker.runTask();
        }
    }

    @Override
    public void onDisable() {
        for (String player : playerLoginStatus.keySet()) {
            if (Bukkit.getPlayerExact(player) != null) {
                Player player1 = Bukkit.getPlayerExact(player);
                assert player1 != null;
                if (!getIsLogin(player1)) {
                    player1.kickPlayer(ChatColor.AQUA + "[UltiTools Login] 腐竹重载/关闭了插件，请重新登录！");
                }
            }
        }
        savePlayerLoginStatus();
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[UltiTools] 基础插件已卸载！");
    }

    public static UltiTools getInstance() {
        return plugin;
    }

    private void makedirs(List<File> folders) {
        for (File eachFolder : folders) {
            if (!eachFolder.exists()) {
                eachFolder.mkdirs();
            }
        }
    }

    public static PageRegister getPageRegister() {
        return pageRegister;
    }

    private static void startBStates() {
        // All you have to do is adding the following two lines in your onEnable method.
        // You can find the plugin ids of your plugins on the page https://bstats.org/what-is-my-plugin-id
        int pluginId = 8652; // <-- Replace with the id of your plugin!
        Metrics metrics = new Metrics(UltiTools.getInstance(), pluginId);
    }
}