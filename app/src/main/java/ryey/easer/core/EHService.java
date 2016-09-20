/*
 * Copyright (c) 2016 - 2017 Rui Zhao <renyuneyun@gmail.com>
 *
 * This file is part of Easer.
 *
 * Easer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Easer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Easer.  If not, see <http://www.gnu.org/licenses/>.
 */

package ryey.easer.core;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ryey.easer.core.data.EventTree;
import ryey.easer.core.data.storage.EventDataStorage;
import ryey.easer.core.data.storage.xml.event.XmlEventDataStorage;
import ryey.easer.plugins.PluginRegistry;

public class EHService extends Service {
    public static final String ACTION_RELOAD = "ryey.easer.action.RELOAD";

    public static final String ACTION_STATE_CHANGED = "ryey.easer.action.STATE_CHANGED";

    List<Lotus> mLotus;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(EHService.class.getSimpleName(), "Received broadcast: " + action);
            switch (action) {
                case ACTION_RELOAD:
                    reloadTriggers();
            }
        }
    };

    private static boolean running = false;

    {
        mLotus = new ArrayList<>();
    }

    public static boolean isRunning() {
        return running;
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, EHService.class);
        context.startService(intent);
    }

    public static void reload(Context context) {
        Intent intent = new Intent();
        intent.setAction(EHService.ACTION_RELOAD);
        context.sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        Log.d(getClass().getSimpleName(), "onCreate");
        running = true;
        Intent intent = new Intent(ACTION_STATE_CHANGED);
        sendBroadcast(intent);
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RELOAD);
        registerReceiver(mReceiver, filter);
        PluginRegistry.init();
        reloadTriggers();
    }

    @Override
    public void onDestroy() {
        Log.d(getClass().getSimpleName(), "onDestroy");
        super.onDestroy();
        unregisterReceiver(mReceiver);
        running = false;
        Intent intent = new Intent(ACTION_STATE_CHANGED);
        sendBroadcast(intent);
    }

    private void reloadTriggers() {
        Log.d(getClass().getSimpleName(), "reloadTriggers");
        mCancelTriggers();
        try {
            EventDataStorage storage = XmlEventDataStorage.getInstance(this);
            List<EventTree> events = storage.getEventTrees();
            mSetTriggers(events);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mCancelTriggers() {
        for (Lotus lotus : mLotus) {
            lotus.cancel();
        }
        mLotus.clear();
    }

    private void mSetTriggers(List<EventTree> eventTreeList) {
        Log.d(getClass().getSimpleName(), "setting triggers");
        for (EventTree event : eventTreeList) {
            Log.d(getClass().getSimpleName(), "  setting: " + event.getName());
            Lotus lotus = new Lotus(this, event);
            lotus.apply();
            Log.d(getClass().getSimpleName(), "  " + event.getName() + " is set");
            mLotus.add(lotus);
        }
        Log.d(getClass().getSimpleName(), "triggers have been set");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
