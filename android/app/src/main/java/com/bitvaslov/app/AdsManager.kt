package com.bitvaslov.app

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.InitializationListener
import com.yandex.mobile.ads.common.YandexAds
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import kotlin.math.roundToInt

object AdsManager {

    /** Боевой баннер из кабинета Яндекса (App ID 20117140). */
    const val BANNER_UNIT = "R-M-20117140-1"

    /** Блоки interstitial/rewarded в кабинете ещё не созданы — показываем демо-блоки. */
    private const val INTERSTITIAL_UNIT = "demo-interstitial-yandex"
    private const val REWARDED_UNIT = "demo-rewarded-yandex"

    private var appContext: Context? = null
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var interstitialShowing = false
    private var rewardedShowing = false

    fun init(context: Context) {
        val ctx = context.applicationContext
        if (appContext != null) return
        appContext = ctx
        YandexAds.enableLogging(true)
        YandexAds.initialize(ctx, object : InitializationListener {
            override fun onInitializationCompleted() {
                loadInterstitial()
                loadRewarded()
            }
        })
    }

    /** Создаёт и сразу загружает баннер под ширину экрана. Вызывать из Compose через AndroidView. */
    fun createBannerView(context: Context, adUnitId: String = BANNER_UNIT): BannerAdView {
        val dm: DisplayMetrics = context.resources.displayMetrics
        val adWidthDp = (dm.widthPixels / dm.density).roundToInt()
        return BannerAdView(context).apply {
            setAdSize(BannerAdSize.sticky(context, adWidthDp))
            setBannerAdEventListener(object : BannerAdEventListener {
                override fun onAdLoaded() = Unit
                override fun onAdFailedToLoad(error: AdRequestError) = Unit
                override fun onAdClicked() = Unit
                override fun onImpression(impressionData: com.yandex.mobile.ads.common.ImpressionData?) = Unit
            })
            loadAd(AdRequest.Builder(adUnitId).build())
        }
    }

    fun loadInterstitial() {
        val ctx = appContext ?: return
        InterstitialAdLoader(ctx).loadAd(
            AdRequest.Builder(INTERSTITIAL_UNIT).build(),
            object : InterstitialAdLoadListener {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    interstitial = null
                }
            },
        )
    }

    fun loadRewarded() {
        val ctx = appContext ?: return
        RewardedAdLoader(ctx).loadAd(
            AdRequest.Builder(REWARDED_UNIT).build(),
            object : RewardedAdLoadListener {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    rewarded = null
                }
            },
        )
    }

    fun showInterstitial(activity: Activity, onDone: () -> Unit = {}) {
        val ad = interstitial
        // сбрасываем до показа, чтобы повторный вызов не показал то же самое объявление дважды
        interstitial = null
        if (ad == null || interstitialShowing) { onDone(); return }
        interstitialShowing = true
        ad.setAdEventListener(object : InterstitialAdEventListener {
            override fun onAdShown() = Unit
            override fun onAdFailedToShow(error: AdError) {
                interstitialShowing = false
                loadInterstitial()
                onDone()
            }

            override fun onAdDismissed() {
                interstitialShowing = false
                loadInterstitial()
                onDone()
            }

            override fun onAdClicked() = Unit
            override fun onAdImpression(impressionData: com.yandex.mobile.ads.common.ImpressionData?) = Unit
        })
        runCatching { ad.show(activity) }.onFailure {
            interstitialShowing = false
            loadInterstitial()
            onDone()
        }
    }

    /**
     * onReward — только когда зритель реально досмотрел ролик.
     * onUnavailable — ролика не было (не загрузился, лимит, показывается другой):
     * вызывать onReward в этом случае нельзя, иначе монетки начисляются просто так.
     */
    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit = {},
        onUnavailable: () -> Unit = {},
    ) {
        val ad = rewarded
        // сбрасываем до показа: раньше onReward срабатывал дважды — в onAdDismissed и в show{}
        rewarded = null
        if (ad == null || rewardedShowing) { onUnavailable(); return }
        rewardedShowing = true
        // награда выдаётся только из onRewarded, onAdDismissed merely сбрасывает состояние
        var granted = false
        ad.setAdEventListener(object : RewardedAdEventListener {
            override fun onAdShown() = Unit
            override fun onAdFailedToShow(error: AdError) {
                rewardedShowing = false
                loadRewarded()
                onUnavailable()
            }

            override fun onAdDismissed() {
                rewardedShowing = false
                loadRewarded()
                if (granted) onReward()
            }

            override fun onAdClicked() = Unit
            override fun onAdImpression(impressionData: com.yandex.mobile.ads.common.ImpressionData?) = Unit
            override fun onRewarded(reward: Reward) {
                granted = true
            }
        })
        runCatching { ad.show(activity) }.onFailure {
            rewardedShowing = false
            loadRewarded()
            onUnavailable()
        }
    }
}
