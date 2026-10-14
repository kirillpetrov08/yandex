package com.blockpuzzle.app

/**
 * Настройки, которые заполняет разработчик перед публикацией.
 * ВАЖНО: без своего сайта и политики конфиденциальности Google Play не пропустит приложение,
 * а РСЯ не сможет проверить app-ads.txt (это влияет на доход).
 */
object Config {
    /**
     * Адрес политики конфиденциальности. Готовая страница лежит в корне репозитория:
     * privacy-policy.html — положите её в публичный репозиторий `kirillpetrov08.github.io`
     * (GitHub Pages пользователя) и вставьте адрес сюда, а также укажите его в Google Play
     * Console (App content → Privacy policy) и в описании приложения.
     * Такой адрес даёт короткий корень домена, где РСЯ находит app-ads.txt.
     */
    const val PRIVACY_POLICY_URL = "https://kirillpetrov08.github.io/privacy-policy.html"

    /** Ссылка на страницу приложения в Google Play (для «оценить» и app-ads.txt). */
    const val PLAY_MARKET_URL = "https://play.google.com/store/apps/details?id=com.blockpuzzle.app"

    /** Почта поддержки: обязательна в описании приложения и для пользователей. */
    const val SUPPORT_EMAIL = "kirillpetrov08@gmail.com"
}
