// ABBYY Real-Time Recognition SDK 1 © 2016 ABBYY Production LLC
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.
package com.abbyy.mobile.sample.passportreader.interfaces;

import android.support.annotation.Nullable;

// Фрагмент с отображением подсказки
// Fragment for prompt displaying
public interface IPromptFragment {
	// Установка идентификатора картинки - подсказки
	// Prompt picture id setter
	void setPromptPictureId( @Nullable Integer promptPictureId );

	// Установка идентификатора картинки уже распознанной страницы
	// Already recognized picture id setter
	void setRecognizedPagePictureId( @Nullable Integer pictureId );

	// Установка текста подсказки
	// Prompt text setter
	void setPromptText( @Nullable String promptText );
}
