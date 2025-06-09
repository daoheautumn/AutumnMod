package com.daohe.autumnmod.coremod;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.TransformerExclusions({"com.daohe.autumnmod.coremod"})
public class UhcCraftHelperCoreMod implements IFMLLoadingPlugin {

    public UhcCraftHelperCoreMod() {
        System.out.println("=================================");
        System.out.println("[UhcCraftHelper] CoreMod constructor called!");
        System.out.println("=================================");
    }

    @Override
    public String[] getASMTransformerClass() {
        System.out.println("[UhcCraftHelper] getASMTransformerClass called");
        return new String[]{"com.daohe.autumnmod.coremod.MouseHelperTransformer"};
    }

    @Override
    public String getModContainerClass() {
        System.out.println("[UhcCraftHelper] getModContainerClass called");
        return null;
    }

    @Override
    public String getSetupClass() {
        System.out.println("[UhcCraftHelper] getSetupClass called");
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        System.out.println("[UhcCraftHelper] injectData called with: " + data);
    }

    @Override
    public String getAccessTransformerClass() {
        System.out.println("[UhcCraftHelper] getAccessTransformerClass called");
        return null;
    }
}