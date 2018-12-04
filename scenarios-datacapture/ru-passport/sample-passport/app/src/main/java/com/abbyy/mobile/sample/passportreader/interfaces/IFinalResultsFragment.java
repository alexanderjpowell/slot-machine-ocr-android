// ABBYY Real-Time Recognition SDK 1 © 2016 ABBYY Production LLC
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.
package com.abbyy.mobile.sample.passportreader.interfaces;

import com.abbyy.mobile.sample.passportreader.FinalResultsFragment;

import java.util.List;

// Фрагмент для отображения окончательных результатов распознавания
// Fragment for final recognition results displaying
public interface IFinalResultsFragment {
	// Установка имен и значений распознанных полей
	// Recognized fields names and values setter
	void setNamesAndValues( List<String> names, List<String> values );

	// OnInteractionCallback setter
	void setCallback( FinalResultsFragment.OnInteractionCallback value );
}
