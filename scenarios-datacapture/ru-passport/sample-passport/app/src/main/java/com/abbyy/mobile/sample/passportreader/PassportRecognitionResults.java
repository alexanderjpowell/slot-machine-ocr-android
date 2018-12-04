// ABBYY Real-Time Recognition SDK 1 © 2016 ABBYY Production LLC
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.
package com.abbyy.mobile.sample.passportreader;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.abbyy.mobile.rtr.IDataCaptureService;
import com.abbyy.mobile.sample.passportreader.interfaces.IPassportRecognitionPresenter;
import com.abbyy.mobile.sample.passportreader.interfaces.IPassportRecognitionResults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class PassportRecognitionResults implements IPassportRecognitionResults {
	// Идентификаторы схем страниц паспорта
	// Passport pages scheme ids
	public static final String TOP_PAGE_ID = "Passport_RU_Top";
	public static final String BOTTOM_PAGE_ID = "Passport_RU_Bottom";
	// Идентификаторы для bundle
	// IDs for bundle store
	private static final String RESULT_HAS_TOP = "resultHasTop";
	private static final String RESULT_HAS_BOTTOM = "resultHasBottom";
	private static final String PASSPORT_NUMBER_ID = "Number";

	public PassportRecognitionResults( IPassportRecognitionPresenter presenter )
	{
		this.presenter = presenter;
	}

	// Загрузка из bundle (для корректного поведения при событиях android activity lifecycle)
	// Loading from bundle (for correct interaction on android activity lifecycle events)
	@Override
	public void load( @Nullable Bundle savedInstanceState )
	{
		hasBottomPage = false;
		hasTopPage = false;
		stableTop = null;
		stableBottom = null;
		currentPage = null;
		lastSchemeId = null;

		if( savedInstanceState == null ) {
			return;
		}

		hasTopPage = savedInstanceState.getBoolean( RESULT_HAS_TOP, false );
		hasBottomPage = savedInstanceState.getBoolean( RESULT_HAS_BOTTOM, false );

		if( hasTopPage ) {
			stableTop = new SinglePageRecognitionResult();
			stableTop.load( savedInstanceState, TOP_PAGE_ID );
		}

		if( hasBottomPage ) {
			stableBottom = new SinglePageRecognitionResult();
			stableBottom.load( savedInstanceState, BOTTOM_PAGE_ID );
		}
	}

	// Сохранение в bundle (для корректного поведения при событиях android activity lifecycle)
	// Saving to bundle (for correct interaction on android activity lifecycle events)
	@Override
	public void save( @NonNull Bundle instance )
	{
		instance.putBoolean( RESULT_HAS_BOTTOM, hasBottomPage );
		instance.putBoolean( RESULT_HAS_TOP, hasTopPage );
		if( hasTopPage ) {
			stableTop.save( instance, TOP_PAGE_ID );
		}
		if( hasBottomPage ) {
			stableBottom.save( instance, BOTTOM_PAGE_ID );
		}
	}

	// Распознали ли нижнюю страницу
	// Is bottom page recognized
	@Override
	public boolean hasBottomPage()
	{
		return hasBottomPage;
	}

	// Распознали ли верхнюю страницу
	// Is top page recognized
	@Override
	public boolean hasTopPage()
	{
		return hasTopPage;
	}

	// Получает имена распознанных полей
	// Exports names of recognized fields
	@Override
	public ArrayList<String> exportFieldNames()
	{
		ArrayList<String> names = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		exportFieldNamesAndValues( names, values );
		return names;
	}

	// Получает значения распознанных полей
	// Exports values of recognized fields
	@Override
	public ArrayList<String> exportFieldValues()
	{
		ArrayList<String> names = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		exportFieldNamesAndValues( names, values );
		return values;
	}

	// Получает имена и значения распознанных полей
	// Exports names and values of recognized fields
	@Override
	public void exportFieldNamesAndValues( List<String> names, List<String> values )
	{
		if( ( !hasTopPage ) || ( !hasBottomPage ) ) {
			throw new IllegalStateException( "Results should have both top and bottom page" );
		}
		if( names.size() > 0 || values.size() > 0 ) {
			throw new IllegalArgumentException();
		}
		for( int i = 0; i < stableBottom.ids.size(); i++ ) {
			names.add( stableBottom.names.get( i ) );
			values.add( stableBottom.values.get( i ) );
		}
		for( int i = 0; i < stableTop.ids.size(); i++ ) {
			// Номер паспорта встречается и на верхней, и на нижней старинцы. Эвристика: берем результат с верхней
			// страницы, т.к. она не заламинирована и качество там лучше
			// The passport number duplicates on both top and bottom page. Heuristics: take result from top page,
			// in case of quality on in is better (top page is not laminated)
			if( stableTop.ids.get( i ).equals( PASSPORT_NUMBER_ID ) ) {
				boolean foundNumber = false;
				for( int j = 0; j < stableBottom.ids.size(); j++ ) {
					if( stableBottom.ids.get( j ).equals( PASSPORT_NUMBER_ID ) ) {
						values.set( j, stableTop.values.get( i ) );
						foundNumber = true;
						break;
					}
				}
				if( foundNumber ) {
					continue;
				}
			}
			names.add( stableTop.names.get( i ) );
			values.add( stableTop.values.get( i ) );
		}
	}

	// Добавление кадра и обновление результата
	// Recognized frame addition and results updating
	@Override
	public void addFrame( IDataCaptureService.DataScheme scheme, IDataCaptureService.DataField[] fields )
	{
		if( scheme != null ) {
			if( !scheme.Id.equals( lastSchemeId ) ) {
				currentPage = new SinglePageRecognitionResult();
				lastSchemeId = scheme.Id;
				presenter.clearRecognitionResult();
			}

			currentPage.addFrame( fields );
		}
	}

	// Уведомление об остановке распознавания
	// Notification about recognition completion
	@Override
	public void onRecognitionCompleted( String scheme )
	{
		if( scheme.equals( TOP_PAGE_ID ) ) {
			hasTopPage = true;
			stableTop = currentPage;
		} else if( scheme.equals( BOTTOM_PAGE_ID ) ) {
			hasBottomPage = true;
			stableBottom = currentPage;
		} else {
			throw new IllegalArgumentException( "Wrong scheme" );
		}
		currentPage = null;
		lastSchemeId = null;
	}

	// Наличие распознанных страниц паспорта
	// Presence of the pages
	private boolean hasBottomPage = false;
	private boolean hasTopPage = false;

	// Результаты распознавания одной страницы
	// Single page recognition results
	private SinglePageRecognitionResult stableTop;
	private SinglePageRecognitionResult stableBottom;
	private SinglePageRecognitionResult currentPage;

	// Идентификатор схемы c последнего распознанного кадра
	// Scheme ID of last recognized frame
	private String lastSchemeId;

	// Presenter для распознавания паспорта
	// Passport recognition presenter
	private IPassportRecognitionPresenter presenter;

	// Результаты распознавания одной страницы
	// Single page recognition results
	private class SinglePageRecognitionResult {
		// Идентификаторы для bundle
		// IDs for bundle store
		private static final String RESULT_IDS = "resultIds";
		private static final String RESULT_VALUES = "resultValues";
		private static final String RESULT_NAMES = "resultNames";

		// Добавление результатов распознавания очередного кадра
		// Frame recognition results adding
		void addFrame( IDataCaptureService.DataField[] fields )
		{
			// Полагаемся на то, что id приходят в одинаковом порядке, чтобы лишний раз не изменять GUI
			// The fields have same order. This code use this to less frequently change values in GUI
			for( int i = 0; i < fields.length; i++ ) {
				if( i >= ids.size() || !( ids.get( i ).equals( fields[i].Id ) ) ) {
					presenter.insertField( i, fields[i].Name, fields[i].Text );
					ids.add( i, fields[i].Id );
					values.add( i, fields[i].Text );
					names.add( i, fields[i].Name );
				} else {
					if( !fields[i].Text.equals( values.get( i ) ) ) {
						values.set( i, fields[i].Text );
						presenter.updateFieldValue( i, values.get( i ) );
					}
				}
			}

			// Удаляем поля, которых нет после обработки текущего кадра (т.к. результаты накапливаются от кадра к кадру,
			// поля, не найденные на одном кадре, не пропадают из общих результатов)
			// Remove fields not presented in results (Results from different frames are accumulated. If the field
			// is not recognized on one frame but is found on others it will be in results)
			Set<String> obtainedIds = new HashSet<>();
			for( int i = 0; i < ids.size(); i++ ) {
				if( !obtainedIds.add( ids.get( i ) ) ) {
					ids.remove( i );
					values.remove( i );
					names.remove( i );
					presenter.removeFieldAt( i );
					--i;
				}
			}
		}

		// Загрузка из bundle (для корректного поведения при событиях android activity lifecycle)
		// Loading from bundle (for correct interaction on android activity lifecycle events)
		void load( @Nullable Bundle savedInstanceState, String postfix )
		{
			// postfix для того, чтобы аккумулировать результаты распознавания двух страниц
			// postfix essential in case of two page results
			ids.clear();
			values.clear();

			if( savedInstanceState == null ) {
				return;
			}

			List<String> _ids = savedInstanceState.getStringArrayList( RESULT_IDS + postfix );
			List<String> _values = savedInstanceState.getStringArrayList( RESULT_VALUES + postfix );
			List<String> _names = savedInstanceState.getStringArrayList( RESULT_NAMES + postfix );
			if( _ids != null && _values != null && _names != null ) {
				if( _ids.size() != _values.size() || _names.size() != _ids.size() ) {
					throw new IllegalArgumentException();
				}
				ids.addAll( _ids );
				values.addAll( _values );
				names.addAll( _names );
			}
		}

		// Сохранение в bundle (для корректного поведения при событиях android activity lifecycle)
		// Saving to bundle (for correct interaction on android activity lifecycle events)
		void save( @NonNull Bundle instance, String postfix )
		{
			instance.putStringArrayList( RESULT_IDS + postfix, ids );
			instance.putStringArrayList( RESULT_VALUES + postfix, values );
			instance.putStringArrayList( RESULT_NAMES + postfix, names );
		}

		// Идентификаторы распознанных полей
		// Recognized fields' ids
		private ArrayList<String> ids = new ArrayList<>();
		// Значения распознанных полей
		// Recognized fields' values
		private ArrayList<String> values = new ArrayList<>();
		// Имена распознанных полей
		// Recognized fields' names
		private ArrayList<String> names = new ArrayList<>();
	}
}
