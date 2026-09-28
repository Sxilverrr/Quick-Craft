package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.crafting.CraftNode;

public class NodeView {
    public static final int WIDTH = 112;
    public static final int HEIGHT = 30;

    public final CraftNode node;
    public int x;
    public int y;
    public int width = WIDTH;

    public NodeView(CraftNode node) {
        this.node = node;
    }
}
