package br.com.flowlauncher;

import android.app.Notification;
import android.app.PendingIntent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** In-memory view of active notifications; no history, network or disk persistence. */
public final class HubService extends NotificationListenerService {
    private static volatile HubService instance;
    private static volatile List<Entry> entries = Collections.emptyList();
    private static volatile Runnable observer;
    static final class Entry {
        final String key, pkg, title, body;
        final long time;
        final PendingIntent action;
        Entry(StatusBarNotification s) {
            Notification n = s.getNotification(); Bundle e = n.extras;
            key = s.getKey(); pkg = s.getPackageName(); time = s.getPostTime(); action = n.contentIntent;
            title = safe(e == null ? null : e.getCharSequence(Notification.EXTRA_TITLE));
            CharSequence b = e == null ? null : e.getCharSequence(Notification.EXTRA_BIG_TEXT);
            if (b == null && e != null) b = e.getCharSequence(Notification.EXTRA_TEXT);
            body = safe(b);
        }
        private static String safe(CharSequence c) { return c == null ? "" : c.toString(); }
    }
    static boolean connected() { return instance != null; }
    static List<Entry> snapshot() { return entries; }
    static void observe(Runnable r) { observer = r; }
    static void dismiss(String key) {
        HubService s = instance;
        if (s != null) { try { s.cancelNotification(key); } catch (RuntimeException ignored) {} }
    }
    @Override public void onListenerConnected() { super.onListenerConnected(); instance = this; refresh(); }
    @Override public void onListenerDisconnected() { if (instance == this) { instance = null; publish(Collections.emptyList()); } super.onListenerDisconnected(); }
    @Override public void onDestroy() { if (instance == this) { instance = null; publish(Collections.emptyList()); } super.onDestroy(); }
    @Override public void onNotificationPosted(StatusBarNotification s) { refresh(); }
    @Override public void onNotificationRemoved(StatusBarNotification s) { refresh(); }
    private void refresh() {
        if (instance != this) return;
        try {
            List<Entry> next = new ArrayList<>(); StatusBarNotification[] active = getActiveNotifications();
            if (active != null) for (StatusBarNotification s : active) {
                Notification n = s.getNotification();
                if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0 || s.getPackageName().equals(getPackageName())) continue;
                next.add(new Entry(s));
            }
            next.sort(Comparator.comparingLong((Entry x) -> x.time).reversed());
            publish(Collections.unmodifiableList(next));
        } catch (RuntimeException ex) { publish(Collections.emptyList()); }
    }
    private static void publish(List<Entry> next) { entries = next; Runnable r = observer; if (r != null) r.run(); }
}
