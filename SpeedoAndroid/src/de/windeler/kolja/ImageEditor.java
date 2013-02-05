package de.windeler.kolja;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

public class ImageEditor extends Activity implements OnClickListener, OnItemSelectedListener{

	/* image converter
	 * we need to have the following functions
	 * 1) show_preview(String input_filename) showing the image on screen, input_filename must be a convertered file
	 * 2) convert_file(String input_filename, String output_filename, boolean append) converting one image
	 * 3) prepare_image(String input_filename) if sgf or graphic file just copy, otherwise extract gif file. Function returns filename
	 * 
	 * 
	 * Than do the following on start of application:
	 * temp_filename=prepare_image(input_filename);
	 * temp_filename_converted=temp_filename.SGF
	 * if(ext!=SGF)
	 * 	convert_file(temp_filename,temp_filename_converted,false)
	 * show_preview(temp_filename_converted)
	 * 
	 * 
	 * And if you have a animation and want to see the next frame
	 * temp_filename_converted=temp_filename_1.SGF
	 * convert_file(temp_filename,temp_filename_converted,false)
	 * show_preview(temp_filename_converted)
	 * 
	 * 
	 * And if you have changed the scaling mode
	 * change var and 
	 * convert_file(temp_filename,temp_filename_converted,false)
	 * show_preview(temp_filename_converted)
	 * 
	 * 
	 * And on upload:
	 * if(multiple_file_just_different_ending) 
	 * 		count_frames
	 * 		delete_destination if exists
	 * 		for(
	 * 			temp_filename_converted=temp_filename_i.SGF
	 * 			convert_file(temp_filename,temp_filename_converted,true)
	 * 		}
	 * } else {
	 * 	convert_file(temp_filename,temp_filename_converted,false)
	 * }
	 * 
	 * finish and let the bluetooth class upload the file
	 */


	public static final String INPUT_FILE_NAME = "leeer"; // vollständinger input filename hoffe ich
	public static final String OUTPUT_FILE_PATH = "RESULT_PATH";
	private static final String TAG = "imgconv";
	public static String filename_of_file_ready_to_convert="";
	public static String filename_of_input_file="";

	private Button mButten;
	private EditText image_filename;

	private int scale_mode=0;
	private int invert_color_mode=0;
	private int show_frame=0;
	private int max_frame=0;
	private byte[] converted_image_buffer = new byte[64*64];
	private convertImageDialog _convertImageDialog;
	private boolean changing_text = false;
	private List<String> garbageList = new ArrayList<String>();

	SharedPreferences settings;
	public static final int REQUEST_SETTINGS=1;
	public static final String PREFS_NAME = "SpeedoAndroidImageEditorSettings";
	public static final String PREFS_SCALE = "scale";
	public static final String PREFS_INVERT = "invert";



	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// prepare layout
		setContentView(R.layout.image_editor_main);
		settings = getSharedPreferences(PREFS_NAME, 0);

		mButten = (Button) findViewById(R.id.SaveImageConverter);
		mButten.setOnClickListener(this);

		mButten = (Button) findViewById(R.id.DiscardImageConverter);
		mButten.setOnClickListener(this);

		mButten = (Button) findViewById(R.id.SettingsImageConverter);
		mButten.setOnClickListener(this);

		mButten = (Button) findViewById(R.id.LeftImageConverter);
		mButten.setOnClickListener(this);
		mButten.setEnabled(false);
		mButten.setBackgroundResource(R.drawable.arrow_left_gray);

		mButten = (Button) findViewById(R.id.RightImageConverter);
		mButten.setOnClickListener(this);
		mButten.setEnabled(false);
		mButten.setBackgroundResource(R.drawable.arrow_right_gray);

		image_filename = (EditText)findViewById(R.id.image_filename);
		String cleaned_filename = getIntent().getStringExtra(INPUT_FILE_NAME).substring(getIntent().getStringExtra(INPUT_FILE_NAME).lastIndexOf('/')+1).replaceAll("(?:[^a-z0-9A-Z]|(?<=['\"])s)","");

		int length_of_substring=cleaned_filename.length()-3; // "IAVjpg"
		if(length_of_substring>8){
			length_of_substring=8;
		}

		///////////////////////////////////////////////////////// TEXT FIELD PROCESS /////////////////////////////////////
		image_filename.setText(cleaned_filename.substring(0,length_of_substring));
		image_filename.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable arg0) {	};

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,	int arg2, int arg3) { };

			@Override
			public void onTextChanged(CharSequence arg0, int start,int befor, int count) {
				Log.i(TAG,"los gehts");
				if(!changing_text){
					String dirty=arg0.toString();
					String clean=dirty.replaceAll("(?:[^a-z0-9A-Z]|(?<=['\"])s)","");
					if(clean.compareTo(dirty)!=0){
						changing_text=true;
						image_filename.setText("");
						image_filename.append(clean);
						changing_text=false;
						show_toast("Please avoid special chars as well as spaces in filename, extension will be added automaticly");
					}

					if(arg0.length()>8){
						changing_text=true;
						image_filename.setText("");
						image_filename.append(arg0.subSequence(0, 8));
						changing_text=false;
						show_toast("Max 8 Chars allowed");
					}
				} // changing text
			} // on text change
		}); // add text change listener
		///////////////////////////////////////////////////////// TEXT FIELD PROCESS /////////////////////////////////////
		show_frame=0;
		scale_mode=settings.getInt(PREFS_SCALE, 0);
		invert_color_mode=settings.getInt(PREFS_INVERT, 0);
		// idea: start show_preview,
		// if the file is an ordinary image, the preview will be shown
		// if its not openable the exeption will be catched
		// and if its a gif, the gif class will split it in temp images, show the first and
		// redirect the input filename to the temp_files
		try {		
			// Than do the following on start of application:
			// temp_filename=prepare_image(input_filename);
			// temp_filename_converted=temp_filename.SGF
			// if(ext!=SGF)
			// convert_file(temp_filename,temp_filename_converted,false)
			// show_preview(temp_filename_converted)
			filename_of_input_file = getIntent().getStringExtra(INPUT_FILE_NAME);
			// prepare -> cut it if its a gif and return filename of first image
			filename_of_file_ready_to_convert=prepare_image(filename_of_input_file);
			// if the file is NOT a SGF file, convert it
			if(!filename_of_input_file.substring(filename_of_input_file.lastIndexOf(".")).toLowerCase().equals(".sgf")){
				String output_filename=filename_of_input_file.substring(0,filename_of_input_file.lastIndexOf("."))+".sgf";
				convert_image(filename_of_file_ready_to_convert, output_filename, 0, false);
				show_preview(output_filename);
			} 
			// was already a SGF file
			else {
				// we dont have to copy it, since its already a sgf file 
				show_preview(filename_of_file_ready_to_convert);
			}

		} catch (Exception e) {
			Log.e("Error reading file", e.toString());
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("Warning");
			alertDialog.setMessage("Could not open file as image! Upload anyway?");
			alertDialog.setButton("Yes",new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {	 
					getIntent().putExtra(OUTPUT_FILE_PATH,getIntent().getStringExtra(INPUT_FILE_NAME));
					cleanUp(getIntent().getStringExtra(INPUT_FILE_NAME));
					setResult(RESULT_OK, getIntent());
					finish();
				}});
			alertDialog.setButton2("No",new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {	
					cleanUp(""); 
					finish();	
				}});
			alertDialog.show();
		}


	}

	// if sgf or graphic file just copy, otherwise extract gif file. Function returns filename
	public String prepare_image(String input_filename) throws IOException{
		//get Temp dir, create the string of the resulting_inputfile
		//String resulting_inputfilename="";

		// first check if its a gif animation!
		// if so, let the Gif Decoder cut it and redirect fileinput to the temp files
		String ext=input_filename.substring(input_filename.lastIndexOf("."));

		// its a animation, return the string to the first image
		if(ext.toLowerCase().equals(".gif")){
			File outputDir =  getBaseContext().getCacheDir(); // context being the Activity pointer
			String filename_without_ext=input_filename.substring(0, input_filename.lastIndexOf("."));					
			FileInputStream stream=new FileInputStream(input_filename);

			// cut it! this will generate a lot of files, filename => foo.gif --> foo_0.gif, foo_1.gif, ... in the same dir
			GifDecoderView status=new GifDecoderView(this, stream,filename_without_ext,outputDir.getAbsolutePath());
			max_frame=status.mGifDecoder.getFrameCount();
			String filename=outputDir.getAbsolutePath()+filename_without_ext.substring(filename_without_ext.lastIndexOf("/")+1)+"_";
			for(int i=0; i<max_frame;i++){
				garbageList.add(filename+String.valueOf(i)+".PNG");
			}

			// status checken .. sollte 0 sein, handle path to intent
			if(max_frame>1){
				mButten = (Button) findViewById(R.id.RightImageConverter);
				mButten.setEnabled(true);
				mButten.setBackgroundResource(R.drawable.arrow_right);
			}

			// this is the filename of the first frame
			return outputDir.getAbsolutePath()+filename_without_ext.substring(filename_without_ext.lastIndexOf("/")+1)+"_0.PNG";

		} else if(ext.toLowerCase().equals(".sgf")){
			return input_filename;

		} else {
			// let convert_image to the work and create our image
			return input_filename;
		}
	}

	// this just shows a already converted image
	public void show_preview(String filename_of_converted_file) throws IOException{
		// it doesn't matter if the input was a gif, a regular image, or a speedoino file .. the 
		// resulting_inputfilename holds the Path to a converted image, ready to display it now

		// open it to read the result
		FileInputStream in=new FileInputStream(filename_of_converted_file);
		Bitmap bMap = Bitmap.createBitmap(128, 64, Bitmap.Config.RGB_565);
		ImageView image = (ImageView) findViewById(R.id.imageSpeedoPreview);
		// read datablock
		in.read(converted_image_buffer, 0, (int)(64*128*0.5)); // 64 lines, 128 cols, but just a half byte per px
		in.close();

		// copy converted raw values to bitmap
		int x=0,y=0;
		for(int byte_read_counter=0;byte_read_counter<64*128*0.5;byte_read_counter++){
			int gs=(converted_image_buffer[byte_read_counter] & 0xf0);
			bMap.setPixel(x,y,Color.rgb(gs,gs,0)); // yellow = green+red, no blue
			gs=(converted_image_buffer[byte_read_counter] & 0xf) << 4;
			bMap.setPixel(x+1,y,Color.rgb(gs,gs,0)); // yellow = green+red, no blue
			x+=2;
			if(x>127){
				y++;
				x=0;
			}
		}

		// rotate image and shrink it
		DisplayMetrics metrics = new DisplayMetrics();
		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		display.getMetrics(metrics);
		Matrix mat = new Matrix();
		mat.postRotate(90);
		Bitmap bMapRotate = Bitmap.createBitmap(bMap, 0, 0, bMap.getWidth(), bMap.getHeight(), mat, true);
		bMap = Bitmap.createScaledBitmap(bMapRotate,(int)(display.getHeight()*0.25), (int) (display.getHeight()*0.5), true);

		// show it
		image.setImageBitmap(bMap);
	}

	// this routine gets a input and a output filename, absolute path
	// warning could be one of 0=no warning or 1=warning if source does not exists
	// the boolean value append, switches the file write option. set true for animations, but delete it in advance
	// int scale_mode: 0) scale it without keeping aspect 1) scale and keep aspect 2) crop it
	public void convert_image(String filename_in, String filename_out, Integer warning, boolean append) throws IOException{
		FileInputStream in,in_size;
		BufferedInputStream buf,buf_size;

		// open it twice, once to get the size
		in_size = new FileInputStream(filename_in);
		in = new FileInputStream(filename_in);
		buf_size = new BufferedInputStream(in_size);
		buf = new BufferedInputStream(in);

		// checkout sizes
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(buf_size,null,options);
		int imageHeight = options.outHeight;
		int imageWidth = options.outWidth;
		buf_size.close();
		in_size.close();


		// load full oder downsized image
		Bitmap bMap;
		if(imageHeight > 256 && imageWidth> 512){
			BitmapFactory.Options opts= new BitmapFactory.Options();
			if(imageHeight>imageWidth*2){
				opts.inSampleSize=imageWidth*4/128; // 4 fach zu groß
			} else {
				opts.inSampleSize=imageHeight*4/6; // 4 fach zu groß
			}
			opts.inSampleSize=4;
			bMap = BitmapFactory.decodeStream(buf,null,opts);
		}    else {
			bMap = BitmapFactory.decodeStream(buf);
		}


		//if bitmap was openable start conversion
		if(bMap!=null){
			// now we have to decide the mode to compress the image in a suiteable size (128x64)
			// 0) Scale it without keeping aspect radio
			// 1) Scale it but keep the aspect. therefor we have to fill the shorter border with "fitting" pixels, and center the scaled image
			// 2) Crop it from the existing picture. simply cut a rect 128x64 from the center of the source image

			// the resulting image
			Bitmap bMapScaled = null; 

			// backup, scale without aspect
			if(scale_mode==0 || scale_mode>2){
				bMapScaled= Bitmap.createScaledBitmap(bMap, 128, 64, true);
				//				bMapScaled=convertToMutable(bMapScaled);
			} 
			// scale it and keep aspect
			else if(scale_mode==1){
				Bitmap bMapScaledTemp;
				bMapScaled= Bitmap.createBitmap(128,64,Bitmap.Config.RGB_565);
				bMapScaled=convertToMutable(bMapScaled);

				// get limiting factor
				float factor_y=bMap.getHeight()*100/64;
				float factor_x=bMap.getWidth()*100/128;

				// image scaling
				int width=0, height=0;
				if(factor_x>factor_y){
					width=(int)(bMap.getWidth()*100/factor_x);
					height=(int)(bMap.getHeight()*100/factor_x);
				} else {
					width=(int)(bMap.getWidth()*100/factor_y);
					height=(int)(bMap.getHeight()*100/factor_y);
				}
				bMapScaledTemp=Bitmap.createScaledBitmap(bMap,width, height, true);

				// fill result image with color
				long avg_color=0;
				int fill_color=0;
				for(int y=0; y<bMapScaledTemp.getHeight(); y++){
					for(int x=0;x<2;x++){
						avg_color+=bMapScaledTemp.getPixel(x,y)& 0xFF; // blue channel, but in grayscale just grayscale
					}
				}
				avg_color/=(bMapScaledTemp.getHeight()*2);
				// if its brighter than half ... turn it on
				if(avg_color>128){
					fill_color=255;
				}
				// draw rect TODO
				for(int x=0; x<bMapScaled.getWidth(); x++){
					for(int y=0; y<bMapScaled.getHeight(); y++){
						bMapScaled.setPixel(x,y,Color.rgb(fill_color,fill_color,fill_color));
					}
				}

				// copy source image to this bitmap now
				int dest_x=0,dest_y=0;
				int offset_x=0,offset_y=0;
				offset_x=(128-bMapScaledTemp.getWidth())/2;
				offset_y=(64-bMapScaledTemp.getHeight())/2;
				for(int x=0; x<bMapScaledTemp.getWidth(); x++){
					for(int y=0; y<bMapScaledTemp.getHeight(); y++){
						dest_x=offset_x+x;
						dest_y=offset_y+y;
						bMapScaled.setPixel(dest_x,dest_y,bMapScaledTemp.getPixel(x,y));
					};
				};
			} 
			// crop it
			else if(scale_mode==2) { 
				// create bitmap
				bMapScaled= Bitmap.createBitmap(128,64,Bitmap.Config.RGB_565);
				bMapScaled=convertToMutable(bMapScaled);

				int source_x=(bMap.getWidth()-128)/2;
				int source_y=(bMap.getHeight()-64)/2;
				int width=128;
				int height=64;
				boolean filling_needed=false;
				if(source_x<0){ // the source is just higher, but not wider
					source_x=0;
					width=bMap.getWidth();
					filling_needed=true;
				}
				if(source_y<0){ // sourch just no as high es needed
					source_y=0;
					height=bMap.getHeight();
					filling_needed=true;
				}

				if(filling_needed){
					// fill result image with color
					Bitmap bMapScaledTemp=Bitmap.createBitmap(bMap, source_x, source_y, width, height); // this one is smaller than 128x64
					long avg_color=0;
					int fill_color=0;
					for(int y=0; y<bMapScaledTemp.getHeight(); y++){
						for(int x=0;x<2;x++){
							avg_color+=bMapScaledTemp.getPixel(x,y)& 0xFF; // blue channel, but in grayscale just grayscale
						}
					}
					avg_color/=(bMapScaledTemp.getHeight()*2);
					// if its brighter than half ... turn it on
					if(avg_color>128){
						fill_color=255;
					}
					// draw rect TODO 128x64
					for(int x=0; x<bMapScaled.getWidth(); x++){
						for(int y=0; y<bMapScaled.getHeight(); y++){
							bMapScaled.setPixel(x,y,Color.rgb(fill_color,fill_color,fill_color));
						}
					}

					// copy source image to this bitmap now
					int dest_x=0,dest_y=0;
					int offset_x=0,offset_y=0;
					offset_x=(128-bMapScaledTemp.getWidth())/2;
					offset_y=(64-bMapScaledTemp.getHeight())/2;
					for(int x=0; x<bMapScaledTemp.getWidth(); x++){
						for(int y=0; y<bMapScaledTemp.getHeight(); y++){
							dest_x=offset_x+x;
							dest_y=offset_y+y;
							bMapScaled.setPixel(dest_x,dest_y,bMapScaledTemp.getPixel(x,y));
						};
					};
				} else {
					// crop out a temp image
					bMapScaled= Bitmap.createBitmap(bMap, source_x, source_y, width, height);
				}
			}


			// convertion to color shema
			int gs=0;
			int left = 0,right =0;
			for(int y=0; y<bMapScaled.getHeight(); y++){
				for(int x=0;x<bMapScaled.getWidth();x++){
					gs=((bMapScaled.getPixel(x,y)>> 16) & 0xFF)*30; // R
					gs+=((bMapScaled.getPixel(x,y)>> 8) & 0xFF)*59; // G
					gs+=(bMapScaled.getPixel(x,y) & 0xFF)*11; // B
					gs/=(30+59+11);
					gs=(int) (Math.floor(gs/16)*16);	// Reduzierung auf 16 Graustufen
					if(invert_color_mode==1){
						gs=255-gs;
					}

					if(x%2==0){
						left=(gs/16) & 0xFF;
					}
					if(x%2==1) {
						//Log.i(TAG,"bin bei x%2==1, x="+String.valueOf(x)+" y="+String.valueOf(y));
						// [0..25]/16 = [0..15]

						right=(gs/16) & 0xFF;
						converted_image_buffer[(int) (64*y+Math.floor(x/2))]= (byte)((left<<4 | right) & 0xFF); // x=0,y=0 => 0 x=127,y=0 => 63
						//Log.i(TAG,"left="+String.valueOf(left)+" right="+String.valueOf(right)+" kombi: "+String.valueOf((byte)((left<<4 | right) & 0xFF)) +" pos:"+String.valueOf((int) (64*y+Math.floor(x/2))));

					}
				};
			};

			// write to file
			FileOutputStream out = new FileOutputStream(filename_out,append);
			garbageList.add(filename_out);
			out.write(converted_image_buffer,0,converted_image_buffer.length);
			out.close();

		} else if (warning==1) { // image konnte nicht geöffnet werden
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("Warning");
			alertDialog.setMessage("Could not open file as image!");
			alertDialog.setButton("OK",new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {	
					cleanUp(""); 
					finish();	
				}});
			alertDialog.show();
		}
		if (buf != null) {
			buf.close();
		}
		if (in != null) {
			in.close();
		}
	}

	public void show_toast(String msg){
		Toast toaster=Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		toaster.show();
	}

	@Override
	public void onClick(View arg0) {
		if(arg0.getId()==R.id.SaveImageConverter){
			// open File
			//String result_filename = basedir+image_filename.getText().toString()+".sgf";
			String input_filename = filename_of_file_ready_to_convert;
			String output_filename=filename_of_input_file.substring(0,filename_of_input_file.lastIndexOf("."))+".sgf";

			// check if there are more images with the same name
			String filename_without_ext=input_filename.substring(0, input_filename.lastIndexOf("."));
			String ext=input_filename.substring(input_filename.lastIndexOf("."));

			// not jet converted .. do it
			if(!ext.toLowerCase().equals(".sgf")){
				Log.i("JKW","Check filename:"+filename_without_ext);
				// prepare filename
				String convert_filename;

				// animation
				if(max_frame>1){
					show_toast("Animation found, converting and uploading complete animation");
					// show dialog
					convert_filename=filename_without_ext.substring(0,filename_without_ext.length()-1); // -1 to remove the "0"
				} 
				// single image
				else {
					convert_filename=input_filename;
					ext=""; // set extention to empty to signalize that this is a single image
				};

				// run conversion
				getIntent().putExtra(OUTPUT_FILE_PATH,output_filename);
				_convertImageDialog = new convertImageDialog(this);
				_convertImageDialog.execute(convert_filename,ext,String.valueOf(max_frame),output_filename); // warum müssen das alles strings sein?
			} else {
				// it was already converted .. tell it to the upload
				cleanUp(output_filename);
				getIntent().putExtra(OUTPUT_FILE_PATH,output_filename);
				setResult(RESULT_OK, getIntent());
				finish();
			}
			// dont write anything down here, because it will be executed WHILE the conversion is running .. exept you want that!

		} else if(arg0.getId()==R.id.DiscardImageConverter){
			setResult(RESULT_CANCELED, getIntent());
			cleanUp("");
			finish();

		} else if(arg0.getId()==R.id.RightImageConverter){
			show_frame++;
			String filename_of_frame=filename_of_file_ready_to_convert.substring(0,filename_of_file_ready_to_convert.lastIndexOf("_")+1);

			try {
				convert_image(filename_of_frame+String.valueOf(show_frame)+".PNG", filename_of_frame+String.valueOf(show_frame)+".sgf", 0, false);
				show_preview(filename_of_frame+String.valueOf(show_frame)+".sgf");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if(show_frame>=max_frame){
				mButten = (Button) findViewById(R.id.RightImageConverter);
				mButten.setEnabled(false);
				mButten.setBackgroundResource(R.drawable.arrow_right_gray);
			}
			if(show_frame>0){	
				mButten = (Button) findViewById(R.id.LeftImageConverter);
				mButten.setEnabled(true);
				mButten.setBackgroundResource(R.drawable.arrow_left);
			}

		} else if(arg0.getId()==R.id.LeftImageConverter){
			if(show_frame>0){
				show_frame--;
			}
			String filename_of_frame=filename_of_file_ready_to_convert.substring(0,filename_of_file_ready_to_convert.lastIndexOf("_")+1);

			try {
				convert_image(filename_of_frame+String.valueOf(show_frame)+".PNG", filename_of_frame+String.valueOf(show_frame)+".sgf", 0, false);
				show_preview(filename_of_frame+String.valueOf(show_frame)+".sgf");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if(show_frame<=0){
				mButten = (Button) findViewById(R.id.LeftImageConverter);
				mButten.setEnabled(false);
				mButten.setBackgroundResource(R.drawable.arrow_left_gray);
			}
			if(show_frame<max_frame){	
				mButten = (Button) findViewById(R.id.RightImageConverter);
				mButten.setEnabled(true);
				mButten.setBackgroundResource(R.drawable.arrow_right);
			}
		} else if(arg0.getId()==R.id.SettingsImageConverter){
			Intent intent = new Intent(getBaseContext(), ImageEditorSettings.class);
			startActivityForResult(intent, REQUEST_SETTINGS);
		};
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " + resultCode + " " + requestCode);
		switch (requestCode) {
		case REQUEST_SETTINGS:
			scale_mode=settings.getInt(PREFS_SCALE, 0);
			invert_color_mode=settings.getInt(PREFS_INVERT, 0);
			String output_filename=filename_of_file_ready_to_convert.substring(0,filename_of_file_ready_to_convert.lastIndexOf("."))+".sgf";
			try {
				convert_image(filename_of_file_ready_to_convert, output_filename, 0, false);
				show_preview(output_filename);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		default:
			Log.i(TAG, "nicht gut, keine ActivityResultHandle gefunden");
			break;
		}
	}

	public static Bitmap convertToMutable(Bitmap imgIn) {
		try {
			//this is the file going to use temporally to save the bytes. 
			// This file will not be a image, it will store the raw image data.
			File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

			//Open an RandomAccessFile
			//Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
			//into AndroidManifest.xml file
			RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

			// get the width and height of the source bitmap.
			int width = imgIn.getWidth();
			int height = imgIn.getHeight();
			Config type = imgIn.getConfig();

			//Copy the byte to the file
			//Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
			FileChannel channel = randomAccessFile.getChannel();
			MappedByteBuffer map = channel.map(MapMode.READ_WRITE, 0, imgIn.getRowBytes()*height);
			imgIn.copyPixelsToBuffer(map);
			//recycle the source bitmap, this will be no longer used.
			imgIn.recycle();
			System.gc();// try to force the bytes from the imgIn to be released

			//Create a new bitmap to load the bitmap again. Probably the memory will be available. 
			imgIn = Bitmap.createBitmap(width, height, type);
			map.position(0);
			//load it back from temporary 
			imgIn.copyPixelsFromBuffer(map);
			//close the temporary file and channel , then delete that also
			channel.close();
			randomAccessFile.close();

			// delete the temp file
			file.delete();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 

		return imgIn;
	}


	// This class should convert the images for us
	// calling paramter:
	// params[0] is the filename
	// params[1] indicates if its a animation or a single image. IF its a animation, it holds the extension. Otherwise its empty 
	// params[2] String casted number of frames, if animation
	// params[3] resulting filename
	protected class convertImageDialog extends AsyncTask<String, Integer, String> {
		private Context context;
		ProgressDialog dialog;

		// just grap the content and prepare the dialog which will be updated
		public convertImageDialog(Context cxt) {
			context = cxt;
			dialog = new ProgressDialog(context);
		}

		@Override
		protected String doInBackground(String... params) {
			Log.i("JKW","starte convertierung");
			try {
				Message msg;
				Bundle bundle;
				String filename=params[0];
				String extension=params[1];
				int frames=Integer.parseInt(params[2]);
				String result_filename=params[3];

				// wenn er nicht leer ist, ists eine animation
				if(!extension.equals("")){  

					// check if file exists and delete it if so
					File target_file = new File(result_filename);
					if(target_file.exists()){
						target_file.delete();
					}

					// convert all files, one by one and append it
					for(int i=0;i<frames;i++){
						String input_filename_convert=filename+String.valueOf(i)+extension;

						msg=mHandlerUpdate.obtainMessage();
						bundle = new Bundle();
						bundle.putInt("current", i);
						bundle.putInt("total", frames);
						msg.setData(bundle);
						mHandlerUpdate.sendMessage(msg);

						convert_image(input_filename_convert, result_filename, 1,true);
					};
					// a simple single image
				} else {
					convert_image(filename, result_filename, 1,false); 
				}
				// clean all temp files
				cleanUp(result_filename);
				
				// close the hole app, because we are done
				Log.i("JKW","Aus dem ImageEditor gebe ich den Dateinamen "+result_filename+" zurück");

			}
			catch (IOException e) {		
				e.printStackTrace();								
			};
			
			// now its converted .. tell it to upload
			setResult(RESULT_OK, getIntent());
			return "japp";
		}

		@Override
		protected void onPreExecute() {
			dialog.setMessage("converting image ...");
			dialog.show();
		};

		@Override
		protected void onPostExecute(String result) {
			dialog.dismiss();
			// close imageEditor
			finish();
		}

		// the dialog updater
		private final Handler mHandlerUpdate = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				dialog.setMessage("Converting image "+msg.getData().getInt("current")+" of "+msg.getData().getInt("total"));
				dialog.setProgress(100*msg.getData().getInt("current")/msg.getData().getInt("total"));
			};
		};
	}


	@Override
	/* And if you have changed the scaling mode
	 * change var and 
	 * convert_file(temp_filename,temp_filename_converted,false)
	 * show_preview(temp_filename_converted)
	 * @param parent - the AdapterView for this listener
	 * @param v - the View for this listener
	 * @param pos - the 0-based position of the selection in the mLocalAdapter
	 * @param row - the 0-based row number of the selection in the View
	 */
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
		scale_mode=arg2;
		show_frame=0;
		try {
			String output_filename=filename_of_file_ready_to_convert.substring(0,filename_of_file_ready_to_convert.lastIndexOf("."))+".sgf";
			convert_image(filename_of_file_ready_to_convert, output_filename, 0, false);
			show_preview(output_filename);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {	}

	public void cleanUp(String preventThisFileFromBeingDeleted){
		File tempFile=null;
		for(int i=0; i<garbageList.size(); i++){
			if(!garbageList.get(i).equals(preventThisFileFromBeingDeleted)){
				tempFile=new File(garbageList.get(i));
				if(tempFile.exists()){
					tempFile.delete();
				};
			}
		}
	}

}