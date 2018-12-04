// ABBYY Real-Time Recognition SDK 1 © 2016 ABBYY Production LLC
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.
package com.abbyy.mobile.sample.passportreader.interfaces;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

// Активити с превью камеры - основной экран. Здесь происходит взаимодействие с ABBYY Real-Time Recognition SDK
// The activity with camera preview is the main screen. Here there is all interaction with ABBYY Real-Time Recognition SDK
public interface ICameraActivity {
	// Остановка распознавания
	// Stop recognition
	void stopRecognition();

	// Фокусировка камеры и запуск распознавания
	// Focus camera and start recognition
	void focusCameraAndStartRecognition();

	// Получение сохраненного в настройках режима подсказки
	// Prompt type saved in shared preferences
	int getSavedPromptType();

	// Навигация на фрагмент с подсказкой. Первый аргумент - id картинки-подсказки,
	// второй - id картинки уже распознанной страницы, третий - текст подсказки
	// Navigation to the prompt fragment. The first argument is id of the prompt picture, the second - id of
	// recognized page picture, the third - prompt text
	IPromptFragment navigateToPromptFragment( @Nullable Integer promptPictureId, @Nullable Integer recognizedPagePictureId,
		@Nullable String text );

	// Переход на фрагмент с результатами в процессе распознавания
	// (в аргументах - id картинок с индикацией текущей страницы)
	// Navigation to the fragment with ongoing recognition results (argument)
	// (in arguments - pictures id of current page)
	ICurrentResultsFragment navigateToResultFragment( Integer pictureTopId, Integer pictureBottomId );

	// Переход на фрагмент с окончательными результатами (в аргументах - результаты)
	// Navigation to the fragment with final results (results are in arguments)
	IFinalResultsFragment navigateToFinalResultsFragment( @NonNull ArrayList<String> names, @NonNull ArrayList<String> values );

	// Устанавливает, занимает ли фрагмент целиком экран или оставляет место для превью
	// The fragment is either fullscreen or some margin is left for the the preview
	void setPreviewVisible( boolean visible );

	// Автофокусировка, вызванная пользователем
	// Manual autofocus (initiated by the user tapping the screen)
	void forceAutoFocus();

	// Вибрация телефона
	// The phone vibration
	void vibrate( int milliseconds );

	// Управление рисование рамки в превью: наличие символической фотографии
	// Enable or disable drawing an iconic photo of russian passport on top of the preview
	void setDrawPhotoInPreview( boolean newValue );

	// Управление рисованием рамки в превью: стабильность
	// Preview frame drawing control: stability
	void setStable( boolean value );

	// Установка id картинки подсказки поверх превью
	// Overlay prompt picture id setter
	void setOverlayPromptImageId( Integer pictureId );

	// Установка отступа нарисованной рамки в превью
	// Preview overlay frame padding setter
	void setFramePaddingInPreview( float framePadding );

	// Получает котекст
	// Returns context
	Context getContext();

	// Закрыть activity
	// Finish activity
	void finish();
}
