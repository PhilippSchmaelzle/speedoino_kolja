
package de.windeler.kolja;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

public class SpeedoAndroidActivity extends TabActivity implements OnClickListener {
	// Name of the connected device
	private static final String TAG = "JKW";

	private MenuItem mMenuItemConnect;
	private BluetoothAdapter mBluetoothAdapter = null;
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	public static BluetoothSerialService mSerialService = null;
	private Toast toast;
	private Handler mTimerHandle = new Handler();
	private ProgressDialog _progressDialog;
	private getDialog _getDialog;


	/**
	 * Our main view. Displays the emulated terminal screen.
	 */
	private TextView mLog;
	private TextView mStatus;
	private TextView mVersion;
	private TextView mDownload;
	private TextView mUpload;
	private Button mCheckVersion;
	private Button mLeftButton;
	private Button mRightButton;
	private Button mUpButton;
	private Button mDownButton;
	private Button browseToUploadMap;
	private Button browseToUploadConfig;
	private Button browseToUploadSpeedo;
	private Button browseToUploadGfx;
	private Button mloadRoot;
	private Button DlselButton;
	private ListView mDLListView;



	// Message types sent from the BluetoothReadService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int MESSAGE_CMD_OK = 6;
	public static final int MESSAGE_CMD_FAILED = 7;
	public static final int MESSAGE_CMD_UNKNOWN = 8;
	public static final int MESSAGE_SET_VERSION = 9;
	public static final int MESSAGE_SET_LOG = 10;
	public static final int MESSAGE_DIR_APPEND = 11;



	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT 		= 2;
	private static final int REQUEST_OPEN_MAP		= 3;	// file open dialog
	private static final int REQUEST_OPEN_CONFIG	= 4;	// file open dialog
	private static final int REQUEST_OPEN_SPEEDO	= 5;	// file open dialog
	private static final int REQUEST_OPEN_GFX		= 6;	// file open dialog



	// transfer messages
	public static final byte CMD_SIGN_ON		=  0x01;
	public static final byte CMD_LEAVE_FM		=  0x04;
	public static final byte CMD_GO_LEFT		=  0x05;
	public static final byte CMD_GO_RIGHT		=  0x06;
	public static final byte CMD_GO_UP			=  0x07;
	public static final byte CMD_GO_DOWN		=  0x08;
	public static final byte CMD_FILE_RECEIVE	=  0x09;
	public static final byte CMD_DIR			=  0x11;
	public static final char STATUS_EOF 		=  0x10;


	// dir me
	private String 	dir_path = "";
	private boolean dir_completed=true;
	private TreeMap<String, String> dirsMap = new TreeMap<String, String>();
	private TreeMap<String, String> filesMap = new TreeMap<String, String>();
	private TreeMap<String, Integer> typeMap = new TreeMap<String, Integer>();
	private ArrayList<HashMap<String, Object>> mList = new ArrayList<HashMap<String, Object>>();
	private static final String ITEM_KEY = "key";
	private static final String ITEM_IMAGE = "image";
	// String buffer for outgoing messages
	public TextView mTest;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Resources res = getResources(); // Resource object to get Drawables
		TabHost tabHost = getTabHost();  // The activity TabHost
		TabHost.TabSpec spec;  // Resusable TabSpec for each tab
		//		Intent intent;  // Reusable Intent for each tab

		// add orders tab
		spec = tabHost.newTabSpec("orders").setIndicator("Connect",
				res.getDrawable(R.drawable.ic_tab_connect))
				.setContent(R.id.connectLayout);
		tabHost.addTab(spec);

		// add positions tab
		spec = tabHost.newTabSpec("positions").setIndicator("Upload",
				res.getDrawable(R.drawable.ic_tab_connect))
				.setContent(R.id.uploadLayout);
		tabHost.addTab(spec);

		// add strategies tab
		spec = tabHost.newTabSpec("strategies").setIndicator("Download",
				res.getDrawable(R.drawable.ic_tab_connect))
				.setContent(R.id.downloadLayout);
		tabHost.addTab(spec);	
		// layout ende

		// buttons
		mLog = (TextView) findViewById(R.id.log_value);
		mStatus = (TextView) findViewById(R.id.status_value);
		mVersion = (TextView) findViewById(R.id.version_value);
		mDownload = (TextView) findViewById(R.id.Download_textView);
		mUpload = (TextView) findViewById(R.id.Upload_textView);
		mCheckVersion = (Button) findViewById(R.id.button_checkVersion);
		mCheckVersion.setOnClickListener(this);
		mLeftButton = (Button) findViewById(R.id.button_left);
		mLeftButton.setOnClickListener(this);
		mRightButton = (Button) findViewById(R.id.button_right);
		mRightButton.setOnClickListener(this);
		mUpButton = (Button) findViewById(R.id.button_up);
		mUpButton.setOnClickListener(this);
		mDownButton = (Button) findViewById(R.id.button_down);
		mDownButton.setOnClickListener(this);
		browseToUploadMap = (Button) findViewById(R.id.browseToUploadMap);
		browseToUploadMap.setOnClickListener(this);
		browseToUploadGfx = (Button) findViewById(R.id.browseToUploadGfx);
		browseToUploadGfx.setOnClickListener(this);
		browseToUploadConfig = (Button) findViewById(R.id.browseToUploadConfig);
		browseToUploadConfig.setOnClickListener(this);
		browseToUploadSpeedo = (Button) findViewById(R.id.browseToUploadSpeedo);
		browseToUploadSpeedo.setOnClickListener(this);
		mloadRoot = (Button) findViewById(R.id.loadRoot);
		mloadRoot.setOnClickListener(this);
		DlselButton = (Button) findViewById(R.id.DownloadButtonSelect);
		DlselButton.setEnabled(false);
		DlselButton.setOnClickListener(this);
		mDLListView = (ListView) findViewById(R.id.dlList);



		if(mLog!=null) mLog.setText(R.string.bindestrich);
		if(mVersion!=null) mVersion.setText(R.string.bindestrich);





		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			toast = Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG);
			toast.show();
			finish();
			return;
		}

	}

	@Override
	public void onStart() {
		super.onStart();

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mSerialService == null) setupBT();
		}
	}


	private void setupBT() {
		Log.d(TAG, "setupBT()");

		// Initialize the BluetoothChatService to perform bluetooth connections
		mSerialService = new BluetoothSerialService(this, mHandlerBT);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mSerialService != null) mSerialService.stop();
		Log.e(TAG, "--- ON DESTROY ---");

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		mMenuItemConnect = menu.getItem(0);
		//menu.add(0,EDIT_CONTACT, 0, "Edit"). setIcon(R.drawable.edit_icon); 
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_connect:
			if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) {
				// Launch the DeviceListActivity to see devices and do scan
				Intent serverIntent = new Intent(this, DeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			}
			else {
				//if (mSerialService.getState() == BluetoothSerialService.STATE_CONNECTED) {
				mSerialService.stop();
				mSerialService.start();
			}
			return true;
		case R.id.menu_settings:
			//doPreferences();
			return true;
		case R.id.menu_about:
			//doDocumentKeys();
			return true;
		}
		return false;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " + resultCode);
		String filePath;
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
				// Attempt to connect to the device
				Log.e(TAG, "Device selected, connecting ...");
				mSerialService.connect(device);
			}
			break;

		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				//Log.d(LOG_TAG, "BT not enabled");
				//finishDialogNoBluetooth();      
				setupBT();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(TAG, "BT not enabled");
				toast = Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT);
				toast.show();
				finish();
			}
			break;
		case REQUEST_OPEN_MAP:
			filePath = data.getStringExtra(FileDialog.RESULT_PATH);
			Log.i(TAG,"File open gab diesen Dateinamen aus:"+filePath);
			mUpload.setText(filePath);
			//process_uploadFile(REQUEST_OPEN_MAP,filePath)
			break;
		case REQUEST_OPEN_CONFIG:
			filePath = data.getStringExtra(FileDialog.RESULT_PATH);
			Log.i(TAG,"File open gab diesen Dateinamen aus:"+filePath);
			mUpload.setText(filePath);
			//process_uploadFile(REQUEST_OPEN_CONFIG,filePath)
			break;
		case REQUEST_OPEN_GFX:
			filePath = data.getStringExtra(FileDialog.RESULT_PATH);
			Log.i(TAG,"File open gab diesen Dateinamen aus:"+filePath);
			mUpload.setText(filePath);
			//process_uploadFile(REQUEST_OPEN_GFX,filePath)
			break;
		case REQUEST_OPEN_SPEEDO:
			filePath = data.getStringExtra(FileDialog.RESULT_PATH);
			Log.i(TAG,"File open gab diesen Dateinamen aus:"+filePath);
			mUpload.setText(filePath);
			//process_uploadFile(REQUEST_OPEN_SPEEDO,filePath)
			break;
		case RESULT_CANCELED:
			Log.i(TAG,"File open abgebrochen");
			break;
		default:
			Log.i(TAG,"nicht gut, keine ActivityResultHandle gefunden");

		}
	}

	// The Handler that gets information back from the BluetoothService
	private final Handler mHandlerBT = new Handler() {
		@Override
		public void handleMessage(Message msg) {        	
			switch (msg.what) {

			// state switch
			case MESSAGE_STATE_CHANGE:
				Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);

				switch (msg.arg1) {
				case BluetoothSerialService.STATE_CONNECTED:
					if (mMenuItemConnect != null) {
						//mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
						mMenuItemConnect.setTitle(R.string.disconnect);
					}
					if(mStatus!=null){	mStatus.setText("Connected,Speedoino found");	};

					toast = Toast.makeText(getApplicationContext(), "Connected, Speedoino found", Toast.LENGTH_SHORT);
					toast.show();

					mTimerHandle.postDelayed(mCheckVer, 500);
					break;

				case BluetoothSerialService.STATE_CONNECTING:
					if (mMenuItemConnect != null) {
						//mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
						mMenuItemConnect.setTitle(R.string.disconnect);
					}
					if(mStatus!=null){	mStatus.setText("Connecting...");	};
					toast = Toast.makeText(getApplicationContext(), "Connecting ...", Toast.LENGTH_SHORT);
					toast.show();
					break;

				case BluetoothSerialService.STATE_NONE:
					if(mStatus!=null){	mStatus.setText(R.string.not_connected);};
					if(mVersion!=null){ mVersion.setText(R.string.bindestrich);	};
					if(mLog!=null){ 	mLog.setText(R.string.bindestrich);		};

					if (mMenuItemConnect != null) {
						//mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
						mMenuItemConnect.setTitle(R.string.connect);
					}

					toast = Toast.makeText(getApplicationContext(), "Connection closed...", Toast.LENGTH_SHORT);
					toast.show();
					break;

				case BluetoothSerialService.STATE_CONNECTED_AND_SEARCHING:
					if(mStatus!=null){	mStatus.setText("Connected, searching...");	};
					break;
				}
				break;

				// display popup
			case MESSAGE_TOAST: //?
				toast = Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),Toast.LENGTH_SHORT);
				toast.show();
				break;

			case MESSAGE_SET_LOG:
				mLog.setText(msg.getData().getString(TOAST));
				break;

			case MESSAGE_SET_VERSION:
				mVersion.setText(msg.getData().getString(TOAST));
				break;

			case MESSAGE_DIR_APPEND:

				if(dir_completed==true){
					dir_completed=false;
					mDownload.setText("");
					filesMap.clear();
					typeMap.clear();
					dirsMap.clear();
					mList.clear();
				};
				String name=msg.getData().getString("name");
				int type=msg.getData().getInt("type");

				Log.e(TAG,"CMD:"+name+" item nr:"+String.valueOf(mSerialService.item));

				if(type==1){ // file
					filesMap.put(name,name);
					typeMap.put(name,1);
				} else if(type==2){ // dir
					dirsMap.put(name, name);
					typeMap.put(name,2);
				};
				if(type==STATUS_EOF){
					dir_completed=true;
					Log.d(TAG,"beginne liste aufzubauen");

					// send to display
					SimpleAdapter fileList = new SimpleAdapter(getApplicationContext(), mList, R.layout.file_dialog_row,
							new String[] { ITEM_KEY, ITEM_IMAGE }, new int[] { R.id.fdrowtext, R.id.fdrowimage });

					if(dir_path!="/"){
						addItem("/", R.drawable.folder);
						typeMap.put("/", 2);
					};

					for (String dir : dirsMap.tailMap("").values()) {
						if(dir.toString().length()>23){
							addItem(dir.toString().substring(0, 20)+"...", R.drawable.folder);
						} else {
							addItem(dir, R.drawable.folder);
						}
					}

					for (String file : filesMap.tailMap("").values()) {
						if(file.toString().length()>23){
							addItem(file.toString().substring(0, 20)+"...", R.drawable.file);
						} else {
							addItem(file, R.drawable.file);
						}
					}
					mDLListView.setAdapter(fileList);

					mDLListView.setOnItemClickListener(new OnItemClickListener(){
						public void onItemClick(AdapterView<?> arg0, View arg1,int arg2, long arg3){
							String name=null;
							Integer type=0;
							HashMap<String, Object> item = new HashMap<String, Object>();

							item = mList.get(arg2);
							name=(String) item.get(ITEM_KEY);
							type=typeMap.get(name);

							if(type==1){
								String path="/";
								if(dir_path!="/")
									path=path+dir_path+"/";
								path=path+name;

								toast = Toast.makeText(getBaseContext(), "You clicked on file "+path, Toast.LENGTH_LONG);
								toast.show();
								DlselButton.setEnabled(true);
							} else if (type==2) {
								//toast = Toast.makeText(getBaseContext(), "loading content of folder "+name+".\nPlease wait...", 9999);
								//toast.show();
								_getDialog = new getDialog();
								_getDialog.execute(name);
							}
						}  // public void onItemClick(A
					}); // setOnItemClickListener(...
				} // if(type==STATUS_EOF){
				break;

			case MESSAGE_CMD_UNKNOWN:
				mLog.setText(R.string.unknown);
				toast = Toast.makeText(getApplicationContext(), R.string.unknown,Toast.LENGTH_SHORT);
				toast.show();
				break;

			case MESSAGE_CMD_FAILED:
				mLog.setText(R.string.noresponse);
				toast = Toast.makeText(getApplicationContext(), R.string.noresponse,Toast.LENGTH_SHORT);
				toast.show();
				break;


				// show device popup
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				toast = Toast.makeText(getApplicationContext(), "Connected, searching Speedoino", Toast.LENGTH_SHORT);
				toast.show();
				break;

			}
		}
	};



	@Override
	public void onClick(View arg0) {
		Intent intent; // reusable
		switch (arg0.getId()){
		case R.id.button_checkVersion:
			mCheckVer.run();
			break;
		case R.id.button_left:
			byte send2[] = new byte[1];
			send2[0]=CMD_GO_LEFT;
			try {
				mSerialService.send(send2,1);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			break;
		case R.id.button_up:
			byte send3[] = new byte[1];
			send3[0]=CMD_GO_UP;
			try {
				mSerialService.send(send3,1);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			break;
		case R.id.button_right:
			byte send4[] = new byte[1];
			send4[0]=CMD_GO_RIGHT;
			try {
				mSerialService.send(send4,1);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			break;
		case R.id.button_down:
			byte send5[] = new byte[1];
			send5[0]=CMD_GO_DOWN;
			try {
				mSerialService.send(send5,1);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			break;
		case R.id.browseToUploadMap:
			intent = new Intent(getBaseContext(),FileDialog.class);
			intent.putExtra(FileDialog.START_PATH, "/sdcard");
			intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
			startActivityForResult(intent, REQUEST_OPEN_MAP);
			break;
		case R.id.browseToUploadConfig:
			intent = new Intent(getBaseContext(),FileDialog.class);
			intent.putExtra(FileDialog.START_PATH, "/sdcard");
			intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
			startActivityForResult(intent, REQUEST_OPEN_CONFIG);
			break;
		case R.id.browseToUploadGfx:
			intent = new Intent(getBaseContext(),FileDialog.class);
			intent.putExtra(FileDialog.START_PATH, "/sdcard");
			intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
			startActivityForResult(intent, REQUEST_OPEN_GFX);
			break;
		case R.id.browseToUploadSpeedo:
			intent = new Intent(getBaseContext(),FileDialog.class);
			intent.putExtra(FileDialog.START_PATH, "/sdcard");
			intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
			startActivityForResult(intent, REQUEST_OPEN_SPEEDO);
			break;
		case R.id.loadRoot:
			_getDialog = new getDialog();
			_getDialog.execute("/");
			break;
		case R.id.DownloadButtonSelect:
			//		     if (selectedFile != null) {
			//                             getIntent().putExtra(RESULT_PATH, selectedFile.getPath());
			//                             setResult(RESULT_OK, getIntent());
			//                             finish();

			break;

		default:
			Log.i(TAG,"Hier wurde was geklickt das ich nicht kenne!!");
			break;
		}
	}	


	private void addItem(String fileName, int imageId) {
		HashMap<String, Object> item = new HashMap<String, Object>();
		item.put(ITEM_KEY, fileName);
		item.put(ITEM_IMAGE, imageId);
		mList.add(item);
	}



	private Runnable mCheckVer = new Runnable() {
		public void run() {
			byte send[] = new byte[1];
			send[0]=CMD_SIGN_ON;
			try {
				mSerialService.send(send,1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};
	
	private void show_dialog(){
		_progressDialog = ProgressDialog.show(this, "", "Loading directory...");
	};
	
	private void hide_dialog(){
	    if (_progressDialog != null)
            _progressDialog.dismiss();
	}
	
	//protected class getDialog extends AsyncTask<Integer, Integer, Integer> {
	protected class getDialog extends AsyncTask<String, Integer, String>{
		@Override
		protected String doInBackground( String... params ){ 
	        try {
				mSerialService.getDir(params[0]);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return "japp";
	    }

	    @Override
        protected void onPreExecute() {   				show_dialog();  };
        @Override
        protected void onPostExecute( String result ){ 	hide_dialog();	}; 
	}
}
