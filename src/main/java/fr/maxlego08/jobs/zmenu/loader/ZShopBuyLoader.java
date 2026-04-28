package fr.maxlego08.jobs.zmenu.loader;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.zmenu.buttons.ZShopBuyButton;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;

public class ZShopBuyLoader extends ButtonLoader {

    private final JobsPlugin plugin;

    public ZShopBuyLoader(JobsPlugin plugin) {
        super(plugin, "ZSHOP_BUY");
        this.plugin = plugin;
    }

    @Override
    public Button load(YamlConfiguration configuration, String path, DefaultButtonValue defaultButtonValue) {
        String material = configuration.getString(path + "material");
        String inventoryName = configuration.getString(path + "inventory", "shop");

        if (material == null) {
            this.plugin.getLogger().severe("Missing 'material' for ZSHOP_BUY button at " + path);
        }

        return new ZShopBuyButton(this.plugin, material, inventoryName);
    }
}
