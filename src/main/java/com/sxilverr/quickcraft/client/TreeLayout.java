package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.crafting.CraftNode;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class TreeLayout {
    public static final int H_GAP = 44;
    public static final int V_GAP = 10;

    public interface WidthFn {
        int widthOf(CraftNode node);
    }

    public final Map<CraftNode, NodeView> views = new IdentityHashMap<CraftNode, NodeView>();
    public final List<NodeView> ordered = new ArrayList<NodeView>();
    private final Map<CraftNode, Integer> widthCache = new IdentityHashMap<CraftNode, Integer>();
    private final List<Integer> columnX = new ArrayList<Integer>();
    private int nextLeafY;

    public TreeLayout(CraftNode root, WidthFn widthFn) {
        List<Integer> maxWidth = new ArrayList<Integer>();
        measure(root, 0, widthFn, maxWidth);
        int x = 0;
        for (int depth = 0; depth < maxWidth.size(); depth++) {
            columnX.add(x);
            x += maxWidth.get(depth) + H_GAP;
        }
        place(root, 0);
    }

    private void measure(CraftNode node, int depth, WidthFn widthFn, List<Integer> maxWidth) {
        int w = widthFn.widthOf(node);
        widthCache.put(node, w);
        if (depth == maxWidth.size()) {
            maxWidth.add(w);
        } else if (w > maxWidth.get(depth)) {
            maxWidth.set(depth, w);
        }
        for (CraftNode child : node.children) {
            measure(child, depth + 1, widthFn, maxWidth);
        }
    }

    private NodeView place(CraftNode node, int depth) {
        NodeView view = new NodeView(node);
        Integer cached = widthCache.get(node);
        view.width = cached == null ? NodeView.WIDTH : cached;
        view.x = columnX.get(depth);
        views.put(node, view);
        ordered.add(view);

        if (node.children.isEmpty()) {
            view.y = nextLeafY;
            nextLeafY += NodeView.HEIGHT + V_GAP;
        } else {
            int first = Integer.MIN_VALUE;
            int last = 0;
            for (CraftNode child : node.children) {
                NodeView childView = place(child, depth + 1);
                if (first == Integer.MIN_VALUE) first = childView.y;
                last = childView.y;
            }
            view.y = (first + last) / 2;
        }
        return view;
    }
}
