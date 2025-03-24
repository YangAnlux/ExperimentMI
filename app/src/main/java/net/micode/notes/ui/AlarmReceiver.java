/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License...（保留原有版权信息）
 */

package net.micode.notes.ui;

// 模块/库导入注释 ======================================================
import android.content.BroadcastReceiver; // 广播接收器基类
import android.content.Context;           // 上下文环境（访问系统服务）
import android.content.Intent;            // 组件间通信载体

// 类注释 =============================================================
/**
 * 闹钟触发接收器
 * 功能：接收系统闹钟触发事件，启动提醒界面
 * 工作流程：
 * 1. 接收来自AlarmManager的系统广播
 * 2. 将原始Intent转换为启动AlarmAlertActivity的请求
 * 3. 在独立任务栈中启动提醒界面
 * 
 * 注意：此类是AlarmInitReceiver设置的闹钟的实际接收端点
 */
public class AlarmReceiver extends BroadcastReceiver {
    
    // 核心方法 ========================================================
    @Override
    public void onReceive(Context context, Intent intent) {
        // >>> 关键算法：意图转换与启动
        // 重新设置目标Activity类（确保启动正确的提醒界面）
        intent.setClass(context, AlarmAlertActivity.class);
        
        // 添加任务栈标志（从非Activity上下文启动必须添加此标志）
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        // 启动提醒界面Activity
        context.startActivity(intent);
        
        /* 
         * >>> 最佳实践提示：
         * 1. 建议在此处添加唤醒锁保证设备在屏幕关闭时能亮屏显示
         * 2. 高版本Android需添加全屏Intent权限：
         *    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
         */
    }
}
