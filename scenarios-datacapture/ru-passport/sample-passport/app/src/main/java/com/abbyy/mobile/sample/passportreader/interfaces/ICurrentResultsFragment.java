// ABBYY Real-Time Recognition SDK 1 © 2016 ABBYY Production LLC
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.
package com.abbyy.mobile.sample.passportreader.interfaces;

import android.support.annotation.Nullable;
import android.view.View;

// Фрагмент для отображения результатов распознавания (в процессе распознавнания)
// The fragment for the results displaying (during recognition process)
public interface ICurrentResultsFragment {
	void setOnTouchListener( View.OnTouchListener listener );

	// Добавление нового распознанного поля
	// Inserts new recognized field
	void insertField( int index, String name, String text );

	// Обновление значение распознанного поля
	// Updates value of the new recognized field
	void updateFieldValue( int index, String text );

	// Удаляет распознанное поле по индексу
	// Removes recognized field by index
	void removeFieldAt( int index );

	// В UI есть две картинки подсказки. Этот setter устанавливает верхнюю из них
	// There are two prompt pictures in the UI. This setter configures the top one
	void setTopPicture( @Nullable Integer id );

	// В UI есть две картинки подсказки. Этот setter устанавливает нижнюю из них
	// There are two prompt pictures in the UI. This setter configures the bottom one
	void setBottomPicture( @Nullable Integer id );

	// Очищает результаты
	// Clears the recognition results
	void clearResult();

	// Управление стабильностью (и цветом прогресс-бара)
	// Stability setter (influences progress-bar color)
	void setStable( boolean stable );
}
