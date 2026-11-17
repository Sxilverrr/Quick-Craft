package com.sxilverr.quickcraft.integration.jei;

import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.MachineRecipeOption;
import com.sxilverr.quickcraft.integration.OriginHint;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@JeiPlugin
public class QuickCraftJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(QuickCraftCommon.MODID, "jei");

    private static final Set<ResourceLocation> SKIP_CATEGORIES = Set.of(
            RecipeTypes.CRAFTING.getUid(),
            RecipeTypes.STONECUTTING.getUid(),
            RecipeTypes.SMITHING.getUid(),
            RecipeTypes.ANVIL.getUid(),
            RecipeTypes.FUELING.getUid(),
            RecipeTypes.COMPOSTING.getUid(),
            RecipeTypes.INFORMATION.getUid());

    private static final int MAX_RECIPES = 4;

    private IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.runtime = jeiRuntime;
        if (emiLoaded()) return;
        QuickCraftIntegrations.setHoveredItemProvider(this::hoveredItem);
        QuickCraftIntegrations.setRecipeViewer(this::showRecipe);
        QuickCraftIntegrations.setOriginProvider(this::findOrigins);
        QuickCraftIntegrations.setTextInputFocused(this::searchFocused);
    }

    @Override
    public void onRuntimeUnavailable() {
        this.runtime = null;
        if (emiLoaded()) return;
        QuickCraftIntegrations.setHoveredItemProvider(null);
        QuickCraftIntegrations.setRecipeViewer(null);
        QuickCraftIntegrations.setOriginProvider(null);
        QuickCraftIntegrations.setTextInputFocused(null);
    }

    private static boolean emiLoaded() {
        try {
            return ModList.get().isLoaded("emi");
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean searchFocused() {
        return runtime != null && runtime.getIngredientListOverlay().hasKeyboardFocus();
    }

    private void showRecipe(ItemStack stack, boolean uses) {
        if (runtime == null || stack == null || stack.isEmpty()) return;
        IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
        RecipeIngredientRole role = uses ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
        runtime.getRecipesGui().show(focusFactory.createFocus(role, VanillaTypes.ITEM_STACK, stack));
    }

    private List<OriginHint> findOrigins(ItemStack stack) {
        if (runtime == null || stack == null || stack.isEmpty()) return List.of();
        try {
            IRecipeManager recipeManager = runtime.getRecipeManager();
            IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
            IFocus<ItemStack> focus = focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack);
            IFocusGroup group = focusFactory.createFocusGroup(List.of(focus));
            List<OriginHint> hints = new ArrayList<>();
            for (IRecipeCategory<?> category : recipeManager.createRecipeCategoryLookup()
                    .limitFocus(List.of(focus)).get().toList()) {
                if (SKIP_CATEGORIES.contains(category.getRecipeType().getUid())) continue;
                ItemStack icon = recipeManager.createRecipeCatalystLookup(category.getRecipeType())
                        .getItemStack().findFirst().orElse(ItemStack.EMPTY);
                OriginHint hint = new OriginHint(icon, category.getTitle().getString(), new ArrayList<>());
                addRecipes(recipeManager, category, focus, group, stack, hint);
                hints.add(hint);
            }
            return hints;
        } catch (Throwable t) {
            return List.of();
        }
    }

    private static <T> void addRecipes(IRecipeManager manager, IRecipeCategory<T> category, IFocus<ItemStack> focus,
                                       IFocusGroup group, ItemStack target, OriginHint hint) {
        try {
            List<T> recipes = manager.createRecipeLookup(category.getRecipeType()).limitFocus(List.of(focus)).get()
                    .limit(MAX_RECIPES).toList();
            for (T recipe : recipes) {
                IRecipeSlotsView view = manager.createRecipeLayoutDrawable(category, recipe, group)
                        .map(IRecipeLayoutDrawable::getRecipeSlotsView).orElse(null);
                if (view == null) continue;
                List<MachineRecipeOption.Slot> slots = new ArrayList<>();
                for (IRecipeSlotView slot : view.getSlotViews(RecipeIngredientRole.INPUT)) {
                    List<ItemStack> options = slot.getIngredients(VanillaTypes.ITEM_STACK).filter(s -> !s.isEmpty()).toList();
                    if (options.isEmpty()) continue;
                    slots.add(new MachineRecipeOption.Slot(options.get(0).getCount(),
                            options.stream().map(s -> s.copyWithCount(1)).toList()));
                }
                if (slots.isEmpty()) continue;
                int made = view.getSlotViews(RecipeIngredientRole.OUTPUT).stream()
                        .flatMap(slot -> slot.getIngredients(VanillaTypes.ITEM_STACK))
                        .filter(s -> ItemStack.isSameItem(s, target))
                        .mapToInt(ItemStack::getCount).findFirst().orElse(1);
                if (category.getRecipeType().getUid().equals(RecipeTypes.BREWING.getUid())) made = 3;
                ResourceLocation id = category.getRegistryName(recipe);
                if (id == null) {
                    id = MachineRecipeOption.fallbackId(category.getRecipeType().getUid(), ItemKey.of(target).toKey(), hint.recipes().size());
                }
                hint.recipes().add(new MachineRecipeOption(id, target.copyWithCount(1), made, slots, hint.icon(), hint.label()));
            }
        } catch (Throwable ignored) {
        }
    }

    private ItemStack hoveredItem() {
        if (runtime == null) return ItemStack.EMPTY;
        if (runtime.getIngredientListOverlay().hasKeyboardFocus()) return ItemStack.EMPTY;

        ItemStack fromList = runtime.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
        if (fromList != null && !fromList.isEmpty()) return fromList;

        ItemStack fromBookmark = runtime.getBookmarkOverlay().getItemStackUnderMouse();
        if (fromBookmark != null && !fromBookmark.isEmpty()) return fromBookmark;

        ItemStack fromScreen = ingredientUnderMouseInScreen();
        if (!fromScreen.isEmpty()) return fromScreen;

        return runtime.getRecipesGui().getIngredientUnderMouse(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
    }

    private ItemStack ingredientUnderMouseInScreen() {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (screen == null) return ItemStack.EMPTY;
        double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
        return runtime.getScreenHelper().getClickableIngredientUnderMouse(screen, mx, my)
                .map(c -> c.getTypedIngredient().getItemStack().orElse(ItemStack.EMPTY))
                .filter(stack -> !stack.isEmpty())
                .findFirst()
                .orElse(ItemStack.EMPTY);
    }
}
