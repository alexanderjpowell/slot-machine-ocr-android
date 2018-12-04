// ABBYY Real-Time Recognition SDK 1 © 2016 ABBYY Production LLC
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.mobile.sample.passportreader;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.abbyy.mobile.rtr.Engine;
import com.abbyy.mobile.rtr.IDataCaptureService;
import com.abbyy.mobile.rtr.IRecognitionService;
import com.abbyy.mobile.sample.passportreader.interfaces.ICameraActivity;
import com.abbyy.mobile.sample.passportreader.interfaces.ICurrentResultsFragment;
import com.abbyy.mobile.sample.passportreader.interfaces.IFinalResultsFragment;
import com.abbyy.mobile.sample.passportreader.interfaces.IPassportRecognitionPresenter;
import com.abbyy.mobile.sample.passportreader.interfaces.IPromptFragment;

import java.util.ArrayList;
import java.util.List;

// Аctivity with camera preview is the main activity of the application. All interaction
// with ABBYY Real-Time Recognition SDK is done here.
public class CameraActivity extends AppCompatActivity implements ICameraActivity {

	// Licensing
	private static final String licenseFileName = "AbbyyRtrSdk.license";

	///////////////////////////////////////////////////////////////////////////////
	// Some application settings that can be changed to modify application behavior:
	// The camera zoom. Optically zooming with a good camera often improves results
	// even at close range and it might be required at longer ranges.
	private static final int cameraZoom = 1;
	// Aspect ratio of document (width/height)
	private static final double aspectRatio = 574.0 / 387;
	// Camera permission request code for Android 6.0
	private static final int CAMERA_PERMISSION_REQUEST_CODE = 42;
	// Tap duration for manual stopping mode enabling
	private static final int MANUAL_STOPPING_CONTROL_THRESHOLD_MILLISECONDS = 900;
	///////////////////////////////////////////////////////////////////////////////

	// The 'Abbyy RTR SDK Engine' and 'Data Capture Service' to be used in this sample application
	private Engine engine;
	private IDataCaptureService dataCaptureService;

	// The camera and the preview surface
	private Camera camera;
	private SurfaceViewWithOverlay surfaceViewWithOverlay;
	private SurfaceHolder previewSurfaceHolder;

	// Actual preview size and orientation
	private Camera.Size cameraPreviewSize;
	private int orientation;

	// Auxiliary variables
	private boolean inPreview = false; // Camera preview is started
	private Handler handler = new Handler(); // Used for posting delayed actions to UI thread
	private boolean isRunning = false; // Recognition is in progress
	private boolean cameraPermissionRequested = false; // Camera permission request has been sent to user (Android 6+)
	private boolean isAutofocusInProgress = false; // Autofocus is in progress

	// UI components
	private TextView warningTextView; // Show warnings from recognizer
	private TextView errorTextView; // Show errors from recognizer
	private ImageView pictureImageView; // Show prompt picture
	private PopupWindow settingsPopup; // Popup view for settings

	// To communicate with the Data Capture Service we will need this callback:
	private IDataCaptureService.Callback dataCaptureCallback = new IDataCaptureService.Callback() {
		@Override
		public void onRequestLatestFrame( byte[] buffer )
		{
			// The service asks to fill the buffer with image data for the latest frame in NV21 format.
			// Delegate this task to the camera. When the buffer is filled we will receive
			// Camera.PreviewCallback.onPreviewFrame (see below)
			camera.addCallbackBuffer( buffer );
		}

		@Override
		public void onFrameProcessed( IDataCaptureService.DataScheme scheme,
			IDataCaptureService.DataField[] fields,
			IDataCaptureService.ResultStabilityStatus resultStatus,
			IDataCaptureService.Warning warning )
		{
			// This callback method is called with these arguments:
			// scheme - represents the type of the 'document' captured by DataCaptureService and the associated data scheme.
			// Scheme may also differentiate parts of a 'document', for example, the two pages of the Russian Passport have different schemes.
			// Null is passed if no document is matched, but the 'fields' might still contain some unstructured components of what we actually see
			// fields: recognized fields. Some fields might contain multiple components as, for example, parts of the number of
			// the Russian Passport (01 23 456789)
			// resultStatus: stability of the results
			// warning: warning if something is going wrong (or might be going wrong)

			presenter.onFrameProcessed( scheme, fields, resultStatus );

			// Show the warning from the service if any. The warnings are intended for the user
			// to take some action (zooming in, checking recognition language, etc.)
			if( warning != IRecognitionService.Warning.TextTooSmall ) {
				warningTextView.setText( warning != null ? warning.name() : "" );
			}

			surfaceViewWithOverlay.setLines( fields );
		}

		@Override
		public void onError( Exception e )
		{
			// An error occurred while processing. Log it. Processing will continue
			Log.e( getString( R.string.app_name ), "Error: " + e.getMessage() );
			if( BuildConfig.DEBUG ) {
				// Make the error easily visible to the developer
				String message = e.getMessage();
				if( message.contains( "ChineseJapanese.rom" ) ) {
					message =
						"Chinese, Japanese and Korean are available in EXTENDED version only. Contact us for more information.";
				}
				if( message.contains( "Russian.edc" ) ) {
					message =
						"Cyrillic script languages are available in EXTENDED version only. Contact us for more information.";
				} else if( message.contains( ".trdic" ) ) {
					message = "Translation is available in EXTENDED version only. Contact us for more information.";
				}
				errorTextView.setText( message );
			}
		}
	};

	// This callback will be used to obtain frames from the camera
	private Camera.PreviewCallback cameraPreviewCallback = new Camera.PreviewCallback() {
		@Override
		public void onPreviewFrame( byte[] data, Camera camera )
		{
			// The buffer that we have given to the camera in IDataCaptureService.Callback.onRequestLatestFrame
			// above have been filled. Send it back to the Data Capture Service
			dataCaptureService.submitRequestedFrame( data );
		}
	};

	// This callback is used to configure preview surface for the camera
	SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

		@Override
		public void surfaceCreated( SurfaceHolder holder )
		{
			// When surface is created, store the holder
			previewSurfaceHolder = holder;
		}

		@Override
		public void surfaceChanged( SurfaceHolder holder, int format, int width, int height )
		{
			// When surface is changed (or created), attach it to the camera, configure camera and start preview
			if( camera != null ) {
				if( isRunning ) {
					stopRecognition();
				}

				setCameraPreviewDisplayAndStartPreview();
			}
		}

		@Override
		public void surfaceDestroyed( SurfaceHolder holder )
		{
			// When surface is destroyed, clear previewSurfaceHolder
			previewSurfaceHolder = null;
		}
	};

	// Start recognition when autofocus completes (used when continuous autofocus is disabled)
	private Camera.AutoFocusCallback startRecognitionCameraAutoFocusCallback = new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus( boolean success, Camera camera )
		{
			onAutoFocusFinished( success, camera );
			startRecognition();
		}
	};

	// Empty autofocus callback (no action is taken except resetting the flag, indicating that autofocus is in progress)
	private Camera.AutoFocusCallback emptyAutoFocusCallback = new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus( boolean success, Camera camera )
		{
			onAutoFocusFinished( success, camera );
		}
	};

	private void onAutoFocusFinished( boolean success, Camera camera )
	{
		isAutofocusInProgress = false;
		if( isContinuousVideoFocusModeEnabled( camera ) ) {
			setCameraFocusMode( Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO );
		} else {
			if( !success ) {
				autoFocus( emptyAutoFocusCallback );
			}
		}
	}

	// Common method for starting autofocus (used when continuous autofocus is disabled)
	private void autoFocus( final Camera.AutoFocusCallback callback )
	{
		if( camera != null ) {
			try {
				isAutofocusInProgress = true;
				setCameraFocusMode( Camera.Parameters.FOCUS_MODE_AUTO );
				camera.autoFocus( callback );
			} catch( Exception e ) {
				Log.e( getString( R.string.app_name ), "Error: " + e.getMessage() );
				handler.post( new Runnable() {
					@Override public void run()
					{
						warningTextView.setText( R.string.focus_error_message );
						// To correctly reset the state (we are not in autofocus any more)
						callback.onAutoFocus( false, null );
					}
				} );
			}
		}
	}

	// Checks that FOCUS_MODE_CONTINUOUS_VIDEO supported
	private boolean isContinuousVideoFocusModeEnabled( Camera camera )
	{
		return camera.getParameters().getSupportedFocusModes().contains( Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO );
	}

	// Sets camera focus mode and focus area
	private void setCameraFocusMode( String mode )
	{
		// Camera sees it as rotated 90 degrees, so there's some confusion with what is width and what is height)
		int width = 0;
		int height = 0;
		int halfCoordinates = 1000;
		int lengthCoordinates = 2000;
		Rect area = surfaceViewWithOverlay.getAreaOfInterest();
		switch( orientation ) {
			case 0:
			case 180:
				height = cameraPreviewSize.height;
				width = cameraPreviewSize.width;
				break;
			case 90:
			case 270:
				width = cameraPreviewSize.height;
				height = cameraPreviewSize.width;
				break;
		}

		camera.cancelAutoFocus();
		Camera.Parameters parameters = camera.getParameters();
		// Set focus and metering area equal to the area of interest. This action is essential because by defaults camera
		// focuses on the center of the frame, while the area of interest in this sample application is at the top
		List<Camera.Area> focusAreas = new ArrayList<>();
		Rect areasRect;

		switch( orientation ) {
			case 0:
				areasRect = new Rect( -halfCoordinates + area.left * lengthCoordinates / width, -halfCoordinates + area.top * lengthCoordinates / height,
					-halfCoordinates + lengthCoordinates * area.right / width, -halfCoordinates + lengthCoordinates * area.bottom / height );
				break;
			case 180:
				areasRect = new Rect( halfCoordinates - area.right * lengthCoordinates / width, halfCoordinates - area.bottom * lengthCoordinates / height,
					halfCoordinates - lengthCoordinates * area.left / width, halfCoordinates - lengthCoordinates * area.top / height );
				break;
			case 90:
				areasRect = new Rect( -halfCoordinates + area.top * lengthCoordinates / height, halfCoordinates - area.right * lengthCoordinates / width,
					-halfCoordinates + lengthCoordinates * area.bottom / height, halfCoordinates - lengthCoordinates * area.left / width );
				break;
			case 270:
				areasRect = new Rect( halfCoordinates - area.bottom * lengthCoordinates / height, -halfCoordinates + area.left * lengthCoordinates / width,
					halfCoordinates - lengthCoordinates * area.top / height, -halfCoordinates + lengthCoordinates * area.right / width );
				break;
			default:
				throw new IllegalArgumentException();
		}

		focusAreas.add( new Camera.Area( areasRect, 800 ) );
		if( parameters.getMaxNumFocusAreas() >= focusAreas.size() ) {
			parameters.setFocusAreas( focusAreas );
		}
		if( parameters.getMaxNumMeteringAreas() >= focusAreas.size() ) {
			parameters.setMeteringAreas( focusAreas );
		}

		parameters.setFocusMode( mode );

		// Commit the camera parameters
		camera.setParameters( parameters );
	}

	// Attach the camera to the surface holder, configure the camera and start preview
	private void setCameraPreviewDisplayAndStartPreview()
	{
		try {
			camera.setPreviewDisplay( previewSurfaceHolder );
		} catch( Throwable t ) {
			Log.e( getString( R.string.app_name ), "Exception in setPreviewDisplay()", t );
		}
		configureCameraAndStartPreview( camera );
	}

	// Stop preview and release the camera
	private void stopPreviewAndReleaseCamera()
	{
		if( camera != null ) {
			camera.setPreviewCallbackWithBuffer( null );
			if( inPreview ) {
				camera.stopPreview();
				inPreview = false;
			}
			camera.release();
			camera = null;
		}
	}

	// Show error on startup if any
	private void showStartupError( String message )
	{
		new AlertDialog.Builder( this )
			.setTitle( "ABBYY RTR SDK" )
			.setMessage( message )
			.setIcon( android.R.drawable.ic_dialog_alert )
			.show()
			.setOnDismissListener( new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss( DialogInterface dialog )
				{
					CameraActivity.this.finish();
				}
			} );
	}

	// Load ABBYY RTR SDK engine and configure the data capture service
	private boolean createDataCaptureService()
	{
		// Initialize the engine and data capture service
		try {
			engine = Engine.load( this, licenseFileName );
			dataCaptureService = engine.createDataCaptureService( "Passport_RU", dataCaptureCallback );
			return true;
		} catch( java.io.IOException e ) {
			// Troubleshooting for the developer
			Log.e( getString( R.string.app_name ), "Error loading ABBYY RTR SDK:", e );
			showStartupError( "Could not load some required resource files. Make sure to configure " +
				"'assets' directory in your application and specify correct 'license file name'. See logcat for details." );
		} catch( Engine.LicenseException e ) {
			// Troubleshooting for the developer
			Log.e( getString( R.string.app_name ), "Error loading ABBYY RTR SDK:", e );
			showStartupError( "License not valid. Make sure you have a valid license file in the " +
				"'assets' directory and specify correct 'license file name' and 'application id'. See logcat for details." );
		} catch( Throwable e ) {
			// Troubleshooting for the developer
			Log.e( getString( R.string.app_name ), "Error loading ABBYY RTR SDK:", e );
			showStartupError( "Unspecified error while loading the engine. See logcat for details." );
		}

		return false;
	}

	// Start recognition
	private void startRecognition()
	{
		// Do not switch off the screen while data capture service is running
		previewSurfaceHolder.setKeepScreenOn( true );

		// Reset 'stability' indicators
		setStable( false );

		// Start the service
		dataCaptureService.start( cameraPreviewSize.width, cameraPreviewSize.height, orientation,
			surfaceViewWithOverlay.getAreaOfInterest() );
		isRunning = true;
	}

	// Stop recognition
	@Override
	public void stopRecognition()
	{
		isRunning = false;

		// Stop the service asynchronously to make application more responsive. Stopping can take some time
		// waiting for all processing threads to stop
		new AsyncTask<Void, Void, Void>() {
			protected Void doInBackground( Void... params )
			{
				dataCaptureService.stop();
				return null;
			}

			protected void onPostExecute( Void result )
			{
				// Restore normal power saving behaviour
				previewSurfaceHolder.setKeepScreenOn( false );

				presenter.onRecognitionServiceStopped();
			}
		}.execute();
	}

	// Opens camera, configures and starts preview
	private void openCameraAndPreview()
	{
		camera = Camera.open();
		if( previewSurfaceHolder != null ) {
			setCameraPreviewDisplayAndStartPreview();
		}
	}

	// Returns orientation of camera
	private int getCameraOrientation()
	{
		Display display = getWindowManager().getDefaultDisplay();
		int orientation = 0;
		switch( display.getRotation() ) {
			case Surface.ROTATION_0:
				orientation = 0;
				break;
			case Surface.ROTATION_90:
				orientation = 90;
				break;
			case Surface.ROTATION_180:
				orientation = 180;
				break;
			case Surface.ROTATION_270:
				orientation = 270;
				break;
		}
		for( int i = 0; i < Camera.getNumberOfCameras(); i++ ) {
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			Camera.getCameraInfo( i, cameraInfo );
			if( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK ) {
				return ( cameraInfo.orientation - orientation + 360 ) % 360;
			}
		}
		// If Camera.open() succeed, this point of code never reached
		return -1;
	}

	private void configureCameraAndStartPreview( Camera camera )
	{
		// Configure camera orientation. This is needed for both correct preview orientation
		// and recognition
		orientation = getCameraOrientation();
		camera.setDisplayOrientation( orientation );

		// Configure camera parameters
		Camera.Parameters parameters = camera.getParameters();

		// Select preview size. The preferred size for the Russian Passport scenario is Full HD (or higher on
		// high performance devices)
		cameraPreviewSize = null;
		for( Camera.Size size : parameters.getSupportedPreviewSizes() ) {
			if( cameraPreviewSize == null ) {
				cameraPreviewSize = size;
			} else {
				int resultArea = cameraPreviewSize.width * cameraPreviewSize.height;
				int newArea = size.width * size.height;
				if( newArea > resultArea ) {
					cameraPreviewSize = size;
				}
			}
		}
		parameters.setPreviewSize( cameraPreviewSize.width, cameraPreviewSize.height );

		// Zoom
		parameters.setZoom( cameraZoom );
		// Buffer format. The only currently supported format is NV21
		parameters.setPreviewFormat( ImageFormat.NV21 );
		// Default focus mode
		parameters.setFocusMode( Camera.Parameters.FOCUS_MODE_AUTO );

		// The camera will fill the buffers with image data and send notifications through the callback.
		// The buffers will be sent to the camera on requests from recognition service (see implementation
		// of IDataCaptureService.Callback.onRequestLatestFrame above)
		camera.setPreviewCallbackWithBuffer( cameraPreviewCallback );

		// Camera sees it as rotated 90 degrees, so there's some confusion with what is width and what is height)
		int width = 0;
		int height = 0;
		switch( orientation ) {
			case 0:
			case 180:
				height = cameraPreviewSize.height;
				width = cameraPreviewSize.width;
				break;
			case 90:
			case 270:
				width = cameraPreviewSize.height;
				height = cameraPreviewSize.width;
				break;
		}

		// Configure the view scale and area of interest
		surfaceViewWithOverlay.setScaleX( surfaceViewWithOverlay.getWidth(), width );
		surfaceViewWithOverlay.setScaleY( surfaceViewWithOverlay.getHeight(), height );
		// Screen and preview coordinates systems are not equal. Moreover, their aspect ratios may be different.
		surfaceViewWithOverlay.setAreaOfInterest( new Rect( 0, 0, width, calculateAreaOfInterestHeight( width ) *
			height / width * surfaceViewWithOverlay.getWidth() / surfaceViewWithOverlay.getHeight() ) );

		// Commit the camera parameters
		camera.setParameters( parameters );

		// Start preview
		camera.startPreview();

		setCameraFocusMode( Camera.Parameters.FOCUS_MODE_AUTO );
		presenter.onCameraReady();
		inPreview = true;
	}

	@Override
	public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults )
	{
		switch( requestCode ) {
			case CAMERA_PERMISSION_REQUEST_CODE:
				if( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
					openCameraAndPreview();
				} else {
					showStartupError( "Camera is essential for this application." );
				}
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	// Focus camera and start recognition
	@Override
	public void focusCameraAndStartRecognition()
	{
		if( surfaceViewWithOverlay != null ) {
			surfaceViewWithOverlay.setStable( false );
		}
		autoFocus( startRecognitionCameraAutoFocusCallback );
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_camera );

		presenter = new PassportRecognitionPresenter();
		presenter.onCreateActivity( savedInstanceState, this );

		// Get references to some UI components
		warningTextView = (TextView) findViewById( R.id.warning_text );
		errorTextView = (TextView) findViewById( R.id.error_text );
		pictureImageView = (ImageView) findViewById( R.id.cameraOverlayPicture );

		// Manually create preview surface. The only reason for this is to
		// avoid making it a public top level class
		RelativeLayout layout = (RelativeLayout) findViewById( R.id.rootLayout );

		surfaceViewWithOverlay = new SurfaceViewWithOverlay( this );
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.MATCH_PARENT,
			RelativeLayout.LayoutParams.MATCH_PARENT );
		surfaceViewWithOverlay.setLayoutParams( params );
		// Add the surface to the layout as the bottom-most view filling the parent
		layout.addView( surfaceViewWithOverlay, 0 );
		surfaceViewWithOverlay.setOnClickListener( clickListener );
		findViewById( R.id.container ).setOnTouchListener( fragmentTouchListener );

		// Create data capture service
		if( createDataCaptureService() ) {
			// Set the callback to be called when the preview surface is ready.
			// We specify it as the last step as a safeguard so that if there are problems
			// loading the engine the preview will never start and we will never attempt calling the service
			surfaceViewWithOverlay.getHolder().addCallback( surfaceCallback );
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		presenter.onResumeActivity();

		if( ContextCompat.checkSelfPermission( this, Manifest.permission.CAMERA ) == PackageManager.PERMISSION_GRANTED ) {
			openCameraAndPreview();
		} else {
			if( !cameraPermissionRequested ) {
				ActivityCompat.requestPermissions( this, new String[] { Manifest.permission.CAMERA }, CAMERA_PERMISSION_REQUEST_CODE );
			}
			// After permission dialog is dismissed, onResume will be invoked again
			cameraPermissionRequested = true;
		}
	}

	@Override
	public void onPause()
	{
		// Clear all pending actions
		handler.removeCallbacksAndMessages( null );
		// Stop the data capture service
		if( dataCaptureService != null ) {
			dataCaptureService.stop();
		}
		stopPreviewAndReleaseCamera();
		presenter.onPauseActivity();
		super.onPause();
	}

	///////////////////////////////////////////////////////////////////////////////
	// Ниже располагается код, спецефичный для сценария распонзавания паспорта РФ.
	// The code below is specific for the scenario of capture of the Russian Passport
	///////////////////////////////////////////////////////////////////////////////

	// Presenter для распознавания паспорта РФ
	// Presenter for passport recognition
	private IPassportRecognitionPresenter presenter;

	final static String PROMPT_TYPE_TAG = "PromptMode";
	final static String PROMPT_FRAGMENT = "PromptFragment";
	final static String CURRENT_RESULTS_FRAGMENT = "CurrentResultsFragment";
	final static String FINAL_RESULTS_FRAGMENT = "FinalResultsFragment";

	@Override
	protected void onSaveInstanceState( Bundle outState )
	{
		super.onSaveInstanceState( outState );
		// Сохранение состояния, чтобы корректно работали операции жизненного цикла (сворачивания/разворачивания и т.п.)
		// State saving to provide correct activity lifecycle operation
		presenter.onSaveState( outState );
	}

	// По кнопке меню вызывается всплывающее окно настроек. В нем можно выбрать режим подсказки
	// Menu button invokes settings popup, which can be used to select the prompt type
	@Override
	public boolean onKeyUp( int keyCode, KeyEvent event )
	{
		if( keyCode == KeyEvent.KEYCODE_MENU ) {
			if( settingsPopup != null ) {
				return true;
			}
			LayoutInflater inflater = getLayoutInflater();
			View rootView = findViewById( R.id.container );
			View settingsView = inflater.inflate( R.layout.popup_settings, (ViewGroup) rootView, false );
			settingsPopup = new PopupWindow( settingsView, ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, false );
			settingsPopup.setOnDismissListener( new PopupWindow.OnDismissListener() {
				@Override public void onDismiss()
				{
					settingsPopup = null;
				}
			} );
			settingsView.setBackgroundColor( getResources().getColor( R.color.settings_background ) );
			settingsPopup.showAtLocation( rootView, Gravity.BOTTOM, 0, 0 );
			Spinner hintSpinner = (Spinner) settingsView.findViewById( R.id.hint_spinner );
			hintSpinner.setSelection( getSavedPromptType() );
			hintSpinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
				@Override public void onItemSelected( AdapterView<?> parent, View view, int position, long id )
				{
					PreferenceManager.getDefaultSharedPreferences( CameraActivity.this ).edit().
						putInt( PROMPT_TYPE_TAG, position ).apply();
					presenter.setPromptMode( position );
				}

				@Override public void onNothingSelected( AdapterView<?> parent )
				{

				}
			} );
			settingsView.findViewById( R.id.settings_ok_button ).setOnClickListener( new View.OnClickListener() {
				@Override public void onClick( View v )
				{
					settingsPopup.dismiss();
				}
			} );
			return true;
		}
		return super.onKeyUp( keyCode, event );
	}

	// Получение сохраненного в настройках режима подсказки
	// Prompt type saved in shared preferences
	@Override
	public int getSavedPromptType()
	{
		return PreferenceManager.getDefaultSharedPreferences( this ).getInt( PROMPT_TYPE_TAG,
			IPassportRecognitionPresenter.PromptMode.PromptBottom.ordinal() );
	}

	// Навигация на фрагмент с подсказкой. Первый аргумент - id картинки-подсказки,
	// второй - id картинки уже распознанной страницы, третий - текст подсказки
	// Navigation to the prompt fragment. The first argument is id of the prompt picture, the second - id of
	// recognized page picture, the third - prompt text
	@Override
	public IPromptFragment navigateToPromptFragment( @Nullable Integer promptPictureId, @Nullable Integer recognizedPagePictureId,
		@Nullable String text )
	{
		FragmentManager manager = getFragmentManager();
		PromptFragment promptFragment = (PromptFragment) manager.findFragmentByTag( PROMPT_FRAGMENT );
		if( promptFragment == null ) {
			promptFragment = PromptFragment.newInstance( promptPictureId, recognizedPagePictureId, text );
		} else {
			promptFragment.setPromptText( text );
			promptFragment.setPromptPictureId( promptPictureId );
			promptFragment.setRecognizedPagePictureId( recognizedPagePictureId );
		}
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.replace( R.id.container, promptFragment, PROMPT_FRAGMENT );
		transaction.addToBackStack( null );
		transaction.commit();
		return promptFragment;
	}

	// Переход на фрагмент с результатами в процессе распознавания
	// (в аргументах - id картинок с индикацией текущей страницы)
	// Navigation to the fragment with ongoing recognition results (argument)
	// (in arguments - pictures id of current page)
	@Override
	public ICurrentResultsFragment navigateToResultFragment( Integer pictureTopId, Integer pictureBottomId )
	{
		FragmentManager manager = getFragmentManager();
		CurrentResultsFragment resultFragment = (CurrentResultsFragment) manager.findFragmentByTag( CURRENT_RESULTS_FRAGMENT );
		if( resultFragment == null ) {
			resultFragment = com.abbyy.mobile.sample.passportreader.CurrentResultsFragment.newInstance( pictureTopId, pictureBottomId );
		} else {
			resultFragment.setTopPicture( pictureTopId );
			resultFragment.setBottomPicture( pictureBottomId );
		}
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.replace( R.id.container, resultFragment, CURRENT_RESULTS_FRAGMENT );
		transaction.addToBackStack( null );
		transaction.commit();

		resultFragment.setStable( false );
		resultFragment.setOnTouchListener( fragmentTouchListener );

		return resultFragment;
	}

	// Переход на фрагмент с окончательными результатами (в аргументах - результаты)
	// Navigation to the fragment with final results (results are in arguments)
	@Override
	public IFinalResultsFragment navigateToFinalResultsFragment( @NonNull ArrayList<String> names, @NonNull ArrayList<String> values )
	{
		FragmentManager manager = getFragmentManager();
		FinalResultsFragment finalResultsFragment = (FinalResultsFragment) manager.findFragmentByTag( FINAL_RESULTS_FRAGMENT );
		if( finalResultsFragment == null ) {
			finalResultsFragment = FinalResultsFragment.newInstance( names, values );
		} else {
			finalResultsFragment.setNamesAndValues( names, values );
		}
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.replace( R.id.container, finalResultsFragment, FINAL_RESULTS_FRAGMENT );
		transaction.addToBackStack( null );
		transaction.commit();
		return finalResultsFragment;
	}

	// Устанавливает, занимает ли фрагмент целиком экран или оставляет место для превью
	// The fragment is either fullscreen or some margin is left for the the preview
	@Override
	public void setPreviewVisible( final boolean visible )
	{
		View container = findViewById( R.id.container );
		// Отступ до контейнера фрагмента сверху
		// Top margin to the fragment container
		int topMargin = 0;
		if( visible ) {
			int width = surfaceViewWithOverlay.getWidth();
			if( width == 0 ) {
				// Если этот метод вызван из onResume, еще нет размеров, нужно отложить обработку
				// There is no size information yet (called from onResume).
				handler.post( new Runnable() {
					@Override public void run()
					{
						setPreviewVisible( visible );
					}
				} );
				return;
			} else {
				// Координаты экранные
				// Screen coordinates
				topMargin = calculateAreaOfInterestHeight( width );
			}
		}

		// Применяем ищменения layout
		// Apply layout changes
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		);
		params.setMargins( 0, topMargin, 0, 0 );
		container.setLayoutParams( params );
		container.requestLayout();
	}

	// Автофокусировка, вызванная пользователем
	// Manual autofocus (initiated by the user tapping the screen)
	@Override
	public void forceAutoFocus()
	{
		// Нельзя вызывать автофокус, если запущен автофокус со стартом распознавания после его окнчания.
		// Запрещаем более общий случай, нельзя вызвать два автофокуса одновременно.
		// В случае !disableContinuousAutofocus автофокус не вызываем, т.к. заново включать FOCUS_MODE_CONTINUOUS_VIDEO

		// Only one autofocus can be called at a time. When inside autofocus callback, it is forbidden to call
		// autofocus from within. If disableContinuousAutofocus is false, ignore autofocus as it will require setting
		// FOCUS_MODE_CONTINUOUS_VIDEO again
		if( !isAutofocusInProgress ) {
			autoFocus( emptyAutoFocusCallback );
		}
	}

	// Обработчик нажатий на экран (в любой точке)
	// Listener for clicks (taps) on the screen
	private View.OnClickListener clickListener = new View.OnClickListener() {
		@Override public void onClick( View v )
		{
			if( settingsPopup != null ) {
				settingsPopup.dismiss();
			} else {
				presenter.onClick();
			}
		}
	};

	// Обработчик нажатий на фрагмент с подсказкой и результатами.
	// Обеспечивает ручное управление временем остановки распознавания
	// Touch events listener for the fragment with prompt and results
	// Useful for manually controlling when to stop collecting frames (the result looks good)
	private View.OnTouchListener fragmentTouchListener = new View.OnTouchListener() {
		// Время начала нажатия на view. Null если не нажато
		// Touch start time stamp. Null, if not pressed
		private Long touchStartTime = null;

		@Override public boolean onTouch( View v, MotionEvent event )
		{
			switch( event.getAction() ) {
				case MotionEvent.ACTION_DOWN:
					touchStartTime = SystemClock.elapsedRealtime();
					// Отключаем автоматическую остановку (основанную на стабильности результатов)
					// Disable automatic stopping (automatic stopping based on results stability)
					presenter.setManualRecognitionControl( true );
					return true;
				case MotionEvent.ACTION_CANCEL:
					presenter.setManualRecognitionControl( false );
					touchStartTime = null;
					return true;
				case MotionEvent.ACTION_UP: {
					long time = SystemClock.elapsedRealtime();
					if( touchStartTime != null ) {
						// Короткое нажатие считается кликом (вызовом фокусировки), а не управлением остановкой.
						// Quick tap is interpreted as a click (autofocus invocation), not manual recognition control
						if( ( time - touchStartTime ) > MANUAL_STOPPING_CONTROL_THRESHOLD_MILLISECONDS ) {
							presenter.userFinishRecognition();
						} else {
							clickListener.onClick( v );
						}
						touchStartTime = null;
					}
					presenter.setManualRecognitionControl( false );
					return true;
				}
				default:
					return false;
			}
		}
	};

	// Вычисление высоты области интереса (и видимого превью) по её ширине. Размеры в экранных координатах
	// Calculating the height of the area of interest (and visible preview) by its width (in screen coordinates)
	private int calculateAreaOfInterestHeight( int width )
	{
		return (int) ( width / aspectRatio );
	}

	@Override
	public void onBackPressed()
	{
		if( settingsPopup != null ) {
			settingsPopup.dismiss();
		} else {
			presenter.onBackButtonPressed();
		}
	}

	// Вибрация телефона
	// The phone vibration
	@Override
	public void vibrate( int milliseconds )
	{
		( (Vibrator) this.getSystemService( Context.VIBRATOR_SERVICE ) ).vibrate( milliseconds );
	}

	// Управление рисование рамки в превью: наличие символической фотографии
	// Enable or disable drawing an iconic photo of russian passport on top of the preview
	@Override
	public void setDrawPhotoInPreview( boolean newValue )
	{
		surfaceViewWithOverlay.setDrawPhotoInPreview( newValue );
		surfaceViewWithOverlay.invalidate();
	}

	// Управление рисованием рамки в превью: стабильность
	// Preview frame drawing control: stability
	@Override
	public void setStable( boolean value )
	{
		surfaceViewWithOverlay.setStable( value );
	}

	// Установка id картинки подсказки поверх превью
	// Overlay prompt picture id setter
	@Override
	public void setOverlayPromptImageId( Integer pictureId )
	{
		// В режиме подсказки PromptMode.PromptTop и PromptMode.Tutorial отображается картинка-подсказка поверх превью
		// камеры. Id этого изображения задается в этом методе (null, если нет изображения)
		// In PromptMode.PromptTop and PromptMode.Tutorial modes overlay prompt image displayed on preview.
		// This method sets the id of the image (null, if image not required)
		if( pictureId == null ) {
			pictureImageView.setImageDrawable( null );
		} else {
			pictureImageView.setImageDrawable( getResources().getDrawable( pictureId ) );
		}
	}

	// Установка отступа нарисованной рамки в превью
	// Preview overlay frame padding setter
	@Override
	public void setFramePaddingInPreview( float framePadding )
	{
		surfaceViewWithOverlay.setFramePaddingInPreview( framePadding );
	}

	@Override
	public Context getContext()
	{
		return this;
	}
}