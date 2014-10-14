package com.benscholer.myomusic;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;


public class MyActivity extends Activity {

	//	public String lastCom;
	public boolean locked = false;
	public long lastPose = System.currentTimeMillis();
	public float originalRoll;
	public boolean fist = false;
	public boolean notFirstOrientationData = false;
	private AudioManager audio;

//	public TextView fistText = (TextView) findViewById(R.id.fist);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my);

		audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//		AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//
//		if (mAudioManager.isMusicActive()) {
//			lastCom = Shared.CMDPLAY;
//		} else {
//			lastCom = Shared.CMDPAUSE;
//		}

		// First, we initialize the Hub singleton with an application identifier.
		Hub hub = Hub.getInstance();
		if (!hub.init(this, getPackageName())) {
			// We can't do anything with the Myo device if the Hub can't be initialized, so exit.
			Toast.makeText(this, "Couldn't initialize Hub", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		Hub.getInstance().pairWithAnyMyo();

		// Next, register for DeviceListener callbacks.
		hub.addListener(mListener);

		Notification notification = new Notification.Builder(this)
				.setContentTitle("MyoMusic running")
				.setContentText("Click to exit.")
				.setSmallIcon(R.drawable.ic_launcher)
				.setOngoing(true)
				.build();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.my, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void musicController(String com) {
		AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		Intent i = new Intent(Shared.SERVICECMD);
		i.putExtra(Shared.CMDNAME, com);
		MyActivity.this.sendBroadcast(i);
	}

	private DeviceListener mListener = new AbstractDeviceListener() {

		private Arm mArm = Arm.UNKNOWN;
		private XDirection mXDirection = XDirection.UNKNOWN;

		// onConnect() is called whenever a Myo has been connected.
		@Override
		public void onConnect(Myo myo, long timestamp) {
			// Set the text color of the text view to cyan when a Myo connects.
			toaster("Myo Connected", Toast.LENGTH_SHORT);
		}

		// onDisconnect() is called whenever a Myo has been disconnected.
		@Override
		public void onDisconnect(Myo myo, long timestamp) {
			// Set the text color of the text view to red when a Myo disconnects.
			toaster("Myo Disconnected", Toast.LENGTH_SHORT);
		}

		// onArmRecognized() is called whenever Myo has recognized a setup gesture after someone has put it on their
		// arm. This lets Myo know which arm it's on and which way it's facing.
		@Override
		public void onArmRecognized(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
			mArm = arm;
			mXDirection = xDirection;
		}

		// onArmLost() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
		// it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
		// when Myo is moved around on the arm.
		@Override
		public void onArmLost(Myo myo, long timestamp) {
			mArm = Arm.UNKNOWN;
			mXDirection = XDirection.UNKNOWN;
		}

		// onOrientationData() is called whenever a Myo provides its current orientation,
		// represented as a quaternion.


		// onPose() is called whenever a Myo provides a new pose.
		@Override
		public void onPose(Myo myo, long timestamp, Pose pose) {
			if (pose.equals(Pose.FIST)) {
				fist = true;
			} else {
				fist = false;
				notFirstOrientationData = false;
			}
			if (System.currentTimeMillis() - lastPose > Shared.MAX_WAIT_TIME) {
				switch (pose) {
//					case THUMB_TO_PINKY:
//						locked = !locked;
//						if (locked) {
//							toaster("Myo locked!!", Toast.LENGTH_SHORT);
//							myo.vibrate(Myo.VibrationType.MEDIUM);
//
//						} else {
//							toaster("Myo unlocked!!", Toast.LENGTH_SHORT);
//							myo.vibrate(Myo.VibrationType.SHORT);
//						}
//						break;
					case FINGERS_SPREAD:
						if (!locked) {
							musicController(Shared.CMDTOGGLEPAUSE);
						}
						break;
					case WAVE_OUT:
						if (!locked) {
							musicController(Shared.CMDNEXT);
						}
						break;
					case WAVE_IN:
						if (!locked) {
							musicController(Shared.CMDPREVIOUS);
						}
						break;
				}
				lastPose = System.currentTimeMillis();
			}
		}

		@Override
		public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
			if (fist) {
				float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
				if (mXDirection == XDirection.TOWARD_ELBOW) {
					roll *= -1;
				}

				if (notFirstOrientationData) {
					originalRoll = roll;
					notFirstOrientationData = true;
				} else {
					float diff = roll - originalRoll;
					if (Math.abs(diff) >= Shared.ROLL_PER_VOLUME_LEVEL) {
						if (diff > 0) {
							volume(true);
							originalRoll = roll;
						} else {
							volume(false);
							originalRoll = roll;
						}
					}
				}
			}
		}
	};

	public void toaster(String str, int len) {
		Toast.makeText(this, str, len).show();
	}

	public void pause(View v) {
		musicController(Shared.CMDTOGGLEPAUSE);
	}

	public void next(View v) {
		musicController(Shared.CMDNEXT);
	}

	public void previous(View v) {
		musicController(Shared.CMDPREVIOUS);
	}

	public void volume(boolean up) {
		if (up) {
			audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
					AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
		} else {
			audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
					AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
		}
	}
}
