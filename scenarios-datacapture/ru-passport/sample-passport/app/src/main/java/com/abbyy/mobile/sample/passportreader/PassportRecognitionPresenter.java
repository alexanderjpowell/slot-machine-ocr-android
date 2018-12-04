// ABBYY Real-Time Recognition SDK 1 © 2016 ABBYY Production LLC
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.
package com.abbyy.mobile.sample.passportreader;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.abbyy.mobile.rtr.IDataCaptureService;
import com.abbyy.mobile.rtr.IRecognitionService;
import com.abbyy.mobile.sample.passportreader.interfaces.ICameraActivity;
import com.abbyy.mobile.sample.passportreader.interfaces.ICurrentResultsFragment;
import com.abbyy.mobile.sample.passportreader.interfaces.IPassportRecognitionPresenter;
import com.abbyy.mobile.sample.passportreader.interfaces.IPassportRecognitionResults;
import com.abbyy.mobile.sample.passportreader.interfaces.IPromptFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

// Presenter, отвечающий за распознавание паспорта
// Presenter for passport RU recognition
class PassportRecognitionPresenter implements IPassportRecognitionPresenter {

	private static final String TAG_STATE = "PresenterState";
	// Порог количества кадров, после которого показываем сообщение R.string.wrong_document_warning_message
	// Frame count threshold after which R.string.wrong_document_warning_message notification is displayed
	private static final int SCHEME_NOT_MATCHED_THRESHOLD = 10;
	// Задержка после окончания распознавания страницы перед переключением на следующий этап
	// The delay after recognition finishing before next step
	private static final int RECOGNIZED_PAGE_SCREEN_DELAY_MILLISECONDS = 2000;
	// Время вибрации после окончания распознавания страницы
	// Vibration time. The vibration after finishing of page recognition
	private static final int PAGE_RECOGNIZED_VIBRATE_MILLISECONDS = 500;

	// Вызывается из CameraActivity.onCreate
	// Called from CameraActivity.onCreate
	@Override
	public void onCreateActivity( @Nullable Bundle savedInstanceState, @NonNull ICameraActivity activity )
	{
		cameraActivity = new WeakReference<>( activity );
		results = new PassportRecognitionResults( this );
		if( savedInstanceState == null ) {
			state = States.PromptDisplayed;
		} else {
			state = States.valueOf( savedInstanceState.getString( TAG_STATE ) );
		}
		results.load( savedInstanceState );
		handler = new Handler();
		isRunningActivity = false;
		manualRecognitionControl = false;
		promptMode = PromptMode.values()[activity.getSavedPromptType()];
	}

	// Вызывается из CameraActivity.onResume
	// Called from CameraActivity.onResume
	@Override
	public void onResumeActivity()
	{
		switch( state ) {
			case PageRecognitionInProgress:
				state = States.PromptDisplayed;
				showPrompt( false );
				break;
			case PromptDisplayed:
			case PromptSecondStep:
			case WaitingResults:
				state = States.PromptDisplayed;
				showPrompt( false );
				break;
			case PageRecognized:
				navigateNextStateAfterPageRecognition( false );
				break;
			case ResultDisplayed:
				break;
			default:
				throw new IllegalStateException();
		}
		isRunningActivity = true;
		manualRecognitionControl = false;
	}

	// Вызывается из CameraActivity.onPause
	// Called from CameraActivity.onPause
	@Override
	public void onPauseActivity()
	{
		isRunningActivity = false;
	}

	// Вызывается из CameraActivity.onSaveInstanceState
	// Called from CameraActivity.onSaveInstanceState
	@Override
	public void onSaveState( Bundle outState )
	{
		outState.putString( TAG_STATE, state.toString() );
		results.save( outState );
	}

	// DataCaptureService остановился
	// DataCaptureService has stopped
	@Override
	public void onRecognitionServiceStopped()
	{
		// Позволяем переключиться на следюущий шаг по тапу и по истечении RECOGNIZED_PAGE_SCREEN_DELAY_MILLISECONDS мс
		// Next step by tap or after RECOGNIZED_PAGE_SCREEN_DELAY_MILLISECONDS milliseconds
		if( state == States.PageRecognized ) {
			handler.postDelayed( new Runnable() {
				@Override public void run()
				{
					if( isRunningActivity ) {
						navigateNextStateAfterPageRecognition( true );
					}
				}
			}, RECOGNIZED_PAGE_SCREEN_DELAY_MILLISECONDS );
		}
	}

	// Получены результаты от DataCaptureService
	// The results obtained from DataCaptureService
	@Override
	public void onFrameProcessed( IDataCaptureService.DataScheme scheme, IDataCaptureService.DataField[] fields,
		IRecognitionService.ResultStabilityStatus resultStatus )
	{
		switch( state ) {
			case PageRecognitionInProgress:
				processFrame( scheme, fields, resultStatus );
				break;
			case PromptDisplayed:
			case PromptSecondStep:
			case WaitingResults:
				processFrameBeforeSchemeMatched( scheme, fields, resultStatus );
				break;
			case PageRecognized:
			case ResultDisplayed:
				// Когда страница распознана, но DataCaptureService еще не остановился (его остановка асинхронна),
				// результаты могут еще приходить. Проигнорируем их.
				// When the page is recognized but DataCaptureService has not stopped results can be obtained. Just
				// ignore them
				break;
			default:
				throw new IllegalStateException();
		}
	}

	// Добавляет новое распознанное поле в UI результатов
	// Inserts new recognized field in the results UI
	@Override
	public void insertField( int index, String name, String text )
	{
		currentResultsFragment.get().insertField( index, name, text );
	}

	// Обновляет значение распознанного поля в UI результатов
	// Updates the value of the recognized field in the results UI
	@Override
	public void updateFieldValue( int index, String text )
	{
		currentResultsFragment.get().updateFieldValue( index, text );
	}

	// Удаляет распознанное поле из UI результатов
	// Removes the recognized field from the results UI
	@Override
	public void removeFieldAt( int index )
	{
		currentResultsFragment.get().removeFieldAt( index );
	}

	// Камера инициализирована, настроена, сфокусирована и готова к распознаванию
	// The camera is initialized, configured, focused and ready for recognition
	@Override
	public void onCameraReady()
	{
		if( state == States.PromptDisplayed ) {
			cameraActivity.get().focusCameraAndStartRecognition();
		}
	}

	// Обработка нажатия кнопки назад в зависмости от состояния
	// Back button pressure processing
	@Override
	public void onBackButtonPressed()
	{
		if( state == States.ResultDisplayed ) {
			restartRecognition();
		} else {
			cameraActivity.get().finish();
		}
	}

	// Очистка результатов распознавания на фрагменте результатов
	// Clears the results in ResultsFragment instance
	@Override
	public void clearRecognitionResult()
	{
		currentResultsFragment.get().clearResult();
	}

	// Обработка тапа (короткого по времени касания) по экрану
	// Click processing (quick tap)
	@Override
	public void onClick()
	{
		// Для режима подсказки поверх превью (и первого цикла туториала) - обеспечиваем переключение по циклу состояний.
		// Кроме того, обеспечиваем переход с экрана результатов распознавания страницы на следующий шаг. В остальных
		// случаях вызываем автофокус.
		// For the PromptMode.PromptTop (and PromptMode.Tutorial for the first cycle) - changes state.
		// For the page with recognition result state - moves to next step.
		// Another cases - invokes autofocus.
		switch( state ) {
			case PromptDisplayed:
				if( isTopPromptMode() ) {
					state = States.PromptSecondStep;
					String prompt;
					if( results.hasBottomPage() ) {
						prompt = cameraActivity.get().getContext().getString( R.string.scan_top_page_text_message );
					} else {
						prompt = cameraActivity.get().getContext().getString( R.string.scan_bottom_page_text_message );
						if( isTopPromptMode() ) {
							cameraActivity.get().setOverlayPromptImageId( R.drawable.ic_passport_bottom_cropped_huge );
						}
					}
					promptFragment.get().setPromptText( prompt );
				} else {
					cameraActivity.get().forceAutoFocus();
				}
				break;
			case PromptSecondStep: {
				state = States.WaitingResults;
				String prompt;
				if( results.hasBottomPage() ) {
					prompt = cameraActivity.get().getContext().getString( R.string.final_scan_top_page_text_message );
				} else {
					prompt = cameraActivity.get().getContext().getString( R.string.final_scan_bottom_page_text_message );
				}
				cameraActivity.get().setOverlayPromptImageId( null );
				promptFragment.get().setPromptText( prompt );
				schemeNotMatchedFrameCount = 0;
				break;
			}
			case PageRecognized:
				navigateNextStateAfterPageRecognition( true );
				break;
			default:
				cameraActivity.get().forceAutoFocus();
				break;
		}
	}

	// Установка режима подсказки
	// The prompt mode setter
	@Override
	public void setPromptMode( int type )
	{
		promptMode = PromptMode.values()[type];
		restartRecognition();
	}

	// Остановка распознавания (в режиме ручного управления временем остановки распознавания)
	// Recognition stopping (in manual stopping time mode)
	@Override
	public void userFinishRecognition()
	{
		if( !manualRecognitionControl ) {
			throw new IllegalStateException( "Must be in user finish recognition control mode" );
		}
		// Проверяем, что можно досрочно завершить распознавание, а также, что распознавание еще идет.
		// Check that recognition stopping is possible and recognition is in progress.
		if( state == States.PageRecognitionInProgress && lastScheme != null ) {
			finishRecognition();
		}
	}

	// Состояния приложения. Реализуется машина состояний, обеспечивающая правильную работу.
	// Application state. This application is state machine.
	private enum States {
		// Отображение подсказки (распознавание запущено, но результатов еще нет)
		// The prompt is displayed (recognition is active but there is no result)
		PromptDisplayed,
		// Второй шаг подсказки, если isTopPromptMode()
		// the second step of the prompt if isTopPromptMode()
		PromptSecondStep,
		// Подсказку закрыли, но результатов еще нет (isTopPromptMode())
		// Prompt closed, but there are no result (isTopPromptMode())
		WaitingResults,
		// Получены результаты распознавания паспорта, но результат не стабилен и пользователь не завершил
		// распознавание в ручном режиме
		// The passport recognition results obtained, but the results are not stable and user has not finished
		// recognition in manual mode
		PageRecognitionInProgress,
		// Страница распознана, показываются результаты (распознавание останавливается или остановлено)
		// The page is recognized, the results are displayed (recognition is stopping or has been stopped)
		PageRecognized,
		// Обе страницы распознаны, показываются результаты (распознавание останавливается или остановлено)
		// Both pages are recognized, the results are displayed (recognition is stopping or has been stopped)
		ResultDisplayed

		// Реализуемые переходы машины состояний
		// State machine transitions
		//
		// PromptDisplayed -> PageRecognitionInProgress (results obtained)
		// PageRecognitionInProgress -> PageRecognized
		// PageRecognized -> PromptDisplayed (only one page is recognized)
		// PageRecognized -> ResultDisplayed (both pages are recognized)
		// ResultDisplayed -> PromptDisplayed (back key or retry button pressed)
		//
		// PromptDisplayed -> PromptSecondStep (only if isTopPromptMode())
		// PromptSecondStep -> WaitingResults
		// WaitingResults -> PageRecognitionInProgress
		// PromptSecondStep -> PageRecognitionInProgress
		//
		// PageRecognitionInProgress -> PromptDisplayed (onPause() -> onResume())
		// PromptSecondStep -> PromptDisplayed (onPause() -> onResume())
		// WaitingResults -> PromptDisplayed (onPause() -> onResume())
	}

	// Установка ручного режима остановки распознавания (по долгому нажатию, а не по стабилизации)
	// Sets manual recognition control (by long tap, not by stability)
	@Override public void setManualRecognitionControl( boolean newValue )
	{
		this.manualRecognitionControl = newValue;
	}

	// UI-компоненты, с которыми взаимодействем
	// UI with which we have interaction
	private WeakReference<ICameraActivity> cameraActivity;
	private WeakReference<IPromptFragment> promptFragment;
	private WeakReference<ICurrentResultsFragment> currentResultsFragment;

	// Текущее состояние в машине состояний
	// Current state in state machine
	private States state;
	// Текущий режим подсказки
	// Current prompt mode
	private PromptMode promptMode;

	// Результаты распознавания (модель)
	// Recognition results (model)
	private IPassportRecognitionResults results;
	// Количество кадров, на которых схема не наложилась (для вывода предупреждения)
	// Number of frames on which scheme did not matched (for warning)
	private int schemeNotMatchedFrameCount = 0;
	// Идентификатор схемы, которую сейчас распознаем. Схема задается документом,
	// который распознался DataCaptureService.
	// Scheme identifier which we are currently recognizing. Scheme is defined by the document type
	// recognized by DataCaptureService.
	private String lastScheme = null;
	// Режим ручной установки распознавания
	// Manual finishing control mode
	private boolean manualRecognitionControl = false;
	// Вспомогательные переменные
	// Auxiliary variables
	private Handler handler;
	private boolean isRunningActivity = false;

	// Взаимодействие с фрагментом FinalResultsFragment
	// Interaction with FinalResultsFragment
	private FinalResultsFragment.OnInteractionCallback finalResultsCallback =
		new FinalResultsFragment.OnInteractionCallback() {
			@Override public void onRetryButtonPressed()
			{
				restartRecognition();
			}
		};

	// Переключение на экран результатов распознавания в процессе (состояние PageRecognitionInProgress)
	// Navigation to the results displaying fragment (state PageRecognitionInProgress)
	private void showResultInProgress( String scheme )
	{
		Integer top = getPictureTopId( scheme );
		Integer bottom = getPictureBottomId( scheme );
		currentResultsFragment = new WeakReference<>( cameraActivity.get().navigateToResultFragment( top, bottom ) );
		cameraActivity.get().setPreviewVisible( true );
	}

	// Отображение экрана подсказки
	// Prompt screen displaying
	private void showPrompt( boolean startRecognition )
	{
		// Элементы подсказки
		// Prompt elements
		Integer imageBottom = null;
		Integer imageTop = null;
		String text = null;
		boolean hasPhoto = false;
		float framePadding = 0;

		// Настройка картинок и сообщений в зависимости от режима подсказки
		// Configuring of pictures and messages
		switch( promptMode ) {
			case PromptBottom:
				if( results.hasBottomPage() ) {
					imageBottom = R.drawable.ic_passport_top;
					text = cameraActivity.get().getContext().getString( R.string.scan_top_page_message );
					hasPhoto = false;
				} else {
					imageBottom = R.drawable.ic_passport_bottom;
					text = cameraActivity.get().getContext().getString( R.string.scan_bottom_page_message );
					// В режиме с распознаванием полной страницы (а не только текста справа) тут должно быть true
					// Рекомендуется просить пользователя навести только на текст (false)
					// For prompt asking for the full-page recognition (not only text on the left) set this true
					// We recommend to set false and ask user to scan only text on the right part of passport
					hasPhoto = false;
				}
				framePadding = cameraActivity.get().getContext().getResources().getDimension( R.dimen.frame_padding );
				break;
			case Tutorial:
			case PromptTop:
				if( results.hasBottomPage() ) {
					text = cameraActivity.get().getContext().getString( R.string.open_top_page_message );
					imageTop = R.drawable.ic_passport_top_huge;
				} else {
					text = cameraActivity.get().getContext().getString( R.string.open_bottom_page_message );
					imageTop = R.drawable.ic_passport_bottom_huge;
				}
				imageBottom = null;
				hasPhoto = false;
				framePadding = 0;
				break;
		}

		Integer recognizedPageImageId = null;
		if( results.hasBottomPage() ) {
			recognizedPageImageId = R.drawable.ic_bottom_page_finish;
		} else {
			if( results.hasTopPage() ) {
				recognizedPageImageId = R.drawable.ic_top_page_finish;
			}
		}

		// Обновление UI
		// Updating UI
		promptFragment = new WeakReference<>( cameraActivity.get().navigateToPromptFragment(
			imageBottom, recognizedPageImageId, text ) );
		cameraActivity.get().setPreviewVisible( true );
		cameraActivity.get().setDrawPhotoInPreview( hasPhoto );
		cameraActivity.get().setOverlayPromptImageId( imageTop );
		cameraActivity.get().setFramePaddingInPreview( framePadding );
		schemeNotMatchedFrameCount = 0;
		lastScheme = null;
		if( startRecognition ) {
			cameraActivity.get().focusCameraAndStartRecognition();
		}
	}

	// Текущий режим подсказки в верхней части экрана (поверх превью)
	// Current prompt mode is picture overlaying preview
	private boolean isTopPromptMode()
	{
		return promptMode == PromptMode.PromptTop || promptMode == PromptMode.Tutorial;
	}

	// Проверка на то, что схема допустима в текущем шаге распознавания (не распознаем одну страницу дважды)
	// Checks that the scheme is valid on current recognition step (the page can be recognized only once)
	private boolean checkValidScheme( String id )
	{
		if( results.hasBottomPage() || results.hasTopPage() ) {
			if( results.hasBottomPage() && results.hasTopPage() ) {
				throw new IllegalStateException( "This method can be called, when there is not-recognized page" );
			}
			return ( isBottomPage( id ) && results.hasTopPage() ) || ( isTopPage( id ) && results.hasBottomPage() );
		}
		// Любая схема допустима, если ни одну страницу пока не распознали
		// If no page is recognized either scheme will be available
		return true;
	}

	// Обработка кадра, на котором схема не наложилась
	// Processes frame with no scheme matched
	private void processFrameBeforeSchemeMatched( IDataCaptureService.DataScheme scheme,
		IDataCaptureService.DataField[] fields, IRecognitionService.ResultStabilityStatus resultStatus )
	{
		if( scheme != null ) {
			if( checkValidScheme( scheme.Id ) ) {
				state = States.PageRecognitionInProgress;
				lastScheme = scheme.Id;
				showResultInProgress( scheme.Id );
				processFrame( scheme, fields, resultStatus );
				cameraActivity.get().setOverlayPromptImageId( null );
			}
			schemeNotMatchedFrameCount = 0;
		} else {
			++schemeNotMatchedFrameCount;
			// Долго не накладывается схема. Возможно, пользователь навел камеру не на паспорт,
			// или снимает его в неправильных условиях.
			// The scheme cannot be matched for a long time. Perhaps user isn't pointing the camera at the passport or
			// is working in the wrong light conditions.
			if( schemeNotMatchedFrameCount > SCHEME_NOT_MATCHED_THRESHOLD &&
				( promptMode == PromptMode.PromptBottom || state == States.WaitingResults ) ) {
				if( ( schemeNotMatchedFrameCount - SCHEME_NOT_MATCHED_THRESHOLD ) %
					( 3 * SCHEME_NOT_MATCHED_THRESHOLD ) == 0 ) {
					cameraActivity.get().forceAutoFocus();
					Toast.makeText( cameraActivity.get().getContext(), cameraActivity.get().getContext().getString(
						R.string.wrong_document_warning_message ), Toast.LENGTH_SHORT ).show();
				}
			}
		}
	}

	// Обработка кадра с наложившейся схемой
	// Processing frame with matched scheme
	private void processFrame( IDataCaptureService.DataScheme scheme, IDataCaptureService.DataField[] fields,
		IRecognitionService.ResultStabilityStatus resultStatus )
	{
		if( scheme == null ) {
			// Возможны ситуации, когда схема перестала накладываться (навели телефон на документ, не
			// дождавшись стабилизации, навели телефон на другой текст). Просто проигнорируем подобные кадры
			// It is possible what there is frame with no scheme after frames with matched scheme (for example, it will
			// be possible if the camera is pointed at passport, then moved to another text before stable results).
			// Just ignore those frames.
			return;
		}

		// Каждую страницу можно распознать только один раз. Это сделано для улучшения usability: не все успевают
		// перевести телефон с одной страницы на другую
		// Every page can be recognized only once for better usability. Not all users have time to move
		// the camera to another page after the first page is recognized
		if( !scheme.Id.equals( lastScheme ) ) {
			if( checkValidScheme( scheme.Id ) ) {
				setPictures( scheme.Id );
				lastScheme = scheme.Id;
			} else {
				// Не перераспознаем второй раз страницу
				// Page can be recognized only once
				return;
			}
		}
		results.addFrame( scheme, fields );
		// Критерии остановки распознавания
		// Recognition finishing criteria
		if( resultStatus == IRecognitionService.ResultStabilityStatus.Stable && ( !manualRecognitionControl ) ) {
			finishRecognition();
		}
	}

	// Остановка распознавания
	// Recognition finishing
	private void finishRecognition()
	{
		results.onRecognitionCompleted( lastScheme );
		state = States.PageRecognized;
		setPictures( null );
		currentResultsFragment.get().setStable( true );
		cameraActivity.get().stopRecognition();
		cameraActivity.get().setStable( true );
		cameraActivity.get().vibrate( PAGE_RECOGNIZED_VIBRATE_MILLISECONDS );
	}

	// Настройка картинок на экране результатов в процессе распознавания
	// Configures pictures on results fragment
	private void setPictures( String scheme )
	{
		Integer top = getPictureTopId( scheme );
		Integer bottom = getPictureBottomId( scheme );
		currentResultsFragment.get().setTopPicture( top );
		currentResultsFragment.get().setBottomPicture( bottom );
	}

	@Nullable private Integer getPictureBottomId( String scheme )
	{
		Integer bottom = null;
		if( results.hasBottomPage() ) {
			bottom = R.drawable.ic_bottom_page_finish;
		}
		if( scheme != null ) {
			if( isBottomPage( scheme ) ) {
				bottom = R.drawable.ic_bottom_page_progress;
			}
		}
		return bottom;
	}

	@Nullable private Integer getPictureTopId( String scheme )
	{
		Integer top = null;
		if( results.hasTopPage() ) {
			top = R.drawable.ic_top_page_finish;
		}
		if( scheme != null ) {
			if( isTopPage( scheme ) ) {
				top = R.drawable.ic_top_page_progress;
			}
		}
		return top;
	}

	// Является ли данная схема схемой нижней страинцы
	// Is that scheme of bottom page
	private boolean isBottomPage( String scheme )
	{
		return scheme.equals( PassportRecognitionResults.BOTTOM_PAGE_ID );
	}

	// Является ли данная схема схемой верхней страинцы
	// Is that scheme of top page
	private boolean isTopPage( String scheme )
	{
		return scheme.equals( PassportRecognitionResults.TOP_PAGE_ID );
	}

	// Переключение на экран со всеми результатами (состояние ResultDisplayed)
	// Navigation to the page with all result (state ResultDisplayed)
	private void displayAllResults()
	{
		ArrayList<String> names = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		results.exportFieldNamesAndValues( names, values );
		cameraActivity.get().navigateToFinalResultsFragment( names, values ).setCallback( finalResultsCallback );
		cameraActivity.get().setPreviewVisible( false );
	}

	// Переключение на следующий шаг после успешного распознавания странцы
	// Transition to next step after successful page recognition
	private void navigateNextStateAfterPageRecognition( boolean startNow )
	{
		if( allResultsReady() ) {
			state = States.ResultDisplayed;
			if( promptMode == PromptMode.Tutorial ) {
				promptMode = PromptMode.PromptBottom;
			}
			displayAllResults();
		} else {
			state = States.PromptDisplayed;
			showPrompt( startNow );
		}
	}

	// Проверка, что все результаты есть
	// All results are ready
	private boolean allResultsReady()
	{
		return results.hasBottomPage() && results.hasTopPage();
	}

	// Перезапуск распознавания паспорта
	// Restarts recognition of the passport
	private void restartRecognition()
	{
		results = new PassportRecognitionResults( this );
		state = States.PromptDisplayed;
		showPrompt( true );
	}
}
