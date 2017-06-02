package com.example.ocr;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.googlecode.tesseract.android.TessBaseAPI;

import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Filters.BradleyLocalThreshold;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	public static final String DATA_PATH = Environment
			.getExternalStorageDirectory().toString() + "/OCR/";
	private static final String TAG = "OCR";

	public static final String lang = "eng";
	Button cameraButton;
	Button galleryButton;
	TextView OCRText;
	protected String _path;
	protected boolean _taken;
	protected static final String PHOTO_TAKEN = "photo_taken";
	int PICK_FROM_GALLERY = 2;
	Bitmap photo;
	String imgDecodableString;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

		for (String path : paths) {
			File dir = new File(path);
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					Log.v(TAG, "ERROR: Creation of directory " + path
							+ " on sdcard failed");
					return;
				} else {
					Log.v(TAG, "Created directory " + path + " on sdcard");
				}
			}

		}

		if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata"))
				.exists()) {
			try {

				AssetManager assetManager = getAssets();
				InputStream in = assetManager.open("tessdata/" + lang
						+ ".traineddata");
				OutputStream out = new FileOutputStream(DATA_PATH + "tessdata/"
						+ lang + ".traineddata");

				// Transfer bytes from in to out
				byte[] buf = new byte[1024];
				int len;
				// while ((lenf = gin.read(buff)) > 0) {
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				// gin.close();
				out.close();

				Log.v(TAG, "Copied " + lang + " traineddata");
			} catch (IOException e) {
				Log.e(TAG,
						"Was unable to copy " + lang + " traineddata "
								+ e.toString());
			}
		}
		setContentView(R.layout.activity_main);
		cameraButton = (Button) findViewById(R.id.CameraButton);
		galleryButton = (Button) findViewById(R.id.GalleryButtonn);
		OCRText = (TextView) findViewById(R.id.OCRText);

		cameraButton.setOnClickListener(this);
		galleryButton.setOnClickListener(this);
		_path = DATA_PATH + "/ocr.jpg";
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.CameraButton:
			startCameraActivity();
			break;
		case R.id.GalleryButtonn:
			getImageFromGallery();
			break;
		default:
			break;
		}

	}

	private void getImageFromGallery() {

		Intent intent = new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

		startActivityForResult(intent, PICK_FROM_GALLERY);

	}

	protected void startCameraActivity() {
		File file = new File(_path);
		Uri outputFileUri = Uri.fromFile(file);

		final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
		startActivityForResult(intent, 0);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.i(TAG, "resultCode: " + resultCode);
		try {
			if (requestCode == PICK_FROM_GALLERY) {
				Uri selectedImage = data.getData();
				String[] filePathColumn = { MediaStore.Images.Media.DATA };

				// Get the cursor
				Cursor cursor = getContentResolver().query(selectedImage,
						filePathColumn, null, null, null);
				// Move to first row
				cursor.moveToFirst();

				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				imgDecodableString = cursor.getString(columnIndex);
				cursor.close();
				onPhotoTaken();

			} else if (resultCode == -1) {
				onPhotoTakenByCamera();
			} else {
				Log.v(TAG, "User cancelled");
			}
		} catch (Exception e) {

		}
	}

	private void onPhotoTakenByCamera() {
		try {

			_taken = true;
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 4;
			Bitmap bitmap = BitmapFactory.decodeFile(_path, options);

			FastBitmap fb = new FastBitmap(bitmap);

			BradleyLocalThreshold brad = new BradleyLocalThreshold();
			brad.applyInPlace(fb);

			// Display the result
			bitmap = fb.toBitmap();

			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

			Log.v(TAG, "Before baseApi");

			TessBaseAPI baseApi = new TessBaseAPI();
			baseApi.setDebug(true);
			baseApi.init(DATA_PATH, lang);
			baseApi.setImage(bitmap);

			String recognizedText = baseApi.getUTF8Text();

			baseApi.end();

			Log.v(TAG, "OCRED TEXT: " + recognizedText);

			if (recognizedText.length() != 0) {
				OCRText.setText(recognizedText);
			}
		} catch (Exception e) {
			cameraCrash();
		}

	}

	private void cameraCrash() {
		try {
			_taken = true;
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 4;

			Bitmap bitmap = BitmapFactory.decodeFile(_path, options);
			FastBitmap fb = new FastBitmap(bitmap);

			BradleyLocalThreshold brad = new BradleyLocalThreshold();
			brad.applyInPlace(fb);

			// Display the result
			bitmap = fb.toBitmap();

			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

			Log.v(TAG, "Before baseApi");

			TessBaseAPI baseApi = new TessBaseAPI();
			baseApi.setDebug(true);
			baseApi.init(DATA_PATH, lang);
			baseApi.setImage(bitmap);

			String recognizedText = baseApi.getUTF8Text();

			baseApi.end();

			Log.v(TAG, "OCRED TEXT: " + recognizedText);

			if (recognizedText.length() != 0) {
				OCRText.setText(recognizedText);
			}
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "Unable to Perfrom Action",
					Toast.LENGTH_SHORT).show();
		}

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(MainActivity.PHOTO_TAKEN, _taken);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.i(TAG, "onRestoreInstanceState()");
		if (savedInstanceState.getBoolean(MainActivity.PHOTO_TAKEN)) {
			onPhotoTaken();
		}
	}

	protected void onPhotoTaken() {
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 4;

			Bitmap bitmap = BitmapFactory.decodeFile(imgDecodableString,
					options);

			FastBitmap fb = new FastBitmap(bitmap);

			BradleyLocalThreshold brad = new BradleyLocalThreshold();
			brad.applyInPlace(fb);

			// Display the result
			bitmap = fb.toBitmap();
			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

			Log.v(TAG, "Before baseApi");

			TessBaseAPI baseApi = new TessBaseAPI();
			baseApi.setDebug(true);
			baseApi.init(DATA_PATH, lang);
			baseApi.setImage(bitmap);

			String recognizedText = baseApi.getUTF8Text();

			baseApi.end();

			Log.v(TAG, "OCRED TEXT: " + recognizedText);

			if (recognizedText.length() != 0) {
				OCRText.setText(recognizedText);
			}
		} catch (Exception e) {
			galleryCrash();
		}
	}

	private void galleryCrash() {
		try {

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 4;

			Bitmap bitmap = BitmapFactory.decodeFile(imgDecodableString,
					options);
			FastBitmap fb = new FastBitmap(bitmap);

			BradleyLocalThreshold brad = new BradleyLocalThreshold();
			brad.applyInPlace(fb);

			// Display the result
			bitmap = fb.toBitmap();
			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

			Log.v(TAG, "Before baseApi");

			TessBaseAPI baseApi = new TessBaseAPI();
			baseApi.setDebug(true);
			baseApi.init(DATA_PATH, lang);
			baseApi.setImage(bitmap);

			String recognizedText = baseApi.getUTF8Text();

			baseApi.end();

			Log.v(TAG, "OCRED TEXT: " + recognizedText);

			if (recognizedText.length() != 0) {
				OCRText.setText(recognizedText);
			}
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "Unable to Perfrom Action",
					Toast.LENGTH_SHORT).show();
		}

	}

}
