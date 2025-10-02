package cc.spea.currencycraft.datagen;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import cc.spea.currencycraft.CurrencyCraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.PoiTypeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.PoiTypeTags;
import net.minecraftforge.common.data.ExistingFileHelper;

public class CurrencyCraftPoiTypeTagsProvider extends PoiTypeTagsProvider {

    public CurrencyCraftPoiTypeTagsProvider(PackOutput p_256012_, CompletableFuture<Provider> p_256617_,
            @Nullable ExistingFileHelper existingFileHelper) {
        super(p_256012_, p_256617_, CurrencyCraft.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        tag(PoiTypeTags.ACQUIRABLE_JOB_SITE).addOptional(ResourceLocation.fromNamespaceAndPath(CurrencyCraft.MODID, "banker_poi"));
    }
    
}
