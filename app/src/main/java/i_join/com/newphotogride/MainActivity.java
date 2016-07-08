package i_join.com.newphotogride;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.GridView;

/**
 * 实现照片墙效果
 */
public class MainActivity extends AppCompatActivity {

    private GridView mPhotoWall;
    private PhotoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPhotoWall = (GridView) findViewById(R.id.photo_wall);
        adapter = new PhotoAdapter(this, 0, Images.imageThumbUrls, mPhotoWall);
        mPhotoWall.setAdapter(adapter);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 退出程序时结束所有的下载任务
        adapter.cancleTash();
    }
}
