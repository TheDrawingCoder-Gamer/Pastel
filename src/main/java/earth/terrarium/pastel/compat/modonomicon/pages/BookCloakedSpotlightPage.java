package earth.terrarium.pastel.compat.modonomicon.pages;

import com.google.gson.JsonObject;
import com.klikli_dev.modonomicon.book.BookTextHolder;
import com.klikli_dev.modonomicon.book.conditions.BookCondition;
import com.klikli_dev.modonomicon.book.conditions.BookNoneCondition;
import com.klikli_dev.modonomicon.book.entries.BookContentEntry;
import com.klikli_dev.modonomicon.book.page.BookSpotlightPage;
import com.klikli_dev.modonomicon.util.BookGsonHelper;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

public class BookCloakedSpotlightPage extends BookSpotlightPage {
    private boolean wasTitleUnspecified = false;

    public BookCloakedSpotlightPage(BookTextHolder title, BookTextHolder text, Either<ItemStack, Ingredient> item, String anchor, BookCondition condition) {
        super(title, text, item, anchor, condition);
    }

    public static BookCloakedSpotlightPage fromJson(ResourceLocation entryId, JsonObject json, HolderLookup.Provider provider) {
        var title = BookGsonHelper.getAsBookTextHolder(json, "title", BookTextHolder.EMPTY, provider);
        var item = ITEM_CODEC.parse(provider.createSerializationContext(JsonOps.INSTANCE), json.get("item")).result().get();
        var text = BookGsonHelper.getAsBookTextHolder(json, "text", BookTextHolder.EMPTY, provider);
        var anchor = GsonHelper.getAsString(json, "anchor", "");
        var condition = json.has("condition")
                ? BookCondition.fromJson(entryId, json.getAsJsonObject("condition"), provider)
                : new BookNoneCondition();
        return new BookCloakedSpotlightPage(title, text, item, anchor, condition);
    }

    public static BookCloakedSpotlightPage fromNetwork(RegistryFriendlyByteBuf buffer) {
        var title = BookTextHolder.fromNetwork(buffer);
        var item = ITEM_STREAM_CODEC.decode(buffer);
        var text = BookTextHolder.fromNetwork(buffer);
        var anchor = buffer.readUtf();
        var condition = BookCondition.fromNetwork(buffer);
        return new BookCloakedSpotlightPage(title, text, item, anchor, condition);
    }

    // _Always_ recalculate an items hover name, if we are basing it off that
    @Override
    public BookTextHolder getTitle() {
        if (this.wasTitleUnspecified) {
            var item = this.item.map(i -> i, i -> i.getItems()[0]);

            return new BookTextHolder(item.getHoverName().copy()
                        .withStyle(Style.EMPTY
                                .withBold(true)
                                .withColor(this.getParentEntry().getBook().getDefaultTitleColor())
                        )
            );
        } else {
            return this.title;
        }
    }

    @Override
    public void build(Level level, BookContentEntry parentEntry, int pageNum) {
        wasTitleUnspecified = this.title.isEmpty();
        super.build(level, parentEntry, pageNum);
    }
}
