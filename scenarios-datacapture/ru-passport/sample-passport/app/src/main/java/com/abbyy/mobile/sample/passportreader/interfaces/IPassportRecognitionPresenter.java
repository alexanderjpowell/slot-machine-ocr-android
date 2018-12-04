// ABBYY Real-Time Recognition SDK 1 © 2016 ABBYY Production LLC
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.
package com.abbyy.mobile.sample.passportreader.interfaces;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.abbyy.mobile.rtr.IDataCaptureService;
import com.abbyy.mobile.rtr.IRecognitionService;

// Presenter, отвечающий за распознавание паспорта
// Presenter for passport RU recognition
public interface IPassportRecognitionPresenter {
	// Вызывается из CameraActivity.onCreate
	// Called from CameraActivity.onCreate
	void onCreateActivity( @Nullable Bundle savedInstanceState, @NonNull ICameraActivity activity );

	// Вызывается из CameraActivity.onResume
	// Called from CameraActivity.onResume
	void onResumeActivity();

	// Вызывается из CameraActivity.onPause
	// Called from CameraActivity.onPause
	void onPauseActivity();

	// Вызывается из CameraActivity.onSaveInstanceState
	// Called from CameraActivity.onSaveInstanceState
	void onSaveState( Bundle outState );

	// DataCaptureService остановился
	// DataCaptureService has stopped
	void onRecognitionServiceStopped();

	// Получены результаты от DataCaptureService
	// The results obtained from DataCaptureService
	void onFrameProcessed( IDataCaptureService.DataScheme scheme, IDataCaptureService.DataField[] fields,
		IRecognitionService.ResultStabilityStatus resultStatus );

	// Добавляет новое распознанное поле в UI результатов
	// Inserts new recognized field in the results UI
	void insertField( int index, String name, String text );

	// Обновляет значение распознанного поля в UI результатов
	// Updates the value of the recognized field in the results UI
	void updateFieldValue( int index, String text );

	// Удаляет распознанное поле из UI результатов
	// Removes the recognized field from the results UI
	void removeFieldAt( int index );

	// Камера инициализирована, настроена, и готова к распознаванию
	// The camera is initialized, configured, and ready for recognition
	void onCameraReady();

	// Обработка нажатия кнопки назад в зависмости от состояния
	// Back button pressure processing
	void onBackButtonPressed();

	// Очистка результатов распознавания на фрагменте результатов
	// Clears the results in ResultsFragment instance
	void clearRecognitionResult();

	// Обработка тапа (короткого по времени касания) по экрану
	// Click processing (quick tap)
	void onClick();

	// Установка режима подсказки
	// The prompt mode setter
	void setPromptMode( int type );

	// Остановка распознавания (в режиме ручного управления временем остановки распознавания)
	// Recognition stopping (in manual stopping time mode)
	void userFinishRecognition();

	// Установка ручного режима остановки распознавания (по долгому нажатию, а не по стабилизации)
	// Sets manual recognition control (by long tap, not by stability)
	void setManualRecognitionControl( boolean newValue );


	// Режим подсказки. Подсказки: способ показа сообщений и картинок пользователю с целью показать,
	// как пользоваться приложением.
	// Prompt mode. Prompt mode is method of displaying messages and pictures which help user to use application.
	enum PromptMode {
		// После запуска приложения первый цикл работы как PromptTop, оситальные как PromptBottom
		// The first working cycle as PromptTop, later cycles as PromptBottom
		Tutorial,
		// Подсказка в виде изображения поверх превью (текст в нижней части экрана)
		// Prompt image overlays preview (text at bottom part of the screen)
		PromptTop,
		// И картинка, и текст отображаются в нижней части экрана
		// Both the text at the picture are displayed at the bottom part of screen
		PromptBottom
	}
}
