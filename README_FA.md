نسخه رایگان On-device

این نسخه از OpenAI یا API پولی استفاده نمی‌کند.
AudioPlaybackCapture صدای Media را می‌گیرد، آن را به صورت PCM 16kHz mono از یک pipe به ML Kit GenAI Speech Recognition می‌دهد و متن انگلیسی را با ML Kit Translation به فارسی ترجمه می‌کند.

مدل ترجمه با اینترنت و Wi-Fi دانلود می‌شود؛ پس از دانلود، ترجمه روی دستگاه انجام می‌شود.
تشخیص گفتار Basic نیز روی دستگاه انجام می‌شود، ولی در دسترس بودن مدل به دستگاه و سرویس‌های ML Kit/AICore وابسته است.

Android 12+ توصیه می‌شود.
بعضی اپ‌ها ممکن است Capture صدای خود را مسدود کنند.

این پروژه در این محیط APK نهایی build نمی‌شود چون Android SDK/Gradle محلی در اختیار نیست.
