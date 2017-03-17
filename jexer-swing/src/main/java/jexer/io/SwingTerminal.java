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

import static jexer.TCommand.cmAbort;
import static jexer.TKeypress.kbBackspace;
import static jexer.TKeypress.kbDel;
import static jexer.TKeypress.kbEnter;
import static jexer.TKeypress.kbEsc;
import static jexer.TKeypress.kbShiftTab;
import static jexer.TKeypress.kbTab;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.LinkedList;
import java.util.List;

import jexer.TKeypress;
import jexer.backend.AbstractTerminal;
import jexer.event.TCommandEvent;
import jexer.event.TInputEvent;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;
import jexer.session.SessionInfo;
import jexer.session.SwingSessionInfo;

/**
 * This class reads keystrokes and mouse events from an Swing JFrame.
 */
public final class SwingTerminal extends AbstractTerminal implements ComponentListener, KeyListener,
                               MouseListener, MouseMotionListener,
                               MouseWheelListener, WindowListener {

    /**
     * The backend Screen.
     */
    private SwingScreen screen;

    /**
     * The session information.
     */
    private SwingSessionInfo sessionInfo;

    /**
     * Getter for sessionInfo.
     *
     * @return the SessionInfo
     */
    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    /**
     * The event queue, filled up by a thread reading on input.
     */
    private List<TInputEvent> eventQueue;

    /**
     * The last reported mouse X position.
     */
    private int oldMouseX = -1;

    /**
     * The last reported mouse Y position.
     */
    private int oldMouseY = -1;

    /**
     * true if mouse1 was down.  Used to report mouse1 on the release event.
     */
    private boolean mouse1 = false;

    /**
     * true if mouse2 was down.  Used to report mouse2 on the release event.
     */
    private boolean mouse2 = false;

    /**
     * true if mouse3 was down.  Used to report mouse3 on the release event.
     */
    private boolean mouse3 = false;

    /**
     * Check if there are events in the queue.
     *
     * @return if true, getEvents() has something to return to the backend
     */
    public boolean hasEvents() {
        synchronized (eventQueue) {
            return (eventQueue.size() > 0);
        }
    }
    
    /**
     * Constructor sets up state for getEvent().
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param screen the top-level Swing frame
     */
    public SwingTerminal(final SwingScreen screen) {
        this.screen      = screen;
        mouse1           = false;
        mouse2           = false;
        mouse3           = false;
        sessionInfo      = screen.getSessionInfo();
        eventQueue       = new LinkedList<TInputEvent>();

        screen.frame.addKeyListener(this);
        screen.frame.addWindowListener(this);
        screen.frame.addComponentListener(this);
        screen.frame.addMouseListener(this);
        screen.frame.addMouseMotionListener(this);
        screen.frame.addMouseWheelListener(this);
    }

    /**
     * Return any events in the IO queue.
     *
     * @param queue list to append new events to
     */
    public void getEvents(final List<TInputEvent> queue) {
        synchronized (eventQueue) {
            if (eventQueue.size() > 0) {
                synchronized (queue) {
                    queue.addAll(eventQueue);
                }
                eventQueue.clear();
            }
        }
    }

    /**
     * Pass Swing keystrokes into the event queue.
     *
     * @param key keystroke received
     */
    public void keyReleased(final KeyEvent key) {
        // Ignore release events
    }

    /**
     * Pass Swing keystrokes into the event queue.
     *
     * @param key keystroke received
     */
    public void keyTyped(final KeyEvent key) {
        // Ignore typed events
    }

    /**
     * Pass Swing keystrokes into the event queue.
     *
     * @param key keystroke received
     */
    public void keyPressed(final KeyEvent key) {
        boolean alt = false;
        boolean shift = false;
        boolean ctrl = false;
        char ch = ' ';
        boolean isKey = false;
        if (key.isActionKey()) {
            isKey = true;
        } else {
            ch = key.getKeyChar();
        }
        alt = key.isAltDown();
        ctrl = key.isControlDown();
        shift = key.isShiftDown();

        /*
        System.err.printf("Swing Key: %s\n", key);
        System.err.printf("   isKey: %s\n", isKey);
        System.err.printf("   alt: %s\n", alt);
        System.err.printf("   ctrl: %s\n", ctrl);
        System.err.printf("   shift: %s\n", shift);
        System.err.printf("   ch: %s\n", ch);
        */

        // Special case: not return the bare modifier presses
        switch (key.getKeyCode()) {
        case KeyEvent.VK_ALT:
            return;
        case KeyEvent.VK_ALT_GRAPH:
            return;
        case KeyEvent.VK_CONTROL:
            return;
        case KeyEvent.VK_SHIFT:
            return;
        case KeyEvent.VK_META:
            return;
        default:
            break;
        }

        TKeypress keypress = null;
        if (isKey) {
            switch (key.getKeyCode()) {
            case KeyEvent.VK_F1:
                keypress = new TKeypress(true, TKeypress.F1, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_F2:
                keypress = new TKeypress(true, TKeypress.F2, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_F3:
                keypress = new TKeypress(true, TKeypress.F3, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_F4:
                keypress = new TKeypress(true, TKeypress.F4, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_F5:
                keypress = new TKeypress(true, TKeypress.F5, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_F6:
                keypress = new TKeypress(true, TKeypress.F6, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_F7:
                keypress = new TKeypress(true, TKeypress.F7, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_F8:
                keypress = new TKeypress(true, TKeypress.F8, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_F9:
                keypress = new TKeypress(true, TKeypress.F9, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_F10:
                keypress = new TKeypress(true, TKeypress.F10, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_F11:
                keypress = new TKeypress(true, TKeypress.F11, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_F12:
                keypress = new TKeypress(true, TKeypress.F12, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_HOME:
                keypress = new TKeypress(true, TKeypress.HOME, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_END:
                keypress = new TKeypress(true, TKeypress.END, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_PAGE_UP:
                keypress = new TKeypress(true, TKeypress.PGUP, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_PAGE_DOWN:
                keypress = new TKeypress(true, TKeypress.PGDN, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_INSERT:
                keypress = new TKeypress(true, TKeypress.INS, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_DELETE:
                keypress = new TKeypress(true, TKeypress.DEL, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_RIGHT:
                keypress = new TKeypress(true, TKeypress.RIGHT, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_LEFT:
                keypress = new TKeypress(true, TKeypress.LEFT, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_UP:
                keypress = new TKeypress(true, TKeypress.UP, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_DOWN:
                keypress = new TKeypress(true, TKeypress.DOWN, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_TAB:
                // Special case: distinguish TAB vs BTAB
                if (shift) {
                    keypress = kbShiftTab;
                } else {
                    keypress = kbTab;
                }
                break;
            case KeyEvent.VK_ENTER:
                keypress = new TKeypress(true, TKeypress.ENTER, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_ESCAPE:
                keypress = new TKeypress(true, TKeypress.ESC, ' ',
                    alt, ctrl, shift);
                break;
            case KeyEvent.VK_BACK_SPACE:
                // Special case: return it as kbBackspace (Ctrl-H)
                keypress = new TKeypress(false, 0, 'H', false, true, false);
                break;
            default:
                // Unsupported, ignore
                return;
            }
        }

        if (keypress == null) {
            switch (ch) {
            case 0x08:
                keypress = kbBackspace;
                break;
            case 0x0A:
                keypress = kbEnter;
                break;
            case 0x1B:
                keypress = kbEsc;
                break;
            case 0x0D:
                keypress = kbEnter;
                break;
            case 0x09:
                if (shift) {
                    keypress = kbShiftTab;
                } else {
                    keypress = kbTab;
                }
                break;
            case 0x7F:
                keypress = kbDel;
                break;
            default:
                if (!alt && ctrl && !shift) {
                    ch = KeyEvent.getKeyText(key.getKeyCode()).charAt(0);
                }
                // Not a special key, put it together
                keypress = new TKeypress(false, 0, ch, alt, ctrl, shift);
            }
        }

        // Save it and we are done.
        synchronized (eventQueue) {
            eventQueue.add(new TKeypressEvent(keypress));
        }
        synchronized (listener) {
            listener.notifyAll();
        }
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowActivated(final WindowEvent event) {
        // Force a total repaint
        synchronized (screen) {
            screen.clearPhysical();
        }
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowClosed(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowClosing(final WindowEvent event) {
        // Drop a cmAbort and walk away
        synchronized (eventQueue) {
            eventQueue.add(new TCommandEvent(cmAbort));
        }
        synchronized (listener) {
            listener.notifyAll();
        }
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowDeactivated(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowDeiconified(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowIconified(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowOpened(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass component events into the event queue.
     *
     * @param event component event received
     */
    public void componentHidden(final ComponentEvent event) {
        // Ignore
    }

    /**
     * Pass component events into the event queue.
     *
     * @param event component event received
     */
    public void componentShown(final ComponentEvent event) {
        // Ignore
    }

    /**
     * Pass component events into the event queue.
     *
     * @param event component event received
     */
    public void componentMoved(final ComponentEvent event) {
        // Ignore
    }

    /**
     * Pass component events into the event queue.
     *
     * @param event component event received
     */
    public void componentResized(final ComponentEvent event) {
        // Drop a new TResizeEvent into the queue
        sessionInfo.queryWindowSize();
        synchronized (eventQueue) {
            TResizeEvent windowResize = new TResizeEvent(TResizeEvent.Type.SCREEN,
                sessionInfo.getWindowWidth(), sessionInfo.getWindowHeight());
            eventQueue.add(windowResize);
        }
        synchronized (listener) {
            listener.notifyAll();
        }
    }

    /**
     * Pass mouse events into the event queue.
     *
     * @param mouse mouse event received
     */
    public void mouseDragged(final MouseEvent mouse) {
        int modifiers = mouse.getModifiersEx();
        boolean eventMouse1 = false;
        boolean eventMouse2 = false;
        boolean eventMouse3 = false;
        if ((modifiers & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            eventMouse1 = true;
        }
        if ((modifiers & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
            eventMouse2 = true;
        }
        if ((modifiers & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
            eventMouse3 = true;
        }
        mouse1 = eventMouse1;
        mouse2 = eventMouse2;
        mouse3 = eventMouse3;
        int x = screen.textColumn(mouse.getX());
        int y = screen.textRow(mouse.getY());

        TMouseEvent mouseEvent = new TMouseEvent(TMouseEvent.Type.MOUSE_MOTION,
            x, y, x, y, mouse1, mouse2, mouse3, false, false);

        synchronized (eventQueue) {
            eventQueue.add(mouseEvent);
        }
        synchronized (listener) {
            listener.notifyAll();
        }
    }

    /**
     * Pass mouse events into the event queue.
     *
     * @param mouse mouse event received
     */
    public void mouseMoved(final MouseEvent mouse) {
        int x = screen.textColumn(mouse.getX());
        int y = screen.textRow(mouse.getY());
        if ((x == oldMouseX) && (y == oldMouseY)) {
            // Bail out, we've moved some pixels but not a whole text cell.
            return;
        }
        oldMouseX = x;
        oldMouseY = y;

        TMouseEvent mouseEvent = new TMouseEvent(TMouseEvent.Type.MOUSE_MOTION,
            x, y, x, y, mouse1, mouse2, mouse3, false, false);

        synchronized (eventQueue) {
            eventQueue.add(mouseEvent);
        }
        synchronized (listener) {
            listener.notifyAll();
        }
    }

    /**
     * Pass mouse events into the event queue.
     *
     * @param mouse mouse event received
     */
    public void mouseClicked(final MouseEvent mouse) {
        // Ignore
    }

    /**
     * Pass mouse events into the event queue.
     *
     * @param mouse mouse event received
     */
    public void mouseEntered(final MouseEvent mouse) {
        // Ignore
    }

    /**
     * Pass mouse events into the event queue.
     *
     * @param mouse mouse event received
     */
    public void mouseExited(final MouseEvent mouse) {
        // Ignore
    }

    /**
     * Pass mouse events into the event queue.
     *
     * @param mouse mouse event received
     */
    public void mousePressed(final MouseEvent mouse) {
        int modifiers = mouse.getModifiersEx();
        boolean eventMouse1 = false;
        boolean eventMouse2 = false;
        boolean eventMouse3 = false;
        if ((modifiers & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            eventMouse1 = true;
        }
        if ((modifiers & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
            eventMouse2 = true;
        }
        if ((modifiers & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
            eventMouse3 = true;
        }
        mouse1 = eventMouse1;
        mouse2 = eventMouse2;
        mouse3 = eventMouse3;
        int x = screen.textColumn(mouse.getX());
        int y = screen.textRow(mouse.getY());

        TMouseEvent mouseEvent = new TMouseEvent(TMouseEvent.Type.MOUSE_DOWN,
            x, y, x, y, mouse1, mouse2, mouse3, false, false);

        synchronized (eventQueue) {
            eventQueue.add(mouseEvent);
        }
        synchronized (listener) {
            listener.notifyAll();
        }
    }

    /**
     * Pass mouse events into the event queue.
     *
     * @param mouse mouse event received
     */
    public void mouseReleased(final MouseEvent mouse) {
        int modifiers = mouse.getModifiersEx();
        boolean eventMouse1 = false;
        boolean eventMouse2 = false;
        boolean eventMouse3 = false;
        if ((modifiers & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            eventMouse1 = true;
        }
        if ((modifiers & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
            eventMouse2 = true;
        }
        if ((modifiers & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
            eventMouse3 = true;
        }
        if (mouse1) {
            mouse1 = false;
            eventMouse1 = true;
        }
        if (mouse2) {
            mouse2 = false;
            eventMouse2 = true;
        }
        if (mouse3) {
            mouse3 = false;
            eventMouse3 = true;
        }
        int x = screen.textColumn(mouse.getX());
        int y = screen.textRow(mouse.getY());

        TMouseEvent mouseEvent = new TMouseEvent(TMouseEvent.Type.MOUSE_UP,
            x, y, x, y, eventMouse1, eventMouse2, eventMouse3, false, false);

        synchronized (eventQueue) {
            eventQueue.add(mouseEvent);
        }
        synchronized (listener) {
            listener.notifyAll();
        }
    }

    /**
     * Pass mouse events into the event queue.
     *
     * @param mouse mouse event received
     */
    public void mouseWheelMoved(final MouseWheelEvent mouse) {
        int modifiers = mouse.getModifiersEx();
        boolean eventMouse1 = false;
        boolean eventMouse2 = false;
        boolean eventMouse3 = false;
        boolean mouseWheelUp = false;
        boolean mouseWheelDown = false;
        if ((modifiers & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            eventMouse1 = true;
        }
        if ((modifiers & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
            eventMouse2 = true;
        }
        if ((modifiers & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
            eventMouse3 = true;
        }
        mouse1 = eventMouse1;
        mouse2 = eventMouse2;
        mouse3 = eventMouse3;
        int x = screen.textColumn(mouse.getX());
        int y = screen.textRow(mouse.getY());
        if (mouse.getWheelRotation() > 0) {
            mouseWheelDown = true;
        }
        if (mouse.getWheelRotation() < 0) {
            mouseWheelUp = true;
        }

        TMouseEvent mouseEvent = new TMouseEvent(TMouseEvent.Type.MOUSE_DOWN,
            x, y, x, y, mouse1, mouse2, mouse3, mouseWheelUp, mouseWheelDown);

        synchronized (eventQueue) {
            eventQueue.add(mouseEvent);
        }
        synchronized (listener) {
            listener.notifyAll();
        }
    }

}
