/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2016 Kevin Lamonte
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * @author Kevin Lamonte [kevin.lamonte@gmail.com]
 * @version 1
 */
package jexer;

import static jexer.TKeypress.kbLeft;
import static jexer.TKeypress.kbRight;
import static jexer.TKeypress.kbSpace;

import java.util.ArrayList;
import java.util.List;

import jexer.bits.CellAttributes;
import jexer.bits.GraphicsChars;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;

/**
 * TTreeItem is a single item in a tree view.
 */
public class TTreeItem extends TWidget {

    /**
     * Hang onto reference to my parent TTreeView so I can call its reflow()
     * when I add a child node.
     */
    private TTreeView view;

    /**
     * Get the parent TTreeView.
     *
     * @return the parent TTreeView
     */
    public final TTreeView getTreeView() {
        return view;
    }

    /**
     * Displayable text for this item.
     */
    private String text;

    /**
     * Get the displayable text for this item.
     *
     * @return the displayable text for this item
     */
    public final String getText() {
        return text;
    }

    /**
     * Set the displayable text for this item.
     *
     * @param text the displayable text for this item
     */
    public final void setText(final String text) {
        this.text = text;
    }

    /**
     * If true, this item is expanded in the tree view.
     */
    private boolean expanded = true;

    /**
     * Get expanded value.
     *
     * @return if true, this item is expanded
     */
    public final boolean isExpanded() {
        return expanded;
    }

    /**
     * Set expanded value.
     *
     * @param expanded new value
     */
    public final void setExpanded(final boolean expanded) {
        this.expanded = expanded;
    }

    /**
     * If true, this item can be expanded in the tree view.
     */
    private boolean expandable = false;

    /**
     * Get expandable value.
     *
     * @return if true, this item is expandable
     */
    public final boolean isExpandable() {
        return expandable;
    }

    /**
     * Set expandable value.
     *
     * @param expandable new value
     */
    public final void setExpandable(final boolean expandable) {
        this.expandable = expandable;
    }

    /**
     * The vertical bars and such along the left side.
     */
    private String prefix = "";

    /**
     * Get the vertical bars and such along the left side.
     *
     * @return the vertical bars and such along the left side
     */
    public final String getPrefix() {
        return prefix;
    }

    /**
     * Whether or not this item is last in its parent's list of children.
     */
    private boolean last = false;

    /**
     * Tree level.  Note package private access.
     */
    int level = 0;

    /**
     * If true, this item will not be drawn.
     */
    private boolean invisible = false;

    /**
     * Set invisible value.
     *
     * @param invisible new value
     */
    public final void setInvisible(final boolean invisible) {
        this.invisible = invisible;
    }

    /**
     * True means selected.
     */
    private boolean selected = false;

    /**
     * Get selected value.
     *
     * @return if true, this item is selected
     */
    public final boolean isSelected() {
        return selected;
    }

    /**
     * Set selected value.
     *
     * @param selected new value
     */
    public final void setSelected(final boolean selected) {
        this.selected = selected;
    }

    /**
     * True means select-able.
     */
    private boolean selectable = true;

    /**
     * Set selectable value.
     *
     * @param selectable new value
     */
    public final void setSelectable(final boolean selectable) {
        this.selectable = selectable;
    }

    /**
     * Pointer to the previous keyboard-navigable item (kbUp).  Note package
     * private access.
     */
    TTreeItem keyboardPrevious = null;

    /**
     * Pointer to the next keyboard-navigable item (kbDown).  Note package
     * private access.
     */
    TTreeItem keyboardNext = null;

    /**
     * Public constructor.
     *
     * @param view root TTreeView
     * @param text text for this item
     * @param expanded if true, have it expanded immediately
     */
    public TTreeItem(final TTreeView view, final String text,
        final boolean expanded) {

        super(view, 0, 0, view.getWidth() - 3, 1);
        this.text = text;
        this.expanded = expanded;
        this.view = view;

        if (view.getTreeRoot() == null) {
            view.setTreeRoot(this, true);
        }

        view.reflow();
    }

    /**
     * Add a child item.
     *
     * @param text text for this item
     * @return the new child item
     */
    public TTreeItem addChild(final String text) {
        return addChild(text, true);
    }

    /**
     * Add a child item.
     *
     * @param text text for this item
     * @param expanded if true, have it expanded immediately
     * @return the new child item
     */
    public TTreeItem addChild(final String text, final boolean expanded) {
        TTreeItem item = new TTreeItem(view, text, expanded);
        item.level = this.level + 1;
        getChildren().add(item);
        view.reflow();
        return item;
    }

    /**
     * Recursively expand the tree into a linear array of items.
     *
     * @param prefix vertical bar of parent levels and such that is set on
     * each child
     * @param last if true, this is the "last" leaf node of a tree
     * @return additional items to add to the array
     */
    public List<TTreeItem> expandTree(final String prefix, final boolean last) {
        List<TTreeItem> array = new ArrayList<TTreeItem>();
        this.last = last;
        this.prefix = prefix;
        array.add(this);

        if ((getChildren().size() == 0) || !expanded) {
            return array;
        }

        String newPrefix = prefix;
        if (level > 0) {
            if (last) {
                newPrefix += "  ";
            } else {
                newPrefix += GraphicsChars.CP437[0xB3];
                newPrefix += ' ';
            }
        }
        for (int i = 0; i < getChildren().size(); i++) {
            TTreeItem item = (TTreeItem) getChildren().get(i);
            if (i == getChildren().size() - 1) {
                array.addAll(item.expandTree(newPrefix, true));
            } else {
                array.addAll(item.expandTree(newPrefix, false));
            }
        }
        return array;
    }

    /**
     * Get the x spot for the + or - to expand/collapse.
     *
     * @return column of the expand/collapse button
     */
    private int getExpanderX() {
        if ((level == 0) || (!expandable)) {
            return 0;
        }
        return prefix.length() + 3;
    }

    /**
     * Recursively unselect my or my children.
     */
    public void unselect() {
        if (selected == true) {
            selected = false;
            view.setSelected(null);
        }
        for (TWidget widget: getChildren()) {
            if (widget instanceof TTreeItem) {
                TTreeItem item = (TTreeItem) widget;
                item.unselect();
            }
        }
    }

    /**
     * Handle mouse release events.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        if ((mouse.getX() == (getExpanderX() - view.getHScroller().getValue()))
            && (mouse.getY() == 0)
        ) {
            if (selectable) {
                // Flip expanded flag
                expanded = !expanded;
                if (expanded == false) {
                    // Unselect children that became invisible
                    unselect();
                }
            }
            // Let subclasses do something with this
            onExpand();
        } else if (mouse.getY() == 0) {
            view.setSelected(this);
            view.dispatch();
        }

        // Update the screen after any thing has expanded/contracted
        view.reflow();
    }

    /**
     * Called when this item is expanded or collapsed.  this.expanded will be
     * true if this item was just expanded from a mouse click or keypress.
     */
    public void onExpand() {
        // Default: do nothing.
        if (!expandable) {
            return;
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbLeft)
            || keypress.equals(kbRight)
            || keypress.equals(kbSpace)
        ) {
            if (selectable) {
                // Flip expanded flag
                expanded = !expanded;
                if (expanded == false) {
                    // Unselect children that became invisible
                    unselect();
                }
                view.setSelected(this);
            }
            // Let subclasses do something with this
            onExpand();
        } else {
            // Pass other keys (tab etc.) on to TWidget's handler.
            super.onKeypress(keypress);
        }
    }

    /**
     * Draw this item to a window.
     */
    @Override
    public void draw() {
        if (invisible) {
            return;
        }

        int offset = -view.getHScroller().getValue();

        CellAttributes color = getTheme().getColor("ttreeview");
        CellAttributes textColor = getTheme().getColor("ttreeview");
        CellAttributes expanderColor = getTheme().getColor("ttreeview.expandbutton");
        CellAttributes selectedColor = getTheme().getColor("ttreeview.selected");

        if (!getParent().isAbsoluteActive()) {
            color = getTheme().getColor("ttreeview.inactive");
            textColor = getTheme().getColor("ttreeview.inactive");
        }

        if (!selectable) {
            textColor = getTheme().getColor("ttreeview.unreadable");
        }

        // Blank out the background
        getScreen().hLineXY(0, 0, getWidth(), ' ', color);

        String line = prefix;
        if (level > 0) {
            if (last) {
                line += GraphicsChars.CP437[0xC0];
            } else {
                line += GraphicsChars.CP437[0xC3];
            }
            line += GraphicsChars.CP437[0xC4];
            if (expandable) {
                line += "[ ] ";
            }
        }
        getScreen().putStringXY(offset, 0, line, color);
        if (selected) {
            getScreen().putStringXY(offset + line.length(), 0, text,
                selectedColor);
        } else {
            getScreen().putStringXY(offset + line.length(), 0, text, textColor);
        }
        if ((level > 0) && (expandable)) {
            if (expanded) {
                getScreen().putCharXY(offset + getExpanderX(), 0, '-',
                    expanderColor);
            } else {
                getScreen().putCharXY(offset + getExpanderX(), 0, '+',
                    expanderColor);
            }
        }
    }

}
