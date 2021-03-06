package genius.mohammad.accelerometer.mouse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener, OnTouchListener {

	private SensorManager mSensorManager;
	private Sensor mSensor;
	private GraphicsView graphicsView;
	private AccelerometerMouseClient client;
	public static MouseButton leftButton = new MouseButton(), rightButton = new MouseButton(), middleButton = new MouseButton(), scrollWheel = new MouseButton();
	private boolean vibrate = true;
	public static int dBm = -200;
	private final static double compression = 0.75;
	public static WifiManager wifi;
	private boolean vibrateDown = false, vibrateUp = false, defaultServer = false, showHelp = false, invertX = false, invertY = false, deadzone = false, tablet = false, portrait = false, changingOr = false, justChangedOr = false, gammaUpdate = false;
	private int vibrateDownDuration = 20, vibrateUpDuration = 20, defaultServerPort = 18250, number = 1280, zero = 22;
	private String defaultServerIp = "shit";
	protected PowerManager.WakeLock mWakeLock;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		updatePrefs();
		graphicsView = new GraphicsView(this);
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		setContentView(graphicsView);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		graphicsView.setOnTouchListener(this);
		if (showHelp || !gammaUpdate) {
			// Show help on startup only once! This keeps track
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean("release", true);
			if (!gammaUpdate)
				editor.putBoolean("showHelp", false);
			editor.commit();
			// Show help dialog
			Intent i = new Intent(this, HelpActivity.class);
			startActivity(i);
		}
		client = new AccelerometerMouseClient("Bullshit", 18250);
		dBm = getSignalStrength();
		if (wifi.isWifiEnabled() && !AccelerometerMouseClient.running && !justChangedOr) {
			if (!defaultServer) {
				showToast("Searching for servers on the local network...", Toast.LENGTH_SHORT);
				client.run(false);
			} else {
				if (defaultServerIp.equals("shit")) {
					showToast("Failed to connect to default server. None defined.", Toast.LENGTH_SHORT);
				} else {
					showToast("Attempting to connect to " + defaultServerIp + "...", Toast.LENGTH_SHORT);
					client.forceUpdate(defaultServerIp, defaultServerPort);
					client.run(true);
				}
			}
		}
		// Just changed orientation?
		if (justChangedOr) {
			try {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
				String socket = prefs.getString("lastServer", "");
				String ip = socket.split(":")[0];
				String port = socket.split(":")[1];
				client.forceUpdate(ip, Integer.parseInt(port));
				client.run(true);
			} catch (Exception e) {
				client.run(false);
			}
		}
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AccelMouseLock");
		this.mWakeLock.acquire();
	}

	private void updatePrefs() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		try {
			showHelp = prefs.getBoolean("showHelp", true);
			vibrateDown = prefs.getBoolean("vibrateDown", true);
			vibrateUp = prefs.getBoolean("vibrateUp", true);
			defaultServer = prefs.getBoolean("defaultServer", false);
			invertX = prefs.getBoolean("invertX", false);
			invertY = prefs.getBoolean("invertY", false);
			deadzone = prefs.getBoolean("deadzone", false);
			portrait = prefs.getBoolean("portrait", false);
			tablet = prefs.getBoolean("tablet", false);
			justChangedOr = prefs.getBoolean("justChangedOr", false);
			gammaUpdate = prefs.getBoolean("release", false);
			defaultServerIp = prefs.getString("serverIP", "shit");
			zero = Integer.parseInt(prefs.getString("zeroPosition", "22"));
			vibrateDownDuration = Integer.parseInt(prefs.getString("vibrationDownIntensity", "30").replaceAll("ms", ""));
			vibrateUpDuration = Integer.parseInt(prefs.getString("vibrationUpIntensity", "20").replaceAll("ms", ""));
			defaultServerPort = Integer.parseInt(prefs.getString("serverPort", "18250"));
		} catch (Exception e) {
			showToast("Error occrued while reading preferences. Please check that they are correct!", Toast.LENGTH_SHORT);
		}
	}

	public int getSignalStrength() {
		try {
			WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			if (!wifiManager.isWifiEnabled()) {
				showToast("No Wi-Fi connection detected!", Toast.LENGTH_SHORT);
			}
			return wifiManager.getConnectionInfo().getRssi();
		} catch (Exception e) {

		}
		return -50;
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Could care less
	}

	@Override
	protected void onResume() {
		super.onResume();
		leftButton.setState(false);
		rightButton.setState(false);
		middleButton.setState(false);
		scrollWheel.setState(false);
		updatePrefs();
		graphicsView.resume();
		if (portrait) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			graphicsView.orientation = 1;
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			graphicsView.orientation = 2;
		}
		client.pause(false);
		Log.d("Orientation", "" + this.getResources().getConfiguration().orientation);
		mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
		if (ManualConnectActivity.configured) {
			client.stop();
			client.forceUpdate(ManualConnectActivity.ipAddress, ManualConnectActivity.port);
			showToast("Attempting to connect to " + ManualConnectActivity.ipAddress + " on port " + ManualConnectActivity.port, Toast.LENGTH_LONG);
			client.run(true);
			ManualConnectActivity.configured = false;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this, mSensor);
		client.pause(true);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.mWakeLock.release();
		client.stop();
		graphicsView.stop();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("justChangedOr", changingOr);
		if (!changingOr)
			editor.putString("lastServer", "");
		editor.commit();
	}

	public void onSensorChanged(SensorEvent event) {
		try {
			float x = invertX ? (-event.values[0] + 9.81f) : event.values[0];
			float y = invertY ? (-event.values[1]) : event.values[1];
			float z = event.values[2];
			// Screen flipping and whatnot
			{
				if (tablet) {
					float dummyX = x;
					x = y;
					y = -dummyX;
				}
				if (graphicsView.orientation == 1) {
					float dummyX = x;
					x = y;
					y = -dummyX;
				}

				switch (zero) {
				case 0:
					x += 4.9;
					break;
				case 22:
					x += 4.9 / 2;
					break;
				}
			}
			if (deadzone) {
				x = applyDeadZoneX(x);
				y = applyDeadZoneY(y);
			}
			client.feedAccelerometerValues(x, y, z);
			// Show connected toast if connected
			if (!AccelerometerMouseClient.toastShown) {
				if (AccelerometerMouseClient.connected) {
					showToast("Connected to server at " + client.socket.getInetAddress().getHostAddress(), Toast.LENGTH_LONG);
				} else {
					showToast("Lost connection to server!", Toast.LENGTH_LONG);
				}
				AccelerometerMouseClient.toastShown = true;
			}
		} catch (Exception e) {

		}
	}

	public float applyDeadZoneX(float x) {
		if (x < 5.8 && x > 3.2) {
			x = 4.90f;
		}
		return x;
	}

	public float applyDeadZoneY(float y) {
		if (y > -.98 && y < .98) {
			y = 0f;
		}
		return y;
	}

	public boolean onTouch(View v, MotionEvent event) {
		Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		if (graphicsView.orientation == 2)
			number = 1280;
		else
			number = 800;
		if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == 261 || event.getAction() == 5) {
			if (!middleButton.getState()) {
				if (isLeft(event.getX(getPID(event)), event.getY(getPID(event)))) {
					leftButton.setState(true);
					leftButton.setPID(getPID(event));
					if (vibrate && vibrateDown)
						vibrator.vibrate(vibrateDownDuration);
				} else if (isRight(event.getX(getPID(event)), event.getY(getPID(event)))) {
					rightButton.setState(true);
					rightButton.setPID(getPID(event));
					if (vibrate && vibrateDown)
						vibrator.vibrate(vibrateDownDuration);
				} else if (isMiddle(event)) {
					if (event.getPointerCount() > 1 || graphicsView.orientation == 1) {
						scrollWheel.setState(true);
					} else {
						middleButton.setState(true);
					}
					if (vibrate && vibrateDown)
						vibrator.vibrate(vibrateDownDuration);
				}
			}
			if (event.getX(getPID(event)) > getWindowManager().getDefaultDisplay().getWidth() - (int) (getWindowManager().getDefaultDisplay().getWidth() * 88 / number + 0.5) && event.getY(getPID(event)) > getWindowManager().getDefaultDisplay().getHeight() - (int) (getWindowManager().getDefaultDisplay().getWidth() * 88 / number + 0.5)) {
				if (vibrate && vibrateDown)
					vibrator.vibrate(vibrateDownDuration);
				if (wifi.isWifiEnabled())
					showToast("Wi-Fi signal strength: " + getSignalStrength() + "dBm", Toast.LENGTH_SHORT);
				else
					getSignalStrength();
			} else if (event.getX(getPID(event)) < (getWindowManager().getDefaultDisplay().getWidth() / 60) + 40 && event.getY(getPID(event)) < (getWindowManager().getDefaultDisplay().getHeight() / 60) + 40) {
				if (AccelerometerMouseClient.connected) {
					showToast("Connected to server at " + client.socket.getInetAddress().getHostAddress(), Toast.LENGTH_LONG);
				} else {
					showToast("Not connected to a server. Please try searching for one by clicking \"Search for Server\" in the menu, or manually connect to the server by clicking \"Manually Connect\".", Toast.LENGTH_LONG);
				}
				if (vibrate && vibrateDown)
					vibrator.vibrate(vibrateDownDuration);
			} else if (event.getX(getPID(event)) > getWindowManager().getDefaultDisplay().getWidth() - (int) (getWindowManager().getDefaultDisplay().getWidth() * 88 / number + 0.5) && event.getY(getPID(event)) < (int) (getWindowManager().getDefaultDisplay().getWidth() * 88 / number + 0.5)) {

				if (vibrate && vibrateDown)
					vibrator.vibrate(vibrateDownDuration);
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
				SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean("portrait", graphicsView.orientation == 2);
				try {
					if (AccelerometerMouseClient.connected) {
						editor.putString("lastServer", client.socket.getInetAddress().getHostAddress() + ":" + client.port);
						client.sendJamSignal();
						changingOr = true;
					}
				} catch (Exception e) {
					// You never know...
				}
				editor.commit();
				if (graphicsView.orientation == 2) {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
					graphicsView.orientation = 1;
				} else {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
					graphicsView.orientation = 2;
				}
			} else if (event.getX(getPID(event)) < (int) (getWindowManager().getDefaultDisplay().getWidth() * 88 / number + 0.5) && event.getY(getPID(event)) > getWindowManager().getDefaultDisplay().getHeight() - (int) (getWindowManager().getDefaultDisplay().getWidth() * 88 / number + 0.5)) {
				if (AccelerometerMouseClient.connected) {
					if (vibrate && vibrateDown)
						vibrator.vibrate(vibrateDownDuration);
					client.pause(!AccelerometerMouseClient.paused);
				}
			}
		} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == 262 || event.getAction() == 6) {
			if (event.getPointerCount() > 1) {
				if (leftButton.getState() && event.getX(getPID(event)) < getWindowManager().getDefaultDisplay().getWidth() / 2) {
					leftButton.setState(false);
					if (vibrate && vibrateUp)
						vibrator.vibrate(vibrateUpDuration);
				} else if (rightButton.getState() && event.getX(getPID(event)) > getWindowManager().getDefaultDisplay().getWidth() / 2) {
					rightButton.setState(false);
					if (vibrate && vibrateUp)
						vibrator.vibrate(vibrateUpDuration);
				} else if (scrollWheel.getState() && isMiddle(event)) {
					scrollWheel.setState(false);
					if (vibrate && vibrateUp)
						vibrator.vibrate(vibrateUpDuration);
				}
			} else {
				if (leftButton.getState() || rightButton.getState() || scrollWheel.getState())
					if (vibrate && vibrateUp)
						vibrator.vibrate(vibrateUpDuration);
				leftButton.setState(false);
				rightButton.setState(false);
				scrollWheel.setState(false);
				if (middleButton.getState()) {
					if (vibrate && vibrateUp)
						vibrator.vibrate(vibrateUpDuration);
				}
				middleButton.setState(false);
			}
		}
		client.feedTouchFlags(leftButton.getState(), rightButton.getState(), middleButton.getState(), scrollWheel.getState());
		Log.d("Touch", "action=" + event.getAction() + ", left flag=" + leftButton.getState() + ", right flag=" + rightButton.getState() + ", middle flag=" + scrollWheel.getState() + ", pointers=" + event.getPointerCount());
		return true;
	}

	private boolean isLeft(float x, float y) {
		if (graphicsView.orientation == 2)
			return (x < (getWindowManager().getDefaultDisplay().getWidth() / 2) - (getWindowManager().getDefaultDisplay().getWidth() / 16) && x > getWindowManager().getDefaultDisplay().getWidth() * 0.2125) && (y > getWindowManager().getDefaultDisplay().getHeight() * 0.2);
		else
			return (x < (getWindowManager().getDefaultDisplay().getWidth() / 2) - (getWindowManager().getDefaultDisplay().getWidth() / 16) && (y > getWindowManager().getDefaultDisplay().getHeight() * 0.475)) && !(x < (int) (getWindowManager().getDefaultDisplay().getWidth() * 88 / number + 0.5) && y > getWindowManager().getDefaultDisplay().getHeight() - (int) (getWindowManager().getDefaultDisplay().getWidth() * 88 / number + 0.5));
	}

	private boolean isRight(float x, float y) {
		if (graphicsView.orientation == 2)
			return (x > (getWindowManager().getDefaultDisplay().getWidth() / 2) + (getWindowManager().getDefaultDisplay().getWidth() / 16) && x < getWindowManager().getDefaultDisplay().getWidth() * 0.7875) && (y > getWindowManager().getDefaultDisplay().getHeight() * 0.2);
		else
			return (x > (getWindowManager().getDefaultDisplay().getWidth() / 2) + (getWindowManager().getDefaultDisplay().getWidth() / 16) && (y > getWindowManager().getDefaultDisplay().getHeight() * 0.475)) && !(x > getWindowManager().getDefaultDisplay().getWidth() - (int) (getWindowManager().getDefaultDisplay().getWidth() * 88 / number + 0.5) && y > getWindowManager().getDefaultDisplay().getHeight() - (int) (getWindowManager().getDefaultDisplay().getWidth() * 88 / number + 0.5));
	}

	private boolean isMiddle(MotionEvent event) {
		if (event.getPointerCount() > 1 && graphicsView.orientation == 2) {
			boolean sentinel = true;
			int falseCounts = 0;
			for (int i = 0; i < event.getPointerCount(); i++) {
				if (event.getX(i) < (getWindowManager().getDefaultDisplay().getWidth() / 2) + (getWindowManager().getDefaultDisplay().getWidth() / 16) && event.getX(i) > (getWindowManager().getDefaultDisplay().getWidth() / 2) - (getWindowManager().getDefaultDisplay().getWidth() / 16)) {
					falseCounts++;
					sentinel = sentinel && false;
				}
			}
			if (event.getX(getPID(event)) < (getWindowManager().getDefaultDisplay().getWidth() / 2) + (getWindowManager().getDefaultDisplay().getWidth() / 16) && event.getX(getPID(event)) > (getWindowManager().getDefaultDisplay().getWidth() / 2) - (getWindowManager().getDefaultDisplay().getWidth() / 16)) {
				sentinel = true;
			}
			if (falseCounts > 1) {
				sentinel = false;
			}
			return !(leftButton.getState() || rightButton.getState()) && sentinel;
		} else {
			if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == 261 || event.getAction() == 5) {
				if (graphicsView.orientation == 1)
					return !(leftButton.getState() || rightButton.getState()) && event.getX(getPID(event)) < (getWindowManager().getDefaultDisplay().getWidth() / 2) + (getWindowManager().getDefaultDisplay().getWidth() / 16) && event.getX(getPID(event)) > (getWindowManager().getDefaultDisplay().getWidth() / 2) - (getWindowManager().getDefaultDisplay().getWidth() / 16) && event.getY(getPID(event)) > getWindowManager().getDefaultDisplay().getHeight() * 0.6 && event.getY(getPID(event)) < getWindowManager().getDefaultDisplay().getHeight() * 0.8;
				else
					return !(leftButton.getState() || rightButton.getState()) && event.getX(getPID(event)) < (getWindowManager().getDefaultDisplay().getWidth() / 2) + (getWindowManager().getDefaultDisplay().getWidth() / 16) && event.getX(getPID(event)) > (getWindowManager().getDefaultDisplay().getWidth() / 2) - (getWindowManager().getDefaultDisplay().getWidth() / 16) && event.getY(getPID(event)) > getWindowManager().getDefaultDisplay().getHeight() * 0.4 && event.getY(getPID(event)) < getWindowManager().getDefaultDisplay().getHeight() * 0.7;

			} else {
				return true;
			}
		}
	}

	private int getPID(MotionEvent event) {
		int action = event.getAction();
		return action >> MotionEvent.ACTION_POINTER_ID_SHIFT;
	}

	private void showToast(String string, int duration) {
		Toast toast = Toast.makeText(this, string, duration);
		toast.setGravity(Gravity.TOP | Gravity.RIGHT, 0, 0);
		toast.show();
	}

	/**
	 * 
	 * Menu
	 * 
	 */

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "Auto Connect"); // Index 0
		menu.add(0, 1, 0, "Manually Connect"); // Index 1
		menu.add(0, 2, 0, "Disconnect"); // Index 2
		menu.add(0, 3, 0, "Preferences"); // Index 3
		Log.i("Menu", "Menu shown");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d("Menu", "Selected item: " + item.getItemId());
		switch (item.getItemId()) {
		case 0:
			client.stop();
			client = null;
			client = new AccelerometerMouseClient("Bullshit", 18250);
			if (wifi.isWifiEnabled()) {
				client.run(false);
				showToast("Searching for servers on the local network...", Toast.LENGTH_SHORT);
			} else {
				showToast("No Wi-Fi connection detected! Search aborted.", Toast.LENGTH_SHORT);
			}
			break;
		case 1:
			Intent i = new Intent(this, ManualConnectActivity.class);
			startActivity(i);
			break;
		case 2:
			client.stop();
			showToast("Disconnected from server.", Toast.LENGTH_SHORT);
			break;
		case 3:
			Intent i2 = new Intent(this, Preferences.class);
			startActivity(i2);
			break;
		}
		return true;
	}

	/**
	 * 
	 * GraphicsView
	 * 
	 */

	static public class GraphicsView extends SurfaceView implements Runnable {

		Thread t;
		SurfaceHolder holder;
		boolean loop = false;
		int orientation = 1;
		Bitmap mouseNone, mouseAll, mouseLeft, mouseRight, mouseMiddle, mouseScroll, orientationSwitch, bar0, bar1, bar2, bar3, bar4, bar5, paused, playing, fluff;

		public GraphicsView(Context context) {
			super(context);
			holder = getHolder();
		}

		public void run() {
			while (loop) {
				if (!holder.getSurface().isValid()) {
					continue;
				}
				try {
					Canvas canvas = holder.lockCanvas();
					if (mouseNone == null) {
						Options options = new BitmapFactory.Options();
						options.inScaled = false;
						options.inDither = false;
						options.inPreferredConfig = Bitmap.Config.ARGB_8888;
						if (orientation == 2) {
							mouseNone = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.none, options), (int) (canvas.getWidth() * compression), (int) (canvas.getHeight() * compression), true);
							mouseAll = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.all, options), (int) (canvas.getWidth() * compression), (int) (canvas.getHeight() * compression), true);
							mouseMiddle = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.middle, options), (int) (canvas.getWidth() * compression), (int) (canvas.getHeight() * compression), true);
							mouseScroll = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.scroll, options), (int) (canvas.getWidth() * compression), (int) (canvas.getHeight() * compression), true);
							mouseLeft = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.left, options), (int) (canvas.getWidth() * compression), (int) (canvas.getHeight() * compression), true);
							mouseRight = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.right, options), (int) (canvas.getWidth() * compression), (int) (canvas.getHeight() * compression), true);
						} else {
							mouseNone = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.noneportrait, options), (int) (canvas.getWidth() * compression), (int) (canvas.getHeight() * compression), true);
							mouseAll = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.allportrait, options), (int) (canvas.getWidth() * compression), (int) (canvas.getHeight() * compression), true);
							mouseMiddle = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.middleportrait, options), (int) (canvas.getWidth() * compression), (int) (canvas.getHeight() * compression), true);
							mouseScroll = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.scrollportrait, options), (int) (canvas.getWidth() * compression), (int) (canvas.getHeight() * compression), true);
							mouseLeft = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.leftportrait, options), (int) (canvas.getWidth() * compression), (int) (canvas.getHeight() * compression), true);
							mouseRight = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.rightportrait, options), (int) (canvas.getWidth() * compression), (int) (canvas.getHeight() * compression), true);
						}
						bar0 = BitmapFactory.decodeResource(getResources(), R.drawable.bar0);
						bar1 = BitmapFactory.decodeResource(getResources(), R.drawable.bar1);
						bar2 = BitmapFactory.decodeResource(getResources(), R.drawable.bar2);
						bar3 = BitmapFactory.decodeResource(getResources(), R.drawable.bar3);
						bar4 = BitmapFactory.decodeResource(getResources(), R.drawable.bar4);
						bar5 = BitmapFactory.decodeResource(getResources(), R.drawable.bar5);
						orientationSwitch = BitmapFactory.decodeResource(getResources(), R.drawable.orientation);
						paused = BitmapFactory.decodeResource(getResources(), R.drawable.pause);
						playing = BitmapFactory.decodeResource(getResources(), R.drawable.play);
						fluff = BitmapFactory.decodeResource(getResources(), R.drawable.fluff);
					}
					Paint paint = new Paint();
					paint.setAntiAlias(true);
					paint.setFilterBitmap(true);
					paint.setDither(true);
					Bitmap bitmap = mouseNone;
					if (leftButton.getState() && !rightButton.getState()) {
						bitmap = mouseLeft;
					}
					if (!leftButton.getState() && rightButton.getState()) {
						bitmap = mouseRight;
					}
					if (leftButton.getState() && rightButton.getState()) {
						bitmap = mouseAll;
					}
					if (middleButton.getState()) {
						bitmap = mouseMiddle;
					}
					if (scrollWheel.getState()) {
						bitmap = mouseScroll;
					}
					canvas.drawBitmap(bitmap, null, new RectF(0, 0, canvas.getWidth(), canvas.getHeight()), paint);
					// Draw a circle for connectivity
					if (!AccelerometerMouseClient.connected) {
						paint.setColor(Color.argb(200, 255, 0, 0));
					} else {
						paint.setColor(Color.argb(200, 0, 255, 0));
					}
					int diameter = (orientation == 2) ? canvas.getWidth() / 60 : canvas.getHeight() / 60;
					canvas.drawOval(new RectF(10, 10, 10 + diameter, 10 + diameter), paint);
					// Signal bars
					if (dBm > -100) {
						bitmap = bar1;
					}
					if (dBm > -80) {
						bitmap = bar2;
					}
					if (dBm > -70) {
						bitmap = bar3;
					}
					if (dBm > -50) {
						bitmap = bar4;
					}
					if (dBm > -30) {
						bitmap = bar5;
					}
					if (dBm < -100) {
						bitmap = bar0;
					}

					int number = (orientation == 2) ? 1280 : 800;
					int radius = (int) (canvas.getWidth() * 160 / number + 0.5);
					Paint paintFluff = new Paint();
					// Draw some fluff on the bottom left/right corners
					canvas.drawBitmap(fluff, null, new RectF(canvas.getWidth() - radius, canvas.getHeight() - radius, canvas.getWidth() + radius, canvas.getHeight() + radius), paintFluff);
					// Draw bars
					canvas.drawBitmap(bitmap, null, new RectF(canvas.getWidth() - (int) (canvas.getWidth() * 68 / number + 0.5), canvas.getHeight() - (int) (canvas.getWidth() * 68 / number + 0.5), canvas.getWidth() - 10, canvas.getHeight() - 10), paint);
					// Draw orientation switch
					canvas.drawBitmap(orientationSwitch, null, new RectF(canvas.getWidth() - (int) (canvas.getWidth() * 68 / number + 0.5), 10, canvas.getWidth() - 10, (int) (canvas.getWidth() * 68 / number + 0.5)), paint);
					// Draw pause/play icon
					if (AccelerometerMouseClient.connected) {
						// Draw fluff
						canvas.drawBitmap(fluff, null, new RectF(-radius, canvas.getHeight() - radius, radius, canvas.getHeight() + radius), paintFluff);
						if (!AccelerometerMouseClient.paused) {
							canvas.drawBitmap(paused, null, new RectF(10, canvas.getHeight() - (int) (canvas.getWidth() * 68 / number + 0.5), (int) (canvas.getWidth() * 68 / number + 0.5), canvas.getHeight() - 10), paint);
						} else {
							canvas.drawBitmap(playing, null, new RectF(10, canvas.getHeight() - (int) (canvas.getWidth() * 68 / number + 0.5), (int) (canvas.getWidth() * 68 / number + 0.5), canvas.getHeight() - 10), paint);
						}
					}
					holder.unlockCanvasAndPost(canvas);
				} catch (Exception e) {

				}
			}
		}

		public void stop() {
			loop = false;
		}

		public void resume() {
			loop = true;
			t = new Thread(this);
			t.start();
		}
	}
}