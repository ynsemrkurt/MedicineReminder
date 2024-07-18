package com.example.medicinereminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicinereminder.Room.Medicine
import com.example.medicinereminder.Room.MedicineDatabase
import com.example.medicinereminder.databinding.FragmentReminderSettingsBinding

class ReminderSettingsFragment : Fragment() {

    private lateinit var medicineAdapter: MedicineAdapter
    private lateinit var binding: FragmentReminderSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReminderSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imageButtonBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        //RecyclerView'i kurar
        binding.recyclerViewReminder.layoutManager = LinearLayoutManager(requireContext())
        medicineAdapter = MedicineAdapter(requireContext())
        binding.recyclerViewReminder.adapter = medicineAdapter

        loadMedicines()
    }

    //Database'den verileri çeker
    private fun loadMedicines() {
        val medicineDao = MedicineDatabase.getDatabase(requireContext()).medicineDao()
        val medicineList: List<Medicine> = medicineDao.getAllMedicines()
        medicineAdapter.submitMedicineList(medicineList)
    }
}
