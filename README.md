# AutoFlix — Android & Android Auto Live TV & Web Player

اپلیکیشن مالتی‌مدیا، پخش زنده شبکه‌های تلویزیونی و مرورگر وب با پشتیبانی کامل از **Android Auto Projection** (نمایش تصویر و رابط کاربری اختصاصی روی مانیتور خودرو).

## ✨ امکانات کلیدی
- **پخش زنده تلویزیون (Live TV):** دسته‌بندی هوشمند شبکه‌ها (سراسری، استانی، فیلم و سریال، موزیک، کودک، ورزشی، خبری و بین‌المللی) + جستجوی آنی و لیست علاقه‌مندی‌ها.
- **پشتیبانی کامل از Android Auto (Projection):**
  - رابط کاربری افقی اختصاصی مانیتور خودرو (`AUTOFLIX CAR OS`).
  - پخش روان تصویر ویدیو روی نمایشگر خودرو با استفاده از `TextureView` و بهینه‌سازی سخت‌افزاری استریم (`HlsMediaSource` + `DefaultLoadControl`).
  - حالت تمام‌صفحه (Fullscreen) روی مانیتور خودرو با دکمه‌های کنترلی بزرگ.
  - قابلیت ارسال آنی شبکه در حال پخش یا سایت از گوشی به مانیتور خودرو (**Car Handoff**).
- **پخش‌کننده پیشرفته موبایل:** پشتیبانی از حالت تمام‌صفحه افقی (Immersive Fullscreen) و تصویر در تصویر (**Picture-in-Picture / PiP**).
- **مرورگر وب و سایت‌های ذخیره‌شده:** مرورگر داخلی با قابلیت ذخیره و مدیریت سایت‌های دلخواه روی گوشی و خودرو.
- **معماری تمیز و امن:** بازنویسی کامل با `Kotlin`، `Jetpack Compose (Material 3)`، `Media3 (ExoPlayer)` و `Room Database` بدون هیچ‌گونه ترکر یا تبلیغات.

---

## 🚀 دریافت بیلد از طریق GitHub Actions
این مخزن مجهز به ورک‌فلوی خودکار **GitHub Actions** در مسیر `.github/workflows/android-build.yml` است:
1. پس از بارگذاری پروژه در گیت‌هاب، به تب **Actions** بروید.
2. با هر `Push` به شاخه `main`/`master` یا با زدن دکمه **Run workflow** در بخش `Android CI Build (APK)`، هر دو نسخه زیر به صورت خودکار ساخته می‌شوند:
   - **`AutoFlix-v0.6.0-debug-apk`** (`app-debug.apk`)
   - **`AutoFlix-v0.6.0-release-apk`** (`app-release.apk` - بهینه‌سازی و فشرده‌شده با R8)
3. فایل‌های APK را از قسمت **Artifacts** در پایین صفحه اجرای اکشن دانلود کنید.

---

## 🛠️ بیلد محلی (Local Build)
```bash
./gradlew assembleDebug
# یا در ویندوز:
.\gradlew.bat assembleDebug
```
خروجی در مسیر `app/build/outputs/apk/debug/app-debug.apk` قرار می‌گیرد.
