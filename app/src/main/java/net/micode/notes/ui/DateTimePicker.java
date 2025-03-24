/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License...（保留原有版权信息）
 */

package net.micode.notes.ui;

// 模块/库导入注释 ======================================================
import java.text.DateFormatSymbols;  // 日期格式符号获取
import java.util.Calendar;           // 日期时间操作

import net.micode.notes.R;          // 资源文件

import android.content.Context;      // 上下文环境
import android.text.format.DateFormat; // 日期格式化工具
import android.view.View;            // 视图基类
import android.widget.FrameLayout;   // 布局容器
import android.widget.NumberPicker;  // 数字选择器控件

// 类注释 =============================================================
/**
 * 自定义日期时间选择器组件
 * 功能：提供日期（周显示）和时间（时/分）的滚动选择界面
 * 特点：
 * 1. 支持12/24小时制切换
 * 2. 周视图日期显示（前三天 + 当天 + 后三天）
 * 3. 日期时间联动调整（跨日自动更新日期）
 * 4. AM/PM选择支持
 */
public class DateTimePicker extends FrameLayout {
    // 常量定义 =========================================================
    private static final boolean DEFAULT_ENABLE_STATE = true; // 默认启用状态
    private static final int HOURS_IN_HALF_DAY = 12;         // 半日小时数
    private static final int HOURS_IN_ALL_DAY = 24;          // 全日小时数
    private static final int DAYS_IN_ALL_WEEK = 7;            // 周显示天数
    // 各选择器数值范围
    private static final int DATE_SPINNER_MIN_VAL = 0;
    private static final int DATE_SPINNER_MAX_VAL = DAYS_IN_ALL_WEEK - 1;
    // ...（其他常量省略）

    // 控件实例 =========================================================
    private final NumberPicker mDateSpinner;   // 日期选择器（周显示）
    private final NumberPicker mHourSpinner;   // 小时选择器
    private final NumberPicker mMinuteSpinner; // 分钟选择器
    private final NumberPicker mAmPmSpinner;   // AM/PM选择器
    
    // 状态变量 =========================================================
    private Calendar mDate;                   // 当前选中日期时间
    private String[] mDateDisplayValues = new String[DAYS_IN_ALL_WEEK]; // 周日期显示文本
    private boolean mIsAm;                    // AM/PM状态
    private boolean mIs24HourView;            // 24小时制标志
    private boolean mIsEnabled;                // 启用状态
    private boolean mInitialising;             // 初始化状态标志

    // 事件监听接口 ======================================================
    public interface OnDateTimeChangedListener {
        void onDateTimeChanged(DateTimePicker view, int year, int month,
                int dayOfMonth, int hourOfDay, int minute);
    }

    // 构造方法 =========================================================
    /**
     * 初始化日期时间选择器
     * @param context 上下文环境
     * @param date 初始时间戳
     * @param is24HourView 是否24小时制
     * 
     * >>> 关键初始化流程：
     * 1. 初始化各选择器控件
     * 2. 设置初始时间值
     * 3. 配置时间格式显示
     * 4. 绑定值变化监听器
     */
    public DateTimePicker(Context context, long date, boolean is24HourView) {
        super(context);
        mDate = Calendar.getInstance();
        mInitialising = true;
        mIsAm = getCurrentHourOfDay() >= HOURS_IN_HALF_DAY;
        inflate(context, R.layout.datetime_picker, this);

        // 初始化日期选择器
        mDateSpinner = (NumberPicker) findViewById(R.id.date);
        mDateSpinner.setMinValue(DATE_SPINNER_MIN_VAL);
        mDateSpinner.setMaxValue(DATE_SPINNER_MAX_VAL);
        mDateSpinner.setOnValueChangedListener(mOnDateChangedListener);

        // 初始化时间选择器
        mHourSpinner = (NumberPicker) findViewById(R.id.hour);
        mHourSpinner.setOnValueChangedListener(mOnHourChangedListener);
        
        // ...（其他控件初始化省略）

        // 更新控件初始状态
        updateDateControl();
        updateHourControl();
        updateAmPmControl();
        set24HourView(is24HourView);
        setCurrentDate(date);
        setEnabled(isEnabled());
        mInitialising = false;
    }

    // 核心逻辑方法 ======================================================
    /**
     * 更新日期选择器显示
     * >>> 算法说明：
     * 1. 以当前选中日期为中心，显示前后三天的日期
     * 2. 格式化为"MM.dd EEEE"格式（如：07.15 星期一）
     * 3. 设置中间位置为当前选中日期
     */
    private void updateDateControl() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(mDate.getTimeInMillis());
        cal.add(Calendar.DAY_OF_YEAR, -DAYS_IN_ALL_WEEK / 2 - 1);
        
        for (int i = 0; i < DAYS_IN_ALL_WEEK; ++i) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            mDateDisplayValues[i] = (String) DateFormat.format("MM.dd EEEE", cal);
        }
        
        mDateSpinner.setDisplayedValues(mDateDisplayValues);
        mDateSpinner.setValue(DAYS_IN_ALL_WEEK / 2); // 中间位置为当前日期
    }

    /**
     * 小时选择值变化监听
     * 处理逻辑：
     * - 12小时制下AM/PM切换时自动调整日期
     * - 24小时制下跨日时间自动调整日期
     * - 更新内部Calendar对象并触发回调
     */
    private NumberPicker.OnValueChangeListener mOnHourChangedListener = 
        new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            // ...（处理日期边界逻辑）
            int newHour = mHourSpinner.getValue() % HOURS_IN_HALF_DAY + (mIsAm ? 0 : HOURS_IN_HALF_DAY);
            mDate.set(Calendar.HOUR_OF_DAY, newHour);
            onDateTimeChanged();
        }
    };

    // 其他重要方法 ======================================================
    /**
     * 设置24小时制显示模式
     * @param is24HourView true-24小时制 false-12小时制
     * 
     * >>> 切换逻辑：
     * 1. 隐藏/显示AM/PM选择器
     * 2. 调整小时选择器数值范围
     * 3. 转换当前小时显示格式
     */
    public void set24HourView(boolean is24HourView) {
        mAmPmSpinner.setVisibility(is24HourView ? View.GONE : View.VISIBLE);
        updateHourControl();
        setCurrentHour(getCurrentHourOfDay());
    }

    // 最佳实践提示 ======================================================
    /*
     * 注意事项：
     * 1. 日期显示范围仅限7天，不适合需要选择更广范围日期的场景
     * 2. 使用Calendar.add方法可能存在时区问题（建议使用UTC时间）
     * 3. 分钟选择器快速滚动间隔设为100ms可能需要调整（用户体验优化）
     * 
     * 优化建议：
     * 1. 使用DatePicker/TimePicker官方控件替代部分逻辑
     * 2. 添加时区选择支持
     * 3. 使用ViewBinding替代findViewById
     */

    // 边界条件处理 ======================================================
    /*
     * 已处理的重要边界情况：
     * 1. 12小时制下12点与0点的转换
     * 2. 日期跨月/跨年时的正确滚动
     * 3. 分钟选择器从59到0时的自动进位
     * 
     * 需注意的未处理情况：
     * 1. 夏令时调整可能导致的时间偏差
     * 2. 极端日期（如2月29日）的滚动处理
     * 3. 本地化格式兼容性（如不同语言周显示）
     */
}
