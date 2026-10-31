# LiveCom

تطبيق Android اجتماعي للبث المباشر والتفاعل الصوتي والمرئي.

## المرحلة الحالية
هذه نسخة تأسيسية قابلة للبناء، تعرض شاشة البداية وتحتوي على GitHub Actions لبناء APK.
لم يتم توصيل Firebase أو ZEGO بعد؛ لا تضع مفاتيح أو أسرار الخدمات داخل المستودع.

## التقنيات
- Kotlin
- Android Gradle Plugin 8.7.3
- JDK 17 / Gradle 8.9
- Firebase Authentication / Cloud Firestore / Storage (مرحلة لاحقة)
- ZEGO Cloud (مرحلة لاحقة)

## البناء
من GitHub افتح Actions ثم شغّل **Build LiveCom Android APK** أو ادفع تحديثاً إلى main.
بعد نجاح البناء نزّل artifact باسم `LiveCom-debug-apk`.

## الحزمة
`com.livecom.app`

## خارطة الطريق
1. تأسيس المشروع والبناء الآلي.
2. Anonymous Authentication.
3. Firestore room directory وتحديث Active Rooms لحظياً.
4. إنشاء الغرف العامة والخاصة ورمز الدخول.
5. دمج ZEGO عبر خادم موثوق لإصدار رموز الدخول.
6. الأدوار والإشراف وسجل الحظر والدردشة.
7. Security Rules واختبارات الأمان وإعادة الاتصال.
