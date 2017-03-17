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
package jexer.io;

import jexer.bits.Cell;
import jexer.bits.CellAttributes;

/**
 * This Screen implementation draws to an xterm/ANSI X3.64/ECMA-48 type
 * terminal.
 */
public final class ECMA48Screen extends Screen {

    /**
     * Emit debugging to stderr.
     */
    private boolean debugToStderr;

    /**
     * We call terminal.cursor() so need the instance.
     */
    private ECMA48Terminal terminal;

    /**
     * Public constructor.
     *
     * @param terminal ECMA48Terminal to use
     */
    public ECMA48Screen(final ECMA48Terminal terminal) {
        debugToStderr = false;

        this.terminal = terminal;

        // Query the screen size
        setDimensions(terminal.getSessionInfo().getWindowWidth(),
            terminal.getSessionInfo().getWindowHeight());
    }

    /**
     * Perform a somewhat-optimal rendering of a line.
     *
     * @param y row coordinate.  0 is the top-most row.
     * @param sb StringBuilder to write escape sequences to
     * @param lastAttr cell attributes from the last call to flushLine
     */
    private void flushLine(final int y, final StringBuilder sb,
        CellAttributes lastAttr) {

        int lastX = -1;
        int textEnd = 0;
        for (int x = 0; x < width; x++) {
            Cell lCell = logical[x][y];
            if (!lCell.isBlank()) {
                textEnd = x;
            }
        }
        // Push textEnd to first column beyond the text area
        textEnd++;

        // DEBUG
        // reallyCleared = true;

        for (int x = 0; x < width; x++) {
            Cell lCell = logical[x][y];
            Cell pCell = physical[x][y];

            if (!lCell.equals(pCell) || reallyCleared) {

                if (debugToStderr) {
                    System.err.printf("\n--\n");
                    System.err.printf(" Y: %d X: %d\n", y, x);
                    System.err.printf("   lCell: %s\n", lCell);
                    System.err.printf("   pCell: %s\n", pCell);
                    System.err.printf("    ====    \n");
                }

                if (lastAttr == null) {
                    lastAttr = new CellAttributes();
                    sb.append(terminal.normal());
                }

                // Place the cell
                if ((lastX != (x - 1)) || (lastX == -1)) {
                    // Advancing at least one cell, or the first gotoXY
                    sb.append(terminal.gotoXY(x, y));
                }

                assert (lastAttr != null);

                if ((x == textEnd) && (textEnd < width - 1)) {
                    assert (lCell.isBlank());

                    for (int i = x; i < width; i++) {
                        assert (logical[i][y].isBlank());
                        // Physical is always updatesd
                        physical[i][y].reset();
                    }

                    // Clear remaining line
                    sb.append(terminal.clearRemainingLine());
                    lastAttr.reset();
                    return;
                }

                // Now emit only the modified attributes
                if ((lCell.getForeColor() != lastAttr.getForeColor())
                    && (lCell.getBackColor() != lastAttr.getBackColor())
                    && (lCell.isBold() == lastAttr.isBold())
                    && (lCell.isReverse() == lastAttr.isReverse())
                    && (lCell.isUnderline() == lastAttr.isUnderline())
                    && (lCell.isBlink() == lastAttr.isBlink())
                ) {
                    // Both colors changed, attributes the same
                    sb.append(terminal.color(lCell.getForeColor(),
                            lCell.getBackColor()));

                    if (debugToStderr) {
                        System.err.printf("1 Change only fore/back colors\n");
                    }
                } else if ((lCell.getForeColor() != lastAttr.getForeColor())
                    && (lCell.getBackColor() != lastAttr.getBackColor())
                    && (lCell.isBold() != lastAttr.isBold())
                    && (lCell.isReverse() != lastAttr.isReverse())
                    && (lCell.isUnderline() != lastAttr.isUnderline())
                    && (lCell.isBlink() != lastAttr.isBlink())
                ) {
                    // Everything is different
                    sb.append(terminal.color(lCell.getForeColor(),
                            lCell.getBackColor(),
                            lCell.isBold(), lCell.isReverse(),
                            lCell.isBlink(),
                            lCell.isUnderline()));

                    if (debugToStderr) {
                        System.err.printf("2 Set all attributes\n");
                    }
                } else if ((lCell.getForeColor() != lastAttr.getForeColor())
                    && (lCell.getBackColor() == lastAttr.getBackColor())
                    && (lCell.isBold() == lastAttr.isBold())
                    && (lCell.isReverse() == lastAttr.isReverse())
                    && (lCell.isUnderline() == lastAttr.isUnderline())
                    && (lCell.isBlink() == lastAttr.isBlink())
                ) {

                    // Attributes same, foreColor different
                    sb.append(terminal.color(lCell.getForeColor(), true));

                    if (debugToStderr) {
                        System.err.printf("3 Change foreColor\n");
                    }
                } else if ((lCell.getForeColor() == lastAttr.getForeColor())
                    && (lCell.getBackColor() != lastAttr.getBackColor())
                    && (lCell.isBold() == lastAttr.isBold())
                    && (lCell.isReverse() == lastAttr.isReverse())
                    && (lCell.isUnderline() == lastAttr.isUnderline())
                    && (lCell.isBlink() == lastAttr.isBlink())
                ) {
                    // Attributes same, backColor different
                    sb.append(terminal.color(lCell.getBackColor(), false));

                    if (debugToStderr) {
                        System.err.printf("4 Change backColor\n");
                    }
                } else if ((lCell.getForeColor() == lastAttr.getForeColor())
                    && (lCell.getBackColor() == lastAttr.getBackColor())
                    && (lCell.isBold() == lastAttr.isBold())
                    && (lCell.isReverse() == lastAttr.isReverse())
                    && (lCell.isUnderline() == lastAttr.isUnderline())
                    && (lCell.isBlink() == lastAttr.isBlink())
                ) {

                    // All attributes the same, just print the char
                    // NOP

                    if (debugToStderr) {
                        System.err.printf("5 Only emit character\n");
                    }
                } else {
                    // Just reset everything again
                    sb.append(terminal.color(lCell.getForeColor(),
                            lCell.getBackColor(),
                            lCell.isBold(),
                            lCell.isReverse(),
                            lCell.isBlink(),
                            lCell.isUnderline()));

                    if (debugToStderr) {
                        System.err.printf("6 Change all attributes\n");
                    }
                }
                // Emit the character
                sb.append(lCell.getChar());

                // Save the last rendered cell
                lastX = x;
                lastAttr.setTo(lCell);

                // Physical is always updated
                physical[x][y].setTo(lCell);

            } // if (!lCell.equals(pCell) || (reallyCleared == true))

        } // for (int x = 0; x < width; x++)
    }

    /**
     * Render the screen to a string that can be emitted to something that
     * knows how to process ECMA-48/ANSI X3.64 escape sequences.
     *
     * @return escape sequences string that provides the updates to the
     * physical screen
     */
    private String flushString() {
        if (!dirty) {
            assert (!reallyCleared);
            return "";
        }

        CellAttributes attr = null;

        StringBuilder sb = new StringBuilder();
        if (reallyCleared) {
            attr = new CellAttributes();
            sb.append(terminal.clearAll());
        }

        for (int y = 0; y < height; y++) {
            flushLine(y, sb, attr);
        }

        dirty = false;
        reallyCleared = false;

        String result = sb.toString();
        if (debugToStderr) {
            System.err.printf("flushString(): %s\n", result);
        }
        return result;
    }

    /**
     * Push the logical screen to the physical device.
     */
    @Override
    public void flushPhysical() {
        String result = flushString();
        if ((cursorVisible)
            && (cursorY <= height - 1)
            && (cursorX <= width - 1)
        ) {
            result += terminal.cursor(true);
            result += terminal.gotoXY(cursorX, cursorY);
        } else {
            result += terminal.cursor(false);
        }
        terminal.getOutput().write(result);
        terminal.flush();
    }
}
