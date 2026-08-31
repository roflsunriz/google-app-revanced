package app.revanced.extension.googleapp;

import java.util.Locale;

public final class LocalizedStrings {
    public final String settingsTitle;
    public final String settingsSummary;
    public final String sdkTitle;
    public final String sdkSummary;
    public final String webTitle;
    public final String webSummary;
    public final String promotionTitle;
    public final String promotionSummary;
    public final String nativeTitle;
    public final String nativeSummary;
    public final String close;
    public final boolean rightToLeft;

    private LocalizedStrings(String[] values, boolean rightToLeft) {
        settingsTitle = values[0];
        settingsSummary = values[1];
        sdkTitle = values[2];
        sdkSummary = values[3];
        webTitle = values[4];
        webSummary = values[5];
        promotionTitle = values[6];
        promotionSummary = values[7];
        nativeTitle = values[8];
        nativeSummary = values[9];
        close = values[10];
        this.rightToLeft = rightToLeft;
    }

    public static LocalizedStrings current() {
        String language = Locale.getDefault().getLanguage();
        switch (language) {
            case "ja":
                return of(new String[]{
                        "Google ReVanced", "広告とプロモーションの除去を管理します",
                        "広告SDK通信を遮断", "常に有効です。既知の広告配信先への接続を拒否します",
                        "Web検索広告を非表示", "検索結果内の広告要素を削除し、空白も詰めます",
                        "セルフプロモーションを非表示", "Googleアプリ内の機能・サービス宣伝を非表示にします",
                        "ネイティブ広告枠を非表示", "広告カードと動画広告枠を完全に折り畳みます",
                        "閉じる"
                }, false);
            case "zh":
                return of(new String[]{
                        "Google ReVanced", "管理广告和推广内容移除",
                        "阻止广告 SDK 通信", "始终启用，拒绝连接已知广告服务",
                        "隐藏网页搜索广告", "移除搜索结果中的广告并收起空白",
                        "隐藏自我推广", "隐藏 Google 应用内的产品和功能推广",
                        "隐藏原生广告位", "完全收起广告卡片和视频广告位",
                        "关闭"
                }, false);
            case "hi":
                return of(new String[]{
                        "Google ReVanced", "विज्ञापन और प्रचार हटाने की सेटिंग प्रबंधित करें",
                        "विज्ञापन SDK संचार रोकें", "हमेशा चालू; ज्ञात विज्ञापन सेवाओं के कनेक्शन रोकता है",
                        "वेब खोज विज्ञापन छिपाएँ", "खोज परिणामों से विज्ञापन और उनकी खाली जगह हटाता है",
                        "स्व-प्रचार छिपाएँ", "Google ऐप के उत्पाद और सुविधा प्रचार छिपाता है",
                        "नेटिव विज्ञापन स्थान छिपाएँ", "विज्ञापन कार्ड और वीडियो स्थान पूरी तरह समेटता है",
                        "बंद करें"
                }, false);
            case "es":
                return of(new String[]{
                        "Google ReVanced", "Gestiona la eliminación de anuncios y promociones",
                        "Bloquear comunicación del SDK", "Siempre activo; bloquea servicios publicitarios conocidos",
                        "Ocultar anuncios de búsqueda web", "Elimina anuncios y cierra el espacio vacío",
                        "Ocultar promociones propias", "Oculta promociones de productos y funciones de Google",
                        "Ocultar espacios publicitarios", "Contrae por completo tarjetas y espacios de vídeo",
                        "Cerrar"
                }, false);
            case "fr":
                return of(new String[]{
                        "Google ReVanced", "Gérer la suppression des annonces et promotions",
                        "Bloquer les communications du SDK", "Toujours actif ; bloque les services publicitaires connus",
                        "Masquer les annonces Web", "Supprime les annonces des résultats et referme l’espace vide",
                        "Masquer l’autopromotion", "Masque les promotions des produits et fonctions Google",
                        "Masquer les emplacements natifs", "Replie entièrement les cartes et espaces vidéo publicitaires",
                        "Fermer"
                }, false);
            case "ar":
                return of(new String[]{
                        "Google ReVanced", "إدارة إزالة الإعلانات والعروض الترويجية",
                        "حظر اتصالات حزمة الإعلانات", "مفعّل دائمًا ويحظر خدمات الإعلانات المعروفة",
                        "إخفاء إعلانات بحث الويب", "يزيل الإعلانات ويغلق المساحات الفارغة",
                        "إخفاء الترويج الذاتي", "يخفي عروض منتجات Google وميزاتها",
                        "إخفاء مواضع الإعلانات الأصلية", "يطوي بطاقات الإعلانات ومواضع الفيديو بالكامل",
                        "إغلاق"
                }, true);
            case "pt":
                return of(new String[]{
                        "Google ReVanced", "Gerencie a remoção de anúncios e promoções",
                        "Bloquear comunicação do SDK", "Sempre ativo; bloqueia serviços de anúncios conhecidos",
                        "Ocultar anúncios da pesquisa Web", "Remove anúncios e elimina o espaço vazio",
                        "Ocultar autopromoções", "Oculta promoções de produtos e recursos do Google",
                        "Ocultar espaços de anúncios nativos", "Recolhe totalmente cartões e espaços de vídeo",
                        "Fechar"
                }, false);
            case "bn":
                return of(new String[]{
                        "Google ReVanced", "বিজ্ঞাপন ও প্রচার অপসারণ পরিচালনা করুন",
                        "বিজ্ঞাপন SDK যোগাযোগ বন্ধ করুন", "সবসময় চালু; পরিচিত বিজ্ঞাপন পরিষেবা ব্লক করে",
                        "ওয়েব সার্চ বিজ্ঞাপন লুকান", "সার্চ ফলাফলের বিজ্ঞাপন ও ফাঁকা স্থান সরায়",
                        "নিজস্ব প্রচার লুকান", "Google অ্যাপের পণ্য ও ফিচার প্রচার লুকায়",
                        "নেটিভ বিজ্ঞাপন স্থান লুকান", "বিজ্ঞাপন কার্ড ও ভিডিও স্থান পুরোপুরি গুটিয়ে দেয়",
                        "বন্ধ করুন"
                }, false);
            case "ru":
                return of(new String[]{
                        "Google ReVanced", "Управление удалением рекламы и промоакций",
                        "Блокировать связь рекламного SDK", "Всегда включено; блокирует известные рекламные службы",
                        "Скрывать рекламу в веб-поиске", "Удаляет рекламу и освобождает пустое место",
                        "Скрывать саморекламу", "Скрывает продвижение продуктов и функций Google",
                        "Скрывать нативные рекламные места", "Полностью сворачивает карточки и видеоблоки",
                        "Закрыть"
                }, false);
            case "ur":
                return of(new String[]{
                        "Google ReVanced", "اشتہارات اور تشہیر ہٹانے کا نظم کریں",
                        "اشتہاری SDK رابطہ روکیں", "ہمیشہ فعال؛ معروف اشتہاری خدمات کو روکتا ہے",
                        "ویب تلاش کے اشتہارات چھپائیں", "اشتہارات اور خالی جگہ کو ہٹا دیتا ہے",
                        "خود تشہیر چھپائیں", "Google ایپ کی مصنوعات اور خصوصیات کی تشہیر چھپاتا ہے",
                        "مقامی اشتہاری جگہ چھپائیں", "اشتہاری کارڈ اور ویڈیو جگہ مکمل طور پر سمیٹتا ہے",
                        "بند کریں"
                }, true);
            default:
                return of(new String[]{
                        "Google ReVanced", "Manage advertisement and promotion removal",
                        "Block ad SDK communication", "Always enabled; rejects connections to known ad services",
                        "Hide web search ads", "Removes search ads and collapses the empty space",
                        "Hide self-promotions", "Hides Google product and feature promotions",
                        "Hide native ad slots", "Fully collapses ad cards and video ad slots",
                        "Close"
                }, false);
        }
    }

    private static LocalizedStrings of(String[] values, boolean rightToLeft) {
        return new LocalizedStrings(values, rightToLeft);
    }
}
