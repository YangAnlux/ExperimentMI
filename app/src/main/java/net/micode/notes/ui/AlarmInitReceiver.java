/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License...（保留原有版权信息）
 */

package net.micode.notes.ui;

// 模块/库导入注释 ======================================================
import android.app.AlarmManager;          // 系统闹钟服务
import android.app.PendingIntent;         // 延时意图包装
import android.content.BroadcastReceiver; // 广播接收器基类
import android.content.ContentUris;       // URI工具类
import android.content.Context;           // 上下文环境
import android.content.Intent;            // 组件间通信
import android.database.Cursor;           // 数据库查询结果游标

import net.micode.notes.data.Notes;      // 便签数据契约类
import net.micode.notes.data.Notes.NoteColumns; // 便签列定义

// 类注释 =============================================================
/**
 * 闹钟初始化接收器
 * 功能：在设备启动或需要重新初始化时，设置所有未来提醒的便签闹钟
 * 工作原理：
 * 1. 查询所有提醒时间大于当前时间的有效便签
 * 2. 为每个符合条件的便签设置系统闹钟
 * 3. 当闹钟触发时启动AlarmReceiver处理提醒
 */
public class AlarmInitReceiver extends BroadcastReceiver {
    // 数据库查询相关配置 ================================================
    // 查询投影（需要获取的列）
    private static final String [] PROJECTION = new String [] {
        NoteColumns.ID,            // 便签ID列
        NoteColumns.ALERTED_DATE  // 提醒时间列
    };

    // 列索引常量
    private static final int COLUMN_ID                = 0; // ID列索引
    private static final int COLUMN_ALERTED_DATE      = 1; // 提醒时间列索引

    // 核心方法 ========================================================
    @Override
    public void onReceive(Context context, Intent intent) {
        // 获取当前系统时间
        long currentDate = System.currentTimeMillis();
        
        // >>> 关键算法：数据库查询
        // 查询条件：提醒时间 > 当前时间 且 类型为普通便签
        Cursor c = context.getContentResolver().query(
                Notes.CONTENT_NOTE_URI,    // 便签内容URI
                PROJECTION,                // 查询列
                NoteColumns.ALERTED_DATE + ">? AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE,
                new String[] { String.valueOf(currentDate) }, // 参数化查询
                null);

        if (c != null) {
            try {
                // 遍历查询结果
                if (c.moveToFirst()) {
                    do {
                        // 获取提醒时间
                        long alertDate = c.getLong(COLUMN_ALERTED_DATE);
                        
                        // >>> 关键算法：闹钟设置
                        // 创建指向AlarmReceiver的Intent
                        Intent sender = new Intent(context, AlarmReceiver.class);
                        // 设置数据URI（格式：content://net.micode.notes/note/[id]）
                        sender.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, 
                                c.getLong(COLUMN_ID)));
                        
                        // 创建PendingIntent（FLAG设为0，新创建的不会覆盖现有）
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                context, 0, sender, 0);
                        
                        // 获取系统闹钟服务
                        AlarmManager alarmManager = (AlarmManager) context
                                .getSystemService(Context.ALARM_SERVICE);
                        
                        // 设置精确唤醒型闹钟（RTC_WAKEUP会在设备休眠时唤醒）
                        alarmManager.set(AlarmManager.RTC_WAKEUP, alertDate, pendingIntent);
                    } while (c.moveToNext());
                }
            } finally {
                // 确保关闭Cursor（最佳实践：避免内存泄漏）
                c.close();
            }
        }
    }
}
