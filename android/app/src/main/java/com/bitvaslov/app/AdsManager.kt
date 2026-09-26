package com.bitvaslov.app

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdsManager {

    // Официальные тестовые блоки Google
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    private var appContext: Context? = null
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null

    fun init(context: Context) {
        val ctx = context.applicationContext
        if (appContext != null) return
        appContext = ctx
        MobileAds.initialize(ctx)
        loadInterstitial()
        loadRewarded()
    }

    fun loadInterstitial() {
        val ctx = appContext ?: return
        InterstitialAd.load(
            ctx,
            TEST_INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                }
            },
        )
    }

    fun loadRewarded() {
        val ctx = appContext ?: return
        RewardedAd.load(
            ctx,
            TEST_REWARDED,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewarded = null
                }
            },
        )
    }

    fun showInterstitial(activity: Activity, onDone: () -> Unit = {}) {
        val ad = interstitial
        // сбрасываем до показа, чтобы повторный вызов не показал то же самое объявление дважды
        interstitial = null
        if (ad == null) { onDone(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadInterstitial()
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                loadInterstitial()
                onDone()
            }
        }
        runCatching { ad.show(activity) }.onFailure {
            loadInterstitial()
            onDone()
        }
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit = {}) {
        val ad = rewarded
        // сбрасываем до показа: раньше onReward срабатывал дважды — в onAdDismissed и в show{}
        rewarded = null
        if (ad == null) { onReward(); return }
        var granted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadRewarded()
                if (granted) onReward()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                loadRewarded()
            }
        }
        runCatching {
            ad.show(activity) {
                granted = true
                onReward()
            }
        }.onFailure {
            loadRewarded()
        }
    }
}
