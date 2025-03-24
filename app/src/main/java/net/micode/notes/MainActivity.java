package net.micode.notes;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// 定义 MainActivity 类，继承自 AppCompatActivity，这是 Android 应用的主活动类
public class MainActivity extends AppCompatActivity {

    /**
     * 重写 onCreate 方法，该方法是 Activity 创建时调用的生命周期方法
     * @param savedInstanceState 保存 Activity 之前状态的 Bundle 对象，如果 Activity 是新创建的，此参数为 null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 调用父类的 onCreate 方法，确保父类的初始化逻辑正常执行
        super.onCreate(savedInstanceState);
        // 启用边缘到边缘的显示模式，使内容可以延伸到屏幕边缘
        EdgeToEdge.enable(this);
        // 设置当前 Activity 的布局文件为 activity_main.xml
        setContentView(R.layout.activity_main);

        // 为 ID 为 main 的视图设置窗口插入监听器，当窗口插入变化时会触发此监听器
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // 从 WindowInsetsCompat 对象中获取系统栏（如状态栏、导航栏）的插入信息
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // 根据系统栏的插入信息设置视图的内边距，避免内容被系统栏遮挡
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            // 返回处理后的 WindowInsetsCompat 对象
            return insets;
        });
    }
}
