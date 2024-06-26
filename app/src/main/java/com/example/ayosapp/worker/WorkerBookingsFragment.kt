package com.example.ayosapp.worker

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ayosapp.R
import com.example.ayosapp.adapter.WorkerBookingAdapter
import com.example.ayosapp.data.BookingsData
import com.example.ayosapp.databinding.FragmentWorkerBookingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class WorkerBookingsFragment : Fragment() {
    private var dataArrayList = ArrayList<BookingsData>()
    private lateinit var binding: FragmentWorkerBookingsBinding
    private lateinit var recyclerViewBookings: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWorkerBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataArrayList = arrayListOf()
        recyclerViewBookings = view.findViewById(R.id.bookingsAvailableRV)
        val layoutManager = LinearLayoutManager(requireActivity())
        recyclerViewBookings.layoutManager = layoutManager
        fetchDataFromFirestore()
    }
    private fun fetchDataFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val bookingRef = db.collection("booking")
        bookingRef.whereEqualTo("status", "booked")
            .orderBy("timeScheduled", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    dataArrayList.add(document.toObject(BookingsData::class.java))
                }
                val adapter = WorkerBookingAdapter(requireActivity(),dataArrayList, userId)
                recyclerViewBookings.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents.", exception)
            }
    }
}