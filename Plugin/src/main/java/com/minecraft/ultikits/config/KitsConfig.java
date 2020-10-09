package com.minecraft.ultikits.config;

import com.minecraft.ultikits.enums.ConfigsEnum;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KitsConfig extends AbstractConfig{

    private static final KitsConfig kitsConfig = new KitsConfig("kits", ConfigsEnum.KIT.toString());

    public KitsConfig() {
        kitsConfig.init();
    }

    private KitsConfig(String name, String filePath) {
        super(name, filePath);
    }

    @Override
    public void load() {
        reload();
        ConfigController.registerConfig(name, kitsConfig);
    }

    @Override
    void doInit(YamlConfiguration config) {
        config.set("xinshou.item", "OAK_PLANKS");
        config.set("xinshou.reBuyable", false);
        config.set("xinshou.name", "新手礼包");
        config.set("xinshou.level", 1);
        config.set("xinshou.job", "全部");
        config.set("xinshou.description", "虽然是木头的，但是却很实用");
        config.set("xinshou.price", 0);
        List<String> list = Arrays.asList("WOODEN_PICKAXE", "WOODEN_AXE", "WOODEN_SHOVEL", "WOODEN_SWORD", "WOODEN_HOE");
        for (String item : list){
            config.set("xinshou.contain."+item+".quantity", 1);
        }
        List<String> playerCommands = new ArrayList<>();
        config.set("xinshou.playerCommands",playerCommands);
        List<String> console = Arrays.asList("say {PLAYER} 领取了新手礼包", "givemoney {PLAYER} 100");
        config.set("xinshou.consoleCommands", console);
    }

}