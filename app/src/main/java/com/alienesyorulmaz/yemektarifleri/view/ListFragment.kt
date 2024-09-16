package com.alienesyorulmaz.yemektarifleri.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.alienesyorulmaz.yemektarifleri.adapter.TarifAdapter
import com.alienesyorulmaz.yemektarifleri.databinding.FragmentListBinding
import com.alienesyorulmaz.yemektarifleri.model.Tarif
import com.alienesyorulmaz.yemektarifleri.roomdb.TarifDAO
import com.alienesyorulmaz.yemektarifleri.roomdb.TarifDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var  db: TarifDatabase
    private lateinit var tarifDao: TarifDAO
    private val mDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Room.databaseBuilder(requireContext(),TarifDatabase::class.java,"Tarifler").build()
        tarifDao = db.TarifDAO()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.floatingActionButton.setOnClickListener{yeniTarif(it)}
        binding.listRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        getData()
    }

    private fun getData(){
        mDisposable.add(tarifDao.getAll().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(this::handleResponse)
        )
    }

    private fun handleResponse(tarifler: List<Tarif>){
        val adapter =TarifAdapter(tarifler)
        binding.listRecyclerView.adapter = adapter
    }

    fun yeniTarif(view: View){
        val action = ListFragmentDirections.actionListFragmentToIntroductionFragment(information = "yeni",id = -1)
        Navigation.findNavController(view).navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }
}