/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.remote.GTaskSyncService;

// 该类继承自PreferenceActivity，用于处理笔记应用的设置相关功能
public class NotesPreferenceActivity extends PreferenceActivity {
    // 共享偏好设置的名称
    public static final String PREFERENCE_NAME = "notes_preferences";
    // 用于存储同步账户名称的偏好设置键
    public static final String PREFERENCE_SYNC_ACCOUNT_NAME = "pref_key_account_name";
    // 用于存储上次同步时间的偏好设置键
    public static final String PREFERENCE_LAST_SYNC_TIME = "pref_last_sync_time";
    // 用于设置背景颜色随机显示的偏好设置键
    public static final String PREFERENCE_SET_BG_COLOR_KEY = "pref_key_bg_random_appear";
    // 同步账户设置的偏好类别键
    private static final String PREFERENCE_SYNC_ACCOUNT_KEY = "pref_sync_account_key";
    // 账户权限过滤的键
    private static final String AUTHORITIES_FILTER_KEY = "authorities";

    // 同步账户设置的偏好类别
    private PreferenceCategory mAccountCategory;
    // 用于接收GTask同步服务广播的接收器
    private GTaskReceiver mReceiver;
    // 原始的Google账户列表
    private Account[] mOriAccounts;
    // 标记是否添加了新账户
    private boolean mHasAddedAccount;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // 设置ActionBar的导航功能，允许通过应用图标返回上一级
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // 从XML资源文件加载偏好设置
        addPreferencesFromResource(R.xml.preferences);
        // 查找同步账户设置的偏好类别
        mAccountCategory = (PreferenceCategory) findPreference(PREFERENCE_SYNC_ACCOUNT_KEY);
        // 初始化广播接收器
        mReceiver = new GTaskReceiver();
        // 创建意图过滤器，过滤GTask同步服务的广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(GTaskSyncService.GTASK_SERVICE_BROADCAST_NAME);
        // 注册广播接收器
        registerReceiver(mReceiver, filter);

        // 初始化原始账户列表为空
        mOriAccounts = null;
        // 加载设置页面的头部视图
        View header = LayoutInflater.from(this).inflate(R.layout.settings_header, null);
        // 将头部视图添加到列表视图中
        getListView().addHeaderView(header, null, true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 如果添加了新账户，尝试自动设置同步账户
        if (mHasAddedAccount) {
            Account[] accounts = getGoogleAccounts();
            if (mOriAccounts != null && accounts.length > mOriAccounts.length) {
                for (Account accountNew : accounts) {
                    boolean found = false;
                    for (Account accountOld : mOriAccounts) {
                        if (TextUtils.equals(accountOld.name, accountNew.name)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        setSyncAccount(accountNew.name);
                        break;
                    }
                }
            }
        }

        // 刷新设置页面的UI
        refreshUI();
    }

    @Override
    protected void onDestroy() {
        // 注销广播接收器，避免内存泄漏
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }

    // 加载同步账户设置的偏好项
    private void loadAccountPreference() {
        // 清空同步账户设置的偏好类别
        mAccountCategory.removeAll();

        // 创建一个新的偏好项
        Preference accountPref = new Preference(this);
        // 获取当前设置的同步账户名称
        final String defaultAccount = getSyncAccountName(this);
        // 设置偏好项的标题
        accountPref.setTitle(getString(R.string.preferences_account_title));
        // 设置偏好项的摘要
        accountPref.setSummary(getString(R.string.preferences_account_summary));
        // 设置偏好项的点击监听器
        accountPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                // 如果当前没有正在进行同步操作
                if (!GTaskSyncService.isSyncing()) {
                    if (TextUtils.isEmpty(defaultAccount)) {
                        // 如果还没有设置同步账户，显示选择账户的对话框
                        showSelectAccountAlertDialog();
                    } else {
                        // 如果已经设置了同步账户，显示更改账户的确认对话框
                        showChangeAccountConfirmAlertDialog();
                    }
                } else {
                    // 如果正在同步，提示用户不能更改账户
                    Toast.makeText(NotesPreferenceActivity.this,
                            R.string.preferences_toast_cannot_change_account, Toast.LENGTH_SHORT)
                            .show();
                }
                return true;
            }
        });

        // 将偏好项添加到同步账户设置的偏好类别中
        mAccountCategory.addPreference(accountPref);
    }

    // 加载同步按钮和上次同步时间的显示
    private void loadSyncButton() {
        // 查找同步按钮和上次同步时间的文本视图
        Button syncButton = (Button) findViewById(R.id.preference_sync_button);
        TextView lastSyncTimeView = (TextView) findViewById(R.id.prefenerece_sync_status_textview);

        // 根据同步状态设置按钮的文本和点击监听器
        if (GTaskSyncService.isSyncing()) {
            syncButton.setText(getString(R.string.preferences_button_sync_cancel));
            syncButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // 取消同步操作
                    GTaskSyncService.cancelSync(NotesPreferenceActivity.this);
                }
            });
        } else {
            syncButton.setText(getString(R.string.preferences_button_sync_immediately));
            syncButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // 开始同步操作
                    GTaskSyncService.startSync(NotesPreferenceActivity.this);
                }
            });
        }
        // 只有当设置了同步账户时，同步按钮才可用
        syncButton.setEnabled(!TextUtils.isEmpty(getSyncAccountName(this)));

        // 根据同步状态设置上次同步时间的显示
        if (GTaskSyncService.isSyncing()) {
            lastSyncTimeView.setText(GTaskSyncService.getProgressString());
            lastSyncTimeView.setVisibility(View.VISIBLE);
        } else {
            long lastSyncTime = getLastSyncTime(this);
            if (lastSyncTime != 0) {
                lastSyncTimeView.setText(getString(R.string.preferences_last_sync_time,
                        DateFormat.format(getString(R.string.preferences_last_sync_time_format),
                                lastSyncTime)));
                lastSyncTimeView.setVisibility(View.VISIBLE);
            } else {
                lastSyncTimeView.setVisibility(View.GONE);
            }
        }
    }

    // 刷新设置页面的UI
    private void refreshUI() {
        loadAccountPreference();
        loadSyncButton();
    }

    // 显示选择账户的对话框
    private void showSelectAccountAlertDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        // 加载对话框的标题视图
        View titleView = LayoutInflater.from(this).inflate(R.layout.account_dialog_title, null);
        TextView titleTextView = (TextView) titleView.findViewById(R.id.account_dialog_title);
        titleTextView.setText(getString(R.string.preferences_dialog_select_account_title));
        TextView subtitleTextView = (TextView) titleView.findViewById(R.id.account_dialog_subtitle);
        subtitleTextView.setText(getString(R.string.preferences_dialog_select_account_tips));

        // 设置对话框的自定义标题
        dialogBuilder.setCustomTitle(titleView);
        dialogBuilder.setPositiveButton(null, null);

        // 获取所有的Google账户
        Account[] accounts = getGoogleAccounts();
        // 获取当前设置的同步账户名称
        String defAccount = getSyncAccountName(this);

        // 记录原始账户列表
        mOriAccounts = accounts;
        // 标记未添加新账户
        mHasAddedAccount = false;

        if (accounts.length > 0) {
            CharSequence[] items = new CharSequence[accounts.length];
            final CharSequence[] itemMapping = items;
            int checkedItem = -1;
            int index = 0;
            for (Account account : accounts) {
                if (TextUtils.equals(account.name, defAccount)) {
                    checkedItem = index;
                }
                items[index++] = account.name;
            }
            // 设置单选列表项
            dialogBuilder.setSingleChoiceItems(items, checkedItem,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // 设置选择的账户为同步账户
                            setSyncAccount(itemMapping[which].toString());
                            // 关闭对话框
                            dialog.dismiss();
                            // 刷新UI
                            refreshUI();
                        }
                    });
        }

        // 加载添加账户的视图
        View addAccountView = LayoutInflater.from(this).inflate(R.layout.add_account_text, null);
        dialogBuilder.setView(addAccountView);

        final AlertDialog dialog = dialogBuilder.show();
        addAccountView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // 标记添加了新账户
                mHasAddedAccount = true;
                // 启动添加账户的设置页面
                Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
                intent.putExtra(AUTHORITIES_FILTER_KEY, new String[] {
                    "gmail-ls"
                });
                startActivityForResult(intent, -1);
                // 关闭对话框
                dialog.dismiss();
            }
        });
    }

    // 显示更改账户的确认对话框
    private void showChangeAccountConfirmAlertDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        // 加载对话框的标题视图
        View titleView = LayoutInflater.from(this).inflate(R.layout.account_dialog_title, null);
        TextView titleTextView = (TextView) titleView.findViewById(R.id.account_dialog_title);
        titleTextView.setText(getString(R.string.preferences_dialog_change_account_title,
                getSyncAccountName(this)));
        TextView subtitleTextView = (TextView) titleView.findViewById(R.id.account_dialog_subtitle);
        subtitleTextView.setText(getString(R.string.preferences_dialog_change_account_warn_msg));
        // 设置对话框的自定义标题
        dialogBuilder.setCustomTitle(titleView);

        // 设置菜单项
        CharSequence[] menuItemArray = new CharSequence[] {
                getString(R.string.preferences_menu_change_account),
                getString(R.string.preferences_menu_remove_account),
                getString(R.string.preferences_menu_cancel)
        };
        dialogBuilder.setItems(menuItemArray, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    // 显示选择账户的对话框
                    showSelectAccountAlertDialog();
                } else if (which == 1) {
                    // 移除同步账户
                    removeSyncAccount();
                    // 刷新UI
                    refreshUI();
                }
            }
        });
        dialogBuilder.show();
    }

    // 获取所有的Google账户
    private Account[] getGoogleAccounts() {
        AccountManager accountManager = AccountManager.get(this);
        return accountManager.getAccountsByType("com.google");
    }

    // 设置同步账户
    private void setSyncAccount(String account) {
        if (!getSyncAccountName(this).equals(account)) {
            // 获取共享偏好设置
            SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            if (account != null) {
                // 保存同步账户名称
                editor.putString(PREFERENCE_SYNC_ACCOUNT_NAME, account);
            } else {
                // 清空同步账户名称
                editor.putString(PREFERENCE_SYNC_ACCOUNT_NAME, "");
            }
            editor.commit();

            // 清空上次同步时间
            setLastSyncTime(this, 0);

            // 清理本地与GTask相关的信息
            new Thread(new Runnable() {
                public void run() {
                    ContentValues values = new ContentValues();
                    values.put(NoteColumns.GTASK_ID, "");
                    values.put(NoteColumns.SYNC_ID, 0);
                    getContentResolver().update(Notes.CONTENT_NOTE_URI, values, null, null);
                }
            }).start();

            // 提示用户设置同步账户成功
            Toast.makeText(NotesPreferenceActivity.this,
                    getString(R.string.preferences_toast_success_set_accout, account),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // 移除同步账户
    private void removeSyncAccount() {
        // 获取共享偏好设置
        SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        if (settings.contains(PREFERENCE_SYNC_ACCOUNT_NAME)) {
            // 移除同步账户名称
            editor.remove(PREFERENCE_SYNC_ACCOUNT_NAME);
        }
        if (settings.contains(PREFERENCE_LAST_SYNC_TIME)) {
            // 移除上次同步时间
            editor.remove(PREFERENCE_LAST_SYNC_TIME);
        }
        editor.commit();

        // 清理本地与GTask相关的信息
        new Thread(new Runnable() {
            public void run() {
                ContentValues values = new ContentValues();
                values.put(NoteColumns.GTASK_ID, "");
                values.put(NoteColumns.SYNC_ID, 0);
                getContentResolver().update(Notes.CONTENT_NOTE_URI, values, null, null);
            }
        }).start();
    }

    // 获取当前设置的同步账户名称
    public static String getSyncAccountName(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return settings.getString(PREFERENCE_SYNC_ACCOUNT_NAME, "");
    }

    // 设置上次同步时间
    public static void setLastSyncTime(Context context, long time) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(PREFERENCE_LAST_SYNC_TIME, time);
        editor.commit();
    }

    // 获取上次同步时间
    public static long getLastSyncTime(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return settings.getLong(PREFERENCE_LAST_SYNC_TIME, 0);
    }

    // 广播接收器，用于接收GTask同步服务的广播
    private class GTaskReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // 刷新UI
            refreshUI();
            if (intent.getBooleanExtra(GTaskSyncService.GTASK_SERVICE_BROADCAST_IS_SYNCING, false)) {
                // 显示同步进度信息
                TextView syncStatus = (TextView) findViewById(R.id.prefenerece_sync_status_textview);
                syncStatus.setText(intent
                        .getStringExtra(GTaskSyncService.GTASK_SERVICE_BROADCAST_PROGRESS_MSG));
            }

        }
    }

    // 处理菜单项的点击事件
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // 返回笔记列表页面
                Intent intent = new Intent(this, NotesListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }
}
