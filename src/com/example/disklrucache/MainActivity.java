package com.example.disklrucache;

/*
 * DisLrucache需要网上下载DiskLrucache.java后添加到自己程序的包中
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MainActivity extends Activity implements OnClickListener {
	Button button;
	Button clearButton;
	TextView textView;
	ImageView imageView;
	DiskLruCache mDiskLruCache = null;
	HttpURLConnection urlConnection = null;
	BufferedOutputStream out = null;
	BufferedInputStream in = null;
	String imageUrl = "http://www.ituc.cn/uploads/allimg/140519/1-140519134UXX.jpg";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 标准的Open方法
		try {
			File cacheDir = getDiskCacheDir(this, "bitmap");
			if (!cacheDir.exists()) {
				cacheDir.mkdirs();
			}

			/*
			 * open第四个参数为最多可以缓存的字节数据
			 * 有了DiskLruCache的实例之后，我们就可以对缓存的数据进行操作了，操作类型主要包括写入、访问、移除等
			 */
			mDiskLruCache = DiskLruCache.open(cacheDir, getAppVersion(this), 1,
					10 * 1024 * 1024);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		initView();

	}

	private void initView() {
		// TODO Auto-generated method stub
		button = (Button) findViewById(R.id.btn);
		button.setOnClickListener(this);
		clearButton = (Button) findViewById(R.id.clearBtn);
		clearButton.setOnClickListener(this);
		imageView = (ImageView) findViewById(R.id.image);
		textView = (TextView)findViewById(R.id.cacheText);
		//显示缓存总字节数,以字节为单位
		textView.setText(Long.toString(mDiskLruCache.size()/1024));
	}

	/*
	 * 下载图片并缓存到Disk的线程
	 */
	public class CacheWriteToDisk implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				String key = hashKeyForDisk(imageUrl);
				/*
				 * DiskLruCache.Editor用来管理缓存数据写入、访问、移除等
				 */
				DiskLruCache.Editor editor = mDiskLruCache.edit(key);
				if (editor != null) {
					OutputStream outputStream = editor.newOutputStream(0);
					if (downloadUrlToStream(imageUrl, outputStream)) {
						editor.commit();
					} else {
						// 否则终止
						editor.abort();
					}
				}
				mDiskLruCache.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/*
	 * 获取缓存地址 当SD卡存在或者SD不可移除的时候,缓存路径是etExternalCacheDir() 例如:
	 * /sdcard/Android/data/<application package>/cache 否则缓存路径是getCacheDir() 例如：
	 * /data/data/<application package>/cache
	 */
	public File getDiskCacheDir(Context context, String uniqueName) {
		String cachePath;
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())
				|| !Environment.isExternalStorageRemovable()) {
			cachePath = context.getExternalCacheDir().getPath();
		} else {
			cachePath = context.getCacheDir().getPath();
		}

		// 返回值为拼接后路径的File对象
		return new File(cachePath + File.separator + uniqueName);
	}

	/*
	 * 获取当前程序版本号 当版本号改变的时候，需要清除缓存
	 */
	public int getAppVersion(Context context) {
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return 1;
	}

	/*
	 * 下载图片并缓存
	 * urlString:http://img.my.csdn.net/uploads/201309/01/1378037235_7476.jpg
	 */
	private boolean downloadUrlToStream(String urlString,
			OutputStream outputStream) {

		try {
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream(),
					8 * 1024);
			out = new BufferedOutputStream(outputStream, 8 * 1024);
			int b;
			while ((b = in.read()) != -1) {
				out.write(b);
			}
			Log.v("myu", "Dowload is true");
			return true;
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		Log.v("myu", "Dowload is false");
		return false;
	}

	/*
	 * 将字符串进行MD5编码1 用来将Key与图片的URL一一对应成MD5码作为KEY
	 */
	public String hashKeyForDisk(String key) {
		String cacheKey;
		try {
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(key.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}

	/*
	 * 将字符串进行MD5编码2 用来将Key与图片的URL一一对应成MD5码作为KEY
	 */
	private String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.btn:
			/*
			 * 读取缓存
			 */
				try {
					String key = hashKeyForDisk(imageUrl);
					DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
					if (snapShot != null) {
						Log.v("myu", "Image in Disk!!!");
						InputStream is = snapShot.getInputStream(0);
						Bitmap bitmap = BitmapFactory.decodeStream(is);
						// 获取缓存的bitmap后，就可以更新UI了
						imageView.setImageBitmap(bitmap);
					} else {
						Log.v("myu", "Image is Null!!!");
						// 如果Disk中没有缓存对应的图片，则开启子线程下载图片并缓存到Disk
						new Thread(new CacheWriteToDisk()).start();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			break;
		case R.id.clearBtn:
			try {
				mDiskLruCache.delete();
				Log.v("myu", "Cache is Delete!!!");
				textView.setText(Long.toString(mDiskLruCache.size()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;

		default:
			break;
		}
	}

}
