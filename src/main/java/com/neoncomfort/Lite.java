package com.neoncomfort;

import java.util.ArrayList;
import java.util.List;

/**
 * Лайт-режим: одним нажатием выключает всё со страниц 2-5 (хвост, частицы, косметика, эффекты...).
 * Страница 1 и FPS Boost не трогаются. Кнопки не блокируются: любую вещь можно включить обратно.
 * При выключении лайта возвращаются прежние значения тех пунктов, которые вы не меняли сами.
 */
public final class Lite {
    /** значения всех модулей до включения лайта (в порядке Modules.ALL), null если нет */
    private static int[] backup;

    /** пункты, которые лайт выключает: страницы 2-5, кроме FPS Boost; переключатели и настройки с пунктом OFF */
    private static List<Modules.Module> affected() {
        List<Modules.Module> res = new ArrayList<>();
        for (int pg = 1; pg < Modules.PAGES.size(); pg++) {
            for (Modules.Module m : Modules.PAGES.get(pg)) {
                if (m == Modules.FPS_BOOST) continue;
                if (!m.isOption() || m.zeroIsOff) res.add(m);
            }
        }
        return res;
    }

    /** выключить все эффекты на страницах 2-5 (кроме FPS Boost); используется Lite и пресетами */
    static void clearEffects() {
        for (Modules.Module m : affected()) {
            if (m.isOption()) m.index = 0;
            else m.enabled = false;
        }
    }

    private static boolean cleared(Modules.Module m) {
        return m.isOption() ? m.index == 0 : !m.enabled;
    }

    public static void setEnabled(boolean on) {
        if (on == Modules.LITE.enabled) return;
        if (on) {
            backup = snapshot();
            for (Modules.Module m : affected()) {
                if (m.isOption()) m.index = 0;
                else m.enabled = false;
            }
            Modules.LITE.enabled = true;
        } else {
            Modules.LITE.enabled = false;
            restore();
            backup = null;
        }
        Config.save();
    }

    private static int[] snapshot() {
        List<Modules.Module> all = Modules.ALL;
        int[] a = new int[all.size()];
        for (int i = 0; i < a.length; i++) {
            Modules.Module m = all.get(i);
            a[i] = m.isOption() ? m.index : (m.enabled ? 1 : 0);
        }
        return a;
    }

    /** вернуть прежние значения, но не трогать то, что вы включили сами, пока был лайт */
    private static void restore() {
        List<Modules.Module> all = Modules.ALL;
        if (backup == null || backup.length != all.size()) return;
        for (Modules.Module m : affected()) {
            int i = all.indexOf(m);
            if (i < 0 || !cleared(m)) continue;
            if (m.isOption()) {
                if (backup[i] >= 0 && backup[i] < m.values.length) m.index = backup[i];
            } else {
                m.enabled = backup[i] != 0;
            }
        }
    }

    // --- сохранение копии настроек в конфиг ---
    static String serialize() {
        if (backup == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < backup.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(backup[i]);
        }
        return sb.toString();
    }

    static void deserialize(String s) {
        backup = null;
        if (s == null || s.trim().isEmpty()) return;
        try {
            String[] t = s.trim().split(",");
            int[] a = new int[t.length];
            for (int i = 0; i < t.length; i++) a[i] = Integer.parseInt(t[i].trim());
            backup = a;
        } catch (NumberFormatException e) {
            backup = null;
        }
    }

    private Lite() {}
}
