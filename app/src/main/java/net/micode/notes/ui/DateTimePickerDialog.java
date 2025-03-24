/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License...（保留原有版权信息）
 */

package net.micode.notes.ui;

// 模块/库导入注释 ======================================================
import java.util.Calendar;                  // 日期时间处理
import net.micode.notes.R;                 // 资源文件
import android.app.AlertDialog;            // 对话框基类
import android.content.Context;            // 上下文环境
import android.content.DialogInterface;    // 对话框交互接口
import android.text.format.DateFormat;     // 日期格式化工具
import android.text.format.DateUtils;      // 日期工具类

// 类注释 =============================================================
/**
 * 日期时间选择对话框
 * 功能：封装日期时间选择控件，提供完整的对话框交互界面
 * 特性：
 * 1. 集成DateTimePicker自定义控件
 * 2. 支持12/24小时制切换
 * 3. 自动更新对话框标题显示当前选择时间
 * 4. 提供确认/取消回调机制
 */
public class DateTimePickerDialog extends AlertDialog implements OnClickListener {
    // 成员变量注释 =====================================================
    private Calendar mDate = Calendar.getInstance();          // 当前选择的日期时间
    private boolean mIs24HourView;                            // 24小时制显示标志
    private OnDateTimeSetListener mOnDateTimeSetListener;     // 时间设置回调接口
    private DateTimePicker mDateTimePicker;                   // 日期时间选择控件实例

    // 接口定义 ========================================================
    /**
     * 日期时间设置回调接口
     */
    public interface OnDateTimeSetListener {
        void OnDateTimeSet(AlertDialog dialog, long date);
    }

    // 构造方法 ========================================================
    /**
     * 初始化日期时间选择对话框
     * @param context 上下文环境
     * @param date 初始时间戳
     *
     * >>> 初始化流程：
     * 1. 创建DateTimePicker实例并添加到对话框
     * 2. 设置时间变化监听器
     * 3. 初始化按钮和标题
     * 4. 配置时间显示格式
     */
    public DateTimePickerDialog(Context context, long date) {
        super(context);
        // 初始化日期时间选择控件
        mDateTimePicker = new DateTimePicker(context);
        setView(mDateTimePicker);  // 将选择器添加到对话框
        
        // 设置时间变化监听
        mDateTimePicker.setOnDateTimeChangedListener(new OnDateTimeChangedListener() {
            @Override
            public void onDateTimeChanged(DateTimePicker view, int year, int month,
                    int dayOfMonth, int hourOfDay, int minute) {
                // 更新当前选择的时间
                mDate.set(Calendar.YEAR, year);
                mDate.set(Calendar.MONTH, month);
                mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                mDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                mDate.set(Calendar.MINUTE, minute);
                updateTitle(mDate.getTimeInMillis());  // 刷新标题显示
            }
        });
        
        // 初始化时间数据
        mDate.setTimeInMillis(date);
        mDate.set(Calendar.SECOND, 0);  // 秒数归零
        mDateTimePicker.setCurrentDate(mDate.getTimeInMillis());
        
        // 设置对话框按钮
        setButton(context.getString(R.string.datetime_dialog_ok), this);       // 确认按钮
        setButton2(context.getString(R.string.datetime_dialog_cancel), null);  // 取消按钮
        
        // 配置时间显示格式
        set24HourView(DateFormat.is24HourFormat(context));
        updateTitle(mDate.getTimeInMillis());
    }

    // 配置方法 ========================================================
    /**
     * 设置24小时制显示模式
     * @param is24HourView true表示24小时制，false表示12小时制
     */
    public void set24HourView(boolean is24HourView) {
        mIs24HourView = is24HourView;
        mDateTimePicker.set24HourView(is24HourView);  // 同步到选择器控件
    }

    // 回调设置 ========================================================
    /**
     * 设置日期时间确定监听器
     * @param callBack 实现OnDateTimeSetListener接口的对象
     */
    public void setOnDateTimeSetListener(OnDateTimeSetListener callBack) {
        mOnDateTimeSetListener = callBack;
    }

    // 界面更新方法 =====================================================
    /**
     * 更新对话框标题显示时间
     * @param date 时间戳
     *
     * >>> 格式说明：
     * - 同时显示日期和时间（示例：2023年7月15日 14:30）
     * - 根据设置显示12/24小时制
     */
    private void updateTitle(long date) {
        int flag = DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME;
        flag |= mIs24HourView ? DateUtils.FORMAT_24HOUR : DateUtils.FORMAT_12HOUR;  // 修复原代码格式标志问题
        setTitle(DateUtils.formatDateTime(getContext(), date, flag));
    }

    // 事件处理 ========================================================
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mOnDateTimeSetListener != null) {
            mOnDateTimeSetListener.OnDateTimeSet(this, mDate.getTimeInMillis());
        }
    }
}
