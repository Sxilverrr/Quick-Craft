package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;

import java.lang.reflect.Field;
import java.util.List;

public final class TextInputGuard {

    private static final int MAX_DEPTH = 6;
    private static final Field RECIPE_BOOK_SEARCH = findEditBoxField(RecipeBookComponent.class);

    private TextInputGuard() {
    }

    public static boolean isTyping(Screen screen) {
        if (screen == null) return false;
        if (screen instanceof ChatScreen
                || screen instanceof AbstractSignEditScreen
                || screen instanceof BookEditScreen
                || screen instanceof AbstractCommandBlockEditScreen) return true;
        if (QuickCraftIntegrations.isTextInputFocused()) return true;
        if (recipeBookTyping(screen)) return true;
        return scan(screen, 0);
    }

    private static boolean scan(ContainerEventHandler parent, int depth) {
        List<? extends GuiEventListener> children;
        try {
            children = parent.children();
        } catch (RuntimeException e) {
            return false;
        }
        if (children == null) return false;
        for (GuiEventListener child : children) {
            if (consumesText(child)) return true;
            if (depth < MAX_DEPTH && child instanceof ContainerEventHandler nested && scan(nested, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumesText(GuiEventListener listener) {
        if (listener instanceof EditBox box) return box.canConsumeInput();
        if (listener instanceof MultiLineEditBox box) return box.isFocused();
        return false;
    }

    private static boolean recipeBookTyping(Screen screen) {
        if (RECIPE_BOOK_SEARCH == null || !(screen instanceof RecipeUpdateListener listener)) return false;
        RecipeBookComponent book = listener.getRecipeBookComponent();
        if (book == null || !book.isVisible()) return false;
        try {
            return RECIPE_BOOK_SEARCH.get(book) instanceof EditBox box && box.canConsumeInput();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static Field findEditBoxField(Class<?> owner) {
        try {
            for (Field field : owner.getDeclaredFields()) {
                if (field.getType() == EditBox.class) {
                    field.setAccessible(true);
                    return field;
                }
            }
        } catch (RuntimeException e) {
            return null;
        }
        return null;
    }
}
