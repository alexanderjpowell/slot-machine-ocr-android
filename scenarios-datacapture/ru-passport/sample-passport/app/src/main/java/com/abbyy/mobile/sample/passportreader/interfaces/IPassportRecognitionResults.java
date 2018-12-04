// ABBYY Real-Time Recognition SDK 1 © 2016 ABBYY Production LLC
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.
package com.abbyy.mobile.sample.passportreader.interfaces;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.abbyy.mobile.rtr.IDataCaptureService;

import java.util.ArrayList;
import java.util.List;

// Результаты распознавания паспорта
// Passport recognition results
public interface IPassportRecognitionResults {
	// Загрузка из bundle (для корректного поведения при событиях android activity lifecycle)
	// Loading from bundle (for correct interaction on android activity lifecycle events)
	void load( @Nullable Bundle savedInstanceState );

	// Сохранение в bundle (для корректного поведения при событиях android activity lifecycle)
	// Saving to bundle (for correct interaction on android activity lifecycle events)
	void save( @NonNull Bundle instance );

	// Распознали ли нижнюю страницу
	// Is bottom page recognized
	boolean hasBottomPage();

	// Распознали ли верхнюю страницу
	// Is top page recognized
	boolean hasTopPage();

	// Получает имена распознанных полей
	// Exports names of recognized fields
	ArrayList<String> exportFieldNames();

	// Получает значения распознанных полей
	// Exports values of recognized fields
	ArrayList<String> exportFieldValues();

	// Получает имена и значения распознанных полей
	// Exports names and values of recognized fields
	void exportFieldNamesAndValues( List<String> names, List<String> values );

	// Добавление кадра и обновление результата
	// Recognized frame addition and results updating
	void addFrame( IDataCaptureService.DataScheme scheme, IDataCaptureService.DataField[] fields );

	// Уведомление об остановке распознавания
	// Notification about recognition completion
	void onRecognitionCompleted( String scheme );
}
