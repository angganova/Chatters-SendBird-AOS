package com.fullstackdiv.chatters.helper

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import android.graphics.drawable.Drawable
import com.bumptech.glide.request.RequestListener
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.BitmapImageViewTarget



/**
 * Created by Angga N P on 3/29/2019.
 */

object HelperImage {

    /**
     * Crops image into a circle that fits within the ImageView.
     */
    fun displayRoundImageFromUrl(context: Context, url: String, imageView: ImageView) {
        val myOptions = RequestOptions()
            .centerCrop()
            .dontAnimate()

        Glide.with(context)
            .asBitmap()
            .apply(myOptions)
            .load(url)
            .into(object : BitmapImageViewTarget(imageView) {
                override fun setResource(resource: Bitmap?) {
                    val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource)
                    circularBitmapDrawable.isCircular = true
                    imageView.setImageDrawable(circularBitmapDrawable)
                }
            })
    }

    /**
     * Displays an image from a URL in an ImageView.
     */
    @JvmOverloads
    fun displayImageFromUrl(context: Context, url: String?,
        imageView: ImageView, placeholderDrawable: Drawable?, listener: RequestListener<Drawable>? = null
    ) {
        val myOptions = RequestOptions()
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .placeholder(placeholderDrawable)

        if (listener != null) {
            Glide.with(context)
                .load(url)
                .apply(myOptions)
                .listener(listener)
                .into(imageView)
        } else {
            Glide.with(context)
                .load(url)
                .apply(myOptions)
                .listener(listener)
                .into(imageView)
        }
    }

    @JvmOverloads
    fun displayRoundImageFromUrlWithoutCache(
        context: Context, url: String,
        imageView: ImageView, listener: RequestListener<Bitmap>? = null
    ) {
        val myOptions = RequestOptions()
            .centerCrop()
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)

        if (listener != null) {
            Glide.with(context)
                .asBitmap()
                .load(url)
                .apply(myOptions)
                .listener(listener)
                .into(object : BitmapImageViewTarget(imageView) {
                    override fun setResource(resource: Bitmap?) {
                        val circularBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(context.getResources(), resource)
                        circularBitmapDrawable.isCircular = true
                        imageView.setImageDrawable(circularBitmapDrawable)
                    }
                })
        } else {
            Glide.with(context)
                .asBitmap()
                .load(url)
                .apply(myOptions)
                .into(object : BitmapImageViewTarget(imageView) {
                    override fun setResource(resource: Bitmap?) {
                        val circularBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(context.getResources(), resource)
                        circularBitmapDrawable.isCircular = true
                        imageView.setImageDrawable(circularBitmapDrawable)
                    }
                })
        }
    }

    /**
     * Displays an image from a URL in an ImageView.
     * If the image is loading or nonexistent, displays the specified placeholder image instead.
     */
    fun displayImageFromUrlWithPlaceHolder(
        context: Context, url: String,
        imageView: ImageView,
        placeholderResId: Int
    ) {
        val myOptions = RequestOptions()
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .placeholder(placeholderResId)

        Glide.with(context)
            .load(url)
            .apply(myOptions)
            .into(imageView)
    }

    /**
     * Displays an image from a URL in an ImageView.
     */
    fun displayGifImageFromUrl(
        context: Context,
        url: String,
        imageView: ImageView,
        placeholderDrawable: Drawable,
        listener: RequestListener<GifDrawable>?
    ) {
        val myOptions = RequestOptions()
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .placeholder(placeholderDrawable)

        if (listener != null) {
            Glide.with(context)
                .asGif()
                .load(url)
                .apply(myOptions)
                .listener(listener)
                .into(imageView)
        } else {
            Glide.with(context)
                .asGif()
                .load(url)
                .apply(myOptions)
                .into(imageView)
        }
    }

    /**
     * Displays an GIF image from a URL in an ImageView.
     */
    fun displayGifImageFromUrl(
        context: Context,
        url: String,
        imageView: ImageView,
        thumbnailUrl: String?,
        placeholderDrawable: Drawable
    ) {
        val myOptions = RequestOptions()
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .placeholder(placeholderDrawable)

        if (thumbnailUrl != null) {
            Glide.with(context)
                .asGif()
                .load(url)
                .apply(myOptions)
                .thumbnail(Glide.with(context).asGif().load(thumbnailUrl))
                .into(imageView)
        } else {
            Glide.with(context)
                .asGif()
                .load(url)
                .apply(myOptions)
                .into(imageView)
        }
    }
}// Prevent instantiation