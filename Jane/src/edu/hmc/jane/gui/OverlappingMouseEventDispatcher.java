/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.hmc.jane.gui;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * This listens directly on the event queue for MouseWheelEvents
 * but does polling for MouseMotionEvents because the performance of listening
 * on the event queue for MouseMotionEvents is unacceptable. It provides a means
 * of dispatching mouse events to components, even if they are partially or completely
 * covered by another component. It will not, however, dispatch events to any component
 * in a window that does not have focus. Note that at present, it only supports
 * MouseWheelEventListeners and MouseMotionEventListeners, (not MouseEventListeners)
 * but you could easily extend it to support
 * them via the event queue as well. Also, note that the MouseDragged event is not
 * supported.
 * @author John
 */
public class OverlappingMouseEventDispatcher {
    private static Set<WeakReference<Component>> mouseMotionListeners =
            Collections.synchronizedSet(new HashSet<WeakReference<Component>>());
    private static Set<WeakReference<Component>> mouseWheelListeners =
            Collections.synchronizedSet(new HashSet<WeakReference<Component>>());

    private static boolean listening=false;

    private static Timer timer = new Timer();

    private static int UPDATE_DELAY=10; //how often to update MouseMotionEvents
                                          //in ms
    private static Component dummySource=new JPanel();

    private static boolean dispatchingMouseWheelEvent=false;

    /*
     * tells the dispatcher to dispatch all mouse events that happen within
     * c's bounds to c, even if c is covered up by another component or a window.
     * Note that unregistration happens automatically if listener is garbage collected
     * and registering will not prevent listener from being garbage collected.
     */
    public static void register(StatelessMouseEventListener listener) {
        if(listener instanceof Component) {
            WeakReference<Component> c =
                    new WeakReference<Component>((Component) listener);

            if(listener instanceof MouseListener) {
                throw new UnsupportedOperationException("Support for MouseListeners"
                        + "is not yet implemented, but there is nothing stopping you"
                        + "from implementing it if you want to.");
            }
            if(listener instanceof MouseMotionListener) {
                mouseMotionListeners.add(c);
            }
            if(listener instanceof MouseWheelListener) {
                mouseWheelListeners.add(c);
            }
            listen();
        } else {
            throw new RuntimeException();
        }
    }

    private static void listen() {
        if(!listening) {
            timer.schedule(
                new TimerTask () {
                    @Override
                    public void run() {
                        try {
                            //make sure we are in the Swing Event dispatch thread
                            SwingUtilities.invokeAndWait(new Runnable() {
                                public void run() {
                                    OverlappingMouseEventDispatcher.dispatchMouseMotion();
                                }
                            });
                        } catch (InterruptedException ignore) {}
                          catch (InvocationTargetException ignore) {}
                    }
                },
                new Date(),
                UPDATE_DELAY
            );
            
            Toolkit.getDefaultToolkit().addAWTEventListener(
                new AWTEventListener() {
                    public void eventDispatched(AWTEvent event) {
                        if(event instanceof MouseWheelEvent)
                            OverlappingMouseEventDispatcher.dispatchMouseWheel(event);
                    }
                },
                MouseEvent.MOUSE_WHEEL_EVENT_MASK
            );
            
            listening=true;
        }
    }

    private static void dispatchMouseMotion() {
        Point p = MouseInfo.getPointerInfo().getLocation();

        MouseEvent event = new MouseEvent(dummySource,
            MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0,
            p.x, p.y, 0, false);

        dispatchAll(event, mouseMotionListeners);
    }

    private static void dispatchMouseWheel(AWTEvent event) {
        if(!dispatchingMouseWheelEvent) {
            try {
                dispatchingMouseWheelEvent=true;
                
                MouseWheelEvent evt = (MouseWheelEvent) event;

                Point p = evt.getPoint();
                SwingUtilities.convertPointToScreen(p, evt.getComponent());

                MouseWheelEvent w = new MouseWheelEvent(evt.getComponent(),
                        evt.getID(), evt.getWhen(), evt.getModifiers(),
                        p.x, p.y, evt.getClickCount(),
                        evt.isPopupTrigger(), evt.getScrollType(),evt.getScrollAmount(),
                        evt.getWheelRotation()
                    );

                dispatchAll(w, mouseWheelListeners);
            } finally {
                dispatchingMouseWheelEvent=false;
            }
        }
    }

    /*
     * dispatches the given event to all listeners if it is within their
     * bounds, converting it to their coordinate system first.
     */
    private static void dispatchAll(MouseEvent event, Set<WeakReference<Component>> listeners) {
         Iterator<WeakReference<Component>> i = listeners.iterator();

        while(i.hasNext()) {
            WeakReference<Component> ref = (WeakReference<Component>) i.next();
            if (ref.get()==null) {
                i.remove();
            } else {
                Component c = ref.get();

                Point p = event.getPoint();
                SwingUtilities.convertPointFromScreen(p, c);

                MouseEvent dispatchEvent;
                if(event instanceof MouseWheelEvent) {
                    MouseWheelEvent w = (MouseWheelEvent) event;
                    dispatchEvent = new MouseWheelEvent(event.getComponent(),
                        event.getID(), event.getWhen(), event.getModifiers(),
                        p.x, p.y, event.getClickCount(),
                        event.isPopupTrigger(), w.getScrollType(),w.getScrollAmount(),
                        w.getWheelRotation()
                    );
                }  else {
                    dispatchEvent = new MouseEvent(event.getComponent(),
                        event.getID(), event.getWhen(), event.getModifiers(),
                        p.x, p.y, event.getClickCount(),
                        event.isPopupTrigger()
                    );
                }

                if(c.contains(dispatchEvent.getPoint()) && getRoot(c).isFocusOwner()) {
                    c.dispatchEvent(dispatchEvent);
                }
            }
        }
    }

    private static Component getRoot(Component c) {
        Component root=c;
        while(root.getParent()!=null) {
            root=root.getParent();
        }

        return root;
    }
}
