package i_join.com.newphotogride;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * 先下载图片在做缓存处理
 * Created by admin on 2016/7/8.
 */
public class PhotoAdapter extends ArrayAdapter<String> implements AbsListView.OnScrollListener {
    /**
     * 记录所有正在下载或等待下载的任务
     */
    private Set<BitmapWorkerTask> taskCollection;

    /**
     * 图片缓存的核心类
     */
    private LruCache<String, Bitmap> mMemoryCache;

    /**
     * GridleView 的实例
     */
    private GridView mPhoto;

    /**
     * 程序第一张图片
     */
    private int firstImage;
    /**
     * 程序总共加载的图片
     */
    private int verticalImage;

    public PhotoAdapter(Context context, int textViewResourceId, String[] objects, GridView photoWall) {
        super(context, textViewResourceId, objects);
        mPhoto = photoWall;
        taskCollection = new HashSet<BitmapWorkerTask>();
        //获取应用程序可用的最大内存
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;//图片的缓存大小为程序最大内存的8分之1
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();//每张图片的大小
            }
        };
        //设置滑动监听
        mPhoto.setOnScrollListener(this);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final String url = getItem(position);
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.photoview, null);
        } else {
            view = convertView;
        }
        final ImageView photo = (ImageView) view.findViewById(R.id.photo_View);
        photo.setTag(url);
        setImageView(url, photo);
        return view;
    }

    private void setImageView(String url, ImageView view) {
        Bitmap bitmap = getBitmapFromLrcCache(url);
        /**
         * if (bitmap != null) {
         imageView.setImageBitmap(bitmap);
         } else {
         imageView.setImageResource(R.drawable.empty_photo);
         }
         * */
        if(bitmap != null) {
            view.setImageBitmap(bitmap);
        }else{
            view.setImageResource(R.drawable.empty_photo);
        }
    }

    //缓存图片,将图片存储到LrcCache中
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromLrcCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    //判断从LrcCache中提取一张图片
    public Bitmap getBitmapFromLrcCache(String key) {

        return mMemoryCache.get(key);
    }


    //滑动状态监听事件
    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        if (i == SCROLL_STATE_IDLE) {//不滑动时，加载图片
            loadBitmaps(firstImage, verticalImage);
        } else {//取消加载
            cancleTash();
        }
    }


    @Override
    public void onScroll(AbsListView absListView, int i, int i1, int i2) {
        firstImage = i;
        verticalImage = i1;
        if (i1 > 0) {//首次进入程序，调用下载任务
            loadBitmaps(firstImage, verticalImage);
        }
    }

    //取消加载方法
    public void cancleTash() {
        if (taskCollection != null) {
            for (BitmapWorkerTask task : taskCollection) {
                task.cancel(false);
            }
        }
    }

    //编写当屏幕滚动的时候，加载图片的事件
    public void loadBitmaps(int i0, int i2) {
        for (int i = i0; i < i0 + i2; i++) {
            String imageurl = Images.imageThumbUrls[i];
            Bitmap bitmap = getBitmapFromLrcCache(imageurl);
            if (bitmap == null) {//如果bitmap没有图片，就进行异步加载
                BitmapWorkerTask task = new BitmapWorkerTask();
                taskCollection.add(task);
                task.execute(imageurl);
            } else {//当有图片时
                ImageView imageView = (ImageView) mPhoto.findViewWithTag(imageurl);
                if (imageView != null && bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    /**
     * 异步下载图片任务
     * Created by admin on 2016/7/8.
     */
    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

        /**
         * 图片的URL地址
         */
        private String imageUrl;

        @Override
        protected Bitmap doInBackground(String... params) {
            imageUrl = params[0];
            // 在后台开始下载图片
            Bitmap bitmap = downloadBitmap(params[0]);
            if (bitmap != null) {
                // 图片下载完成后缓存到LrcCache中
                addBitmapToMemoryCache(params[0], bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            // 根据Tag找到相应的ImageView控件，将下载好的图片显示出来。
            ImageView imageView = (ImageView) mPhoto.findViewWithTag(imageUrl);
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
            taskCollection.remove(this);
        }

        /**
         * 建立HTTP请求，并获取Bitmap对象。
         *
         * @param imageUrl
         *            图片的URL地址
         * @return 解析后的Bitmap对象
         */
        private Bitmap downloadBitmap(String imageUrl) {
            Bitmap bitmap = null;
            HttpURLConnection con = null;
            try {
                URL url = new URL(imageUrl);
                con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5 * 1000);
                con.setReadTimeout(10 * 1000);
                con.setDoInput(true);
                con.setDoOutput(true);
                bitmap = BitmapFactory.decodeStream(con.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
            return bitmap;
        }

    }

}
