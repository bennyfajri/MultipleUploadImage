package com.example.multipleuploadimage

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.multipleuploadimage.databinding.ActivityMainBinding
import com.gunawan.multipleimages.utils.FileCompressor
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_TAKE_PHOTO = 1
        private const val REQUEST_OPEN_GALLERY = 2
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var fileCompressor: FileCompressor
    private lateinit var addImageAdapter: AddImageAdapter
    private lateinit var fileImage: File
    private lateinit var dialog: AlertDialog
    private var listImage: MutableList<File> = ArrayList()
    private var selectedSelectImage: Int = 0
//    private val multipleImagesViewModel by viewModel<MultipleImagesViewModel>()
    private val listSelectImage = arrayOf("Take Photo", "Choose from Gallery")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        fileCompressor = FileCompressor(this)

        binding.imageView.setOnClickListener { selectImage() }
        initAdapter()
    }

    private fun initAdapter() {
        addImageAdapter = AddImageAdapter(listImage)
        binding.rvImage.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = addImageAdapter

            addImageAdapter.setOnCustomClickListener(object : AddImageAdapter.OnCustomClickListener {
                override fun onDeleteClicked(position: Int) {
                    listImage.removeAt(position)
                    addImageAdapter.notifyDataSetChanged()
                }
            })
        }
    }

    private fun selectImage() {
        val builder = AlertDialog.Builder(this)
        builder.setItems(listSelectImage) { _, item ->
            when {
                listSelectImage[item] == "Take Photo" -> {
                    selectedSelectImage = 0
                    if (checkPersmission()) {
                        takePhoto()
                    }
                    else {
                        requestPermission()
                    }
                }
                listSelectImage[item] == "Choose from Gallery" -> {
                    selectedSelectImage = 1
                    if (checkPersmission()) {
                        openGallery()
                    }
                    else {
                        requestPermission()
                    }
                }
            }
        }
        builder.show()
    }

    private fun checkPersmission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED  && ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                if (selectedSelectImage == 0) {
                    takePhoto()
                } else {
                    openGallery()
                }

            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        fileImage = createFile()
        val uri = if(Build.VERSION.SDK_INT >= 24){
            FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider",
                fileImage)
        } else {
            Uri.fromFile(fileImage)
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        startActivityForResult(intent, REQUEST_TAKE_PHOTO)
    }

    private fun openGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_OPEN_GALLERY)
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("IMG_${timeStamp}", ".jpg", storageDir)
    }

    private fun bitmapToFile(bitmap: Bitmap): File {
        return try {
            fileImage = createFile()
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos)
            val bitmapdata = bos.toByteArray()

            val fos = FileOutputStream(fileImage)
            fos.write(bitmapdata)
            fos.flush()
            fos.close()
            fileImage
        } catch (e: Exception) {
            e.printStackTrace()
            fileImage
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                fileImage = fileCompressor.compressToFile(fileImage)!!
                listImage.add(fileImage)
                addImageAdapter.notifyDataSetChanged()
            }
        } else if (requestCode == REQUEST_OPEN_GALLERY) {
            if (resultCode == Activity.RESULT_OK) {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(this.contentResolver, data!!.data!!))
                } else {
                    MediaStore.Images.Media.getBitmap(this.contentResolver, data!!.data!!)
                }
                val tempFile = fileCompressor.compressToFile(bitmapToFile(bitmap))
                listImage.add(tempFile!!)
                addImageAdapter.notifyDataSetChanged()
            }
        }
    }
}