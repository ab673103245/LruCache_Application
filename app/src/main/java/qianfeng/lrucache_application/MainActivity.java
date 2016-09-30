package qianfeng.lrucache_application;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private ImageView iv;

    private LruCache<String, Bitmap> lruCache;

    private String urlImg = "http://img4.cache.netease.com/photo/0008/2016-09-18/C183QJ2B2ODN0008.550x.0.jpg";
    private String urlKey = "C183QJ2B2ODN0008.550x.0.jpg"; // 注意SD卡中的文件名不能含有'/',因为这是系统的文件路径分隔符

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

            Bitmap bitmap = (Bitmap) msg.obj;

            iv.setImageBitmap(bitmap);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iv = ((ImageView) findViewById(R.id.iv));

        long maxMemory = Runtime.getRuntime().maxMemory();
        int maxSize = (int) (maxMemory / 8); // 注意这里不要写死，要适用于不同的机型。一般是取1/8。
        lruCache = new LruCache<String, Bitmap>(maxSize);
    }

    public void downLoadImage(View view) {  // 点击下载图片

        // 先在内存中加载图片，如果内存中没有，再去SD卡中找，SD卡再找不到，就去网络中请求下载。
        // 如果SD卡找到，就把它存储进内存的LruCache中，再显示出来。在网络上下载好图片之后，先存储进SD卡中，再存储进内存中，再在内存中把它显示出来。
        Bitmap bitmap =  lruCache.get(urlKey);
        if (bitmap == null) {  // 如果内存中没有，那就去Sd卡中寻找

            bitmap = getBitmapFromSDCard(urlKey);
            if(bitmap == null)// 有可能图片不在SD卡中
            {
                downLoadImg();
            }else
            {
                lruCache.put(urlKey,bitmap); // 存储进内存中
                iv.setImageBitmap(bitmap);  // 并显示出来
            }
        } else {
            iv.setImageBitmap(bitmap);
        }

    }

    private void downLoadImg() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                HttpURLConnection con = null;
                try {
                    URL url = new URL(urlImg);
                    con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(5000);
                    con.connect();
                    if(con.getResponseCode() == 200)
                    {
                        Bitmap bitmap = BitmapFactory.decodeStream(con.getInputStream());

                        saveBitmap2SDCard(bitmap);  // 把网络图片存储进SD卡中

                        Message msg  = mHandler.obtainMessage();
                        msg.obj = bitmap;
                        mHandler.sendMessage(msg);
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    private void saveBitmap2SDCard(Bitmap bitmap) {

        // 把网络图片存储进SD卡中
        File externalCacheDir = this.getExternalCacheDir();

        try {
            FileOutputStream fos = new FileOutputStream(new File(externalCacheDir,urlKey).getAbsolutePath());
            if(urlKey.toLowerCase().endsWith(".png"))
            {

                bitmap.compress(Bitmap.CompressFormat.PNG,100,fos);
            }else
            {
                bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Bitmap getBitmapFromSDCard(String urlKey) {
        File externalCacheDir = this.getExternalCacheDir();

        // 如果SD卡有图片，则把它存储进内存中，并显示出来。
        return BitmapFactory.decodeFile(new File(externalCacheDir, urlKey).getAbsolutePath());

    }
}
