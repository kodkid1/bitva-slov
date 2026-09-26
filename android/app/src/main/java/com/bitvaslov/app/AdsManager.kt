package com.bitvaslov.app

import android.content.Context
import com.google.android.gms.ads.AdRequest
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

    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null

    fun init(context: Context) {
        MobileAds.initialize(context)
        loadInterstitial(context)
        loadRewarded(context)
    }

    fun loadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
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

    fun loadRewarded(context: Context) {
        RewardedAd.load(
            context,
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

    fun showInterstitial(activity: android.app.Activity, onDone: () -> Unit = {}) {
        val ad = interstitial
        if (ad == null) { onDone(); return }
        ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                loadInterstitial(activity)
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                interstitial = null
                loadInterstitial(activity)
                onDone()
            }
        }
        ad.show(activity)
    }

    fun showRewarded(activity: android.app.Activity, onReward: () -> Unit = {}) {
        val ad = rewarded
        if (ad == null) { onReward(); return }
        ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewarded = null
                loadRewarded(activity)
                onReward()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                rewarded = null
                loadRewarded(activity)
                onReward()
            }
        }
        ad.show(activity) {
            onReward()
        }
    }
}