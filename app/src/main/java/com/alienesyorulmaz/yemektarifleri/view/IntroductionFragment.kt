package com.alienesyorulmaz.yemektarifleri.view

import android.Manifest
import android.app.DirectAction
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Path.Direction
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.room.Room
import com.alienesyorulmaz.yemektarifleri.databinding.FragmentIntroductionBinding
import com.alienesyorulmaz.yemektarifleri.model.Tarif
import com.alienesyorulmaz.yemektarifleri.roomdb.TarifDAO
import com.alienesyorulmaz.yemektarifleri.roomdb.TarifDatabase
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.io.IOException

class IntroductionFragment : Fragment() {

    private var _binding: FragmentIntroductionBinding? = null
    private val binding get() = _binding!!
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private var secilenGorsel: Uri? = null
    private var secilenBitmap: Bitmap? = null //şecilen görseli uygulamaya kullanabileceği uygun hale getirir
    private lateinit var  db: TarifDatabase
    private lateinit var tarifDao: TarifDAO
    private val mDisposable = CompositeDisposable()
    private var secilenTarif: Tarif? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher()

        db = Room.databaseBuilder(requireContext(),TarifDatabase::class.java,"Tarifler").build()
        tarifDao = db.TarifDAO()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentIntroductionBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imageView.setOnClickListener{GorselSec(it)}
        binding.kaydetButton.setOnClickListener { Kaydet(it) }
        binding.silButton.setOnClickListener { Sil(it) }

        arguments?.let {
            val bilgi = IntroductionFragmentArgs.fromBundle(it).information

            if (bilgi == "yeni"){
                //Yeni Tarif Eklenecek
                secilenTarif = null
                binding.kaydetButton.isEnabled = true
                binding.silButton.isEnabled = false
                binding.adText.setText("")
                binding.tarifText.setText("")
            }else{
                //Eski tarif gösterilecek
                binding.kaydetButton.isEnabled = false
                binding.silButton.isEnabled = true
                val id = IntroductionFragmentArgs.fromBundle(it).id

                mDisposable.add(tarifDao.findById(id).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(this::handleResponse))
            }
        }
    }

    private fun handleResponse(tarif: Tarif){
        val bitmap = BitmapFactory.decodeByteArray(tarif.gorsel,0,tarif.gorsel.size)
        binding.imageView.setImageBitmap(bitmap)
        binding.adText.setText(tarif.isim)
        binding.tarifText.setText(tarif.malzeme)
        secilenTarif = tarif
    }

    fun GorselSec(view: View){ //bu izinleri githuba koyabilirsin

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){
                //izin verilmemiş, izin istememiz gerekiyor.
                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.READ_MEDIA_IMAGES)){
                    //snackbar göstermemiz lazım, kullanıcıdan neden izin istediğimizi bir kez daha söyleyecek izin istememiz lazım
                    Snackbar.make(view,"Galeriye gidip resim seçmen gerekli!",Snackbar.LENGTH_INDEFINITE).setAction("İzin ver",View.OnClickListener { permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES) }).show()
                }else{
                    //İzin isteyeceğiz
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }else{
                //izin verilmiş, galeriye gidebilirim
                val intentToGalery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGalery)
            }
        }else{
            if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                //izin verilmemiş, izin istememiz gerekiyor.
                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.READ_EXTERNAL_STORAGE)){
                    //snackbar göstermemiz lazım, kullanıcıdan neden izin istediğimizi bir kez daha söyleyecek izin istememiz lazım
                    Snackbar.make(view,"Galeriye gidip resim seçmen gerekli!",Snackbar.LENGTH_INDEFINITE).setAction("İzin ver",View.OnClickListener { permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }).show()
                }else{
                    //İzin isteyeceğiz
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }else{
                //izin verilmiş, galeriye gidebilirim
                val intentToGalery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGalery)
            }
        }
    }

    fun Kaydet(view: View){
        val isim = binding.adText.text.toString()
        val malzeme = binding.tarifText.text.toString()

        if(secilenBitmap != null){
            val kucukBitmap = smallBitmap(secilenBitmap!!,300)
            val outputStream = ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteDizisi = outputStream.toByteArray()

            val tarif =Tarif(isim,malzeme,byteDizisi)

            //RXJava
            mDisposable.add(tarifDao.insert(tarif).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(this::handleResponseForInsert)
            )

        }

    }

    private fun handleResponseForInsert(){
        //bir önceki fragment'e dön
        val action = IntroductionFragmentDirections.actionIntroductionFragmentToListFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    fun Sil(view: View){

        if (secilenTarif != null){
            mDisposable.add(tarifDao.delete(tarif = secilenTarif!!).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(this::handleResponseForInsert))
        }
    }

    private fun registerLauncher(){

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->

            if (result.resultCode == AppCompatActivity.RESULT_OK){
                val intentFromResult = result.data
                if (intentFromResult != null){
                    secilenGorsel = intentFromResult.data

                    try {
                        if(Build.VERSION.SDK_INT >= 28){
                            val source = ImageDecoder.createSource(requireActivity().contentResolver,secilenGorsel!!)
                            secilenBitmap = ImageDecoder.decodeBitmap(source)
                            binding.imageView.setImageBitmap(secilenBitmap)
                        }else{
                            secilenBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver,secilenGorsel)
                            binding.imageView.setImageBitmap(secilenBitmap)
                        }
                    }catch (e: IOException){
                        println(e.localizedMessage)
                    }
                }
            }

        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {result->

            if(result){
                //izin verildi Galeriye gidebiliriz
                val intentToGalery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGalery)
            }else{
                //izin verilmedi
                Toast.makeText(requireContext(),"İzin Verilmedi!",Toast.LENGTH_LONG).show()
            }

        }

    }

    private fun smallBitmap(chosenBitmap: Bitmap, maxSize: Int): Bitmap{
        var width = chosenBitmap.width
        var height = chosenBitmap.height

        val bitmapRatio: Double = width.toDouble() / height.toDouble()

        if (bitmapRatio >= 1){
            //görsel yatay
            width = maxSize
           val kisaltilmisHeight = width / bitmapRatio
           height = kisaltilmisHeight.toInt()
        }else{
            //görsel dikey
            height = maxSize
            val kisaltilmisWidth = height * bitmapRatio
            width = kisaltilmisWidth.toInt()
        }

        return Bitmap.createScaledBitmap(chosenBitmap,width,height,true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }
}