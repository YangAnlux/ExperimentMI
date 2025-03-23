/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License...（保留原有版权信息）
 */

package net.micode.notes.ui;

// 模块/库导入注释 ======================================================
import android.app.Activity;                // 基础活动类
import android.app.AlertDialog;              // 对话框组件
import android.content.Context;              // 上下文访问
import android.content.DialogInterface;     // 对话框交互接口
import android.content.Intent;               // 组件间通信
import android.media.AudioManager;           // 音频管理
import android.media.MediaPlayer;            // 媒体播放控制
import android.media.RingtoneManager;        // 系统铃声管理
import android.net.Uri;                      // 统一资源标识符
import android.os.Bundle;                   // 活动状态保存
import android.os.PowerManager;             // 电源管理
import android.provider.Settings;           // 系统设置访问
import android.view.Window;                 // 窗口管理
import android.view.WindowManager;          // 窗口参数管理

import net.micode.notes.R;                  // 资源文件
import net.micode.notes.data.Notes;         // 便签数据模型
import net.micode.notes.tool.DataUtils;     // 数据工具类

import java.io.IOException;                 // IO异常处理

// 类注释 =============================================================
/**
 * 闹钟提醒活动类
 * 功能：处理便签提醒的展示和交互，包含以下主要功能：
 * 1. 在锁屏界面显示提醒
 * 2. 播放系统默认闹铃音效
 * 3. 提供进入编辑界面或关闭提醒的选项
 */
public class AlarmAlertActivity extends Activity implements OnClickListener, OnDismissListener {
    // 变量注释 =========================================================
    private long mNoteId;                    // 当前提醒关联的便签ID
    private String mSnippet;                 // 便签内容摘要
    private static final int SNIPPET_PREW_MAX_LEN = 60; // 摘要最大显示长度
    MediaPlayer mPlayer;                     // 媒体播放器实例

    // 生命周期方法注释 =================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 窗口设置：隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // >>> 关键算法：屏幕显示控制
        final Window win = getWindow();
        // 在锁屏时显示窗口
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        // 屏幕关闭时的处理（添加保持屏幕唤醒的标记）
        if (!isScreenOn()) {
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON  // 强制点亮屏幕
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        }

        // 获取启动此活动的Intent
        Intent intent = getIntent();

        try {
            // >>> 关键算法：从URI解析便签ID
            // URI格式示例：content://net.micode.notes/note/1
            mNoteId = Long.valueOf(intent.getData().getPathSegments().get(1));
            
            // 从数据库获取便签内容摘要
            mSnippet = DataUtils.getSnippetById(this.getContentResolver(), mNoteId);
            
            // 摘要截断处理（超过长度添加省略标识）
            mSnippet = mSnippet.length() > SNIPPET_PREW_MAX_LEN 
                    ? mSnippet.substring(0, SNIPPET_PREW_MAX_LEN) 
                        + getResources().getString(R.string.notelist_string_info)
                    : mSnippet;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return;  // 数据异常直接结束活动
        }

        // 初始化媒体播放器
        mPlayer = new MediaPlayer();
        // 检查便签是否仍然有效
        if (DataUtils.visibleInNoteDatabase(getContentResolver(), mNoteId, Notes.TYPE_NOTE)) {
            showActionDialog();  // 显示操作对话框
            playAlarmSound();    // 播放提醒音效
        } else {
            finish();  // 便签已删除则结束
        }
    }

    // 工具方法注释 =====================================================
    /**
     * 检查屏幕是否亮起
     */
    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }

    // 音频处理相关方法 =================================================
    /**
     * 播放系统默认闹铃音效
     * 最佳实践提示：建议使用RingtoneManager替代MediaPlayer简化铃声播放
     */
    private void playAlarmSound() {
        // 获取系统默认闹铃URI
        Uri url = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);

        // >>> 关键算法：静音模式处理
        int silentModeStreams = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);

        // 检查静音模式是否会影响警报流
        if ((silentModeStreams & (1 << AudioManager.STREAM_ALARM)) != 0) {
            mPlayer.setAudioStreamType(silentModeStreams);
        } else {
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        }

        try {
            // 配置媒体播放器
            mPlayer.setDataSource(this, url);
            mPlayer.prepare();       // 同步准备（可能阻塞主线程）
            mPlayer.setLooping(true); // 循环播放
            mPlayer.start();
        } catch (IllegalArgumentException | SecurityException | 
               IllegalStateException | IOException e) {
            e.printStackTrace();
            // 边界条件说明：需要处理设备无默认铃声的情况
        }
    }

    // 对话框相关方法 ====================================================
    /**
     * 显示操作对话框
     * 注意事项：当屏幕关闭时不显示"进入"按钮
     */
    private void showActionDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.app_name);  // 使用应用名称作为标题
        dialog.setMessage(mSnippet);         // 显示便签摘要
        
        // 确认按钮（关闭对话框）
        dialog.setPositiveButton(R.string.notealert_ok, this);
        
        // 仅在屏幕亮起时显示进入按钮
        if (isScreenOn()) {
            dialog.setNegativeButton(R.string.notealert_enter, this);
        }
        
        // 设置对话框关闭监听
        dialog.show().setOnDismissListener(this);
    }

    // 事件处理 ========================================================
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEGATIVE:
                // 启动便签编辑活动
                Intent intent = new Intent(this, NoteEditActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra(Intent.EXTRA_UID, mNoteId);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        stopAlarmSound();  // 停止铃声
        finish();         // 结束活动
    }

    // 资源释放方法 ====================================================
    /**
     * 停止并释放媒体播放器资源
     * 最佳实践提示：必须释放MediaPlayer防止内存泄漏
     */
    private void stopAlarmSound() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();  // 释放系统资源
            mPlayer = null;     // 帮助GC回收
        }
    }
}
