package com.yudikarma.multipleselectiondemo.model

import android.os.Parcelable
import com.naver.android.helloyako.imagecrop.model.CropInfo
import kotlinx.android.parcel.Parcelize

/**
 * GaleryFragmentModel is Model class with Parcelable for save info about File at Galery Fragment
 *
 * @param fileName path name image in Internal Storage
 * @param infoCrop : for save crop info from android image zoom library
 * @param position : position image at list image show in galery fragment
 * @param type_file : type file show in galery fragment "foto" or video
 */
@Parcelize
data class GaleryFragmentModel(var fileName: String?=null,var infoCrop:String?= null,var position:Long? = null ,var type_file:Int?=null):Parcelable

/**
 * GaleryFragmentModel is Model class with non Parcelable for save info about File at Galery Fragment
 *
 * @param fileName path name image in Internal Storage
 * @param infoCrop : for save crop info from android image zoom library
 * @param position : position image at list image show in galery fragment
 * @param type_file : type file show in galery fragment "foto" or video
 */
data class GaleryFragmentModelNonParcel(var fileName: String?=null,var infoCrop:CropInfo?= null,var position:Long? = null,var type_file:Int?=null)