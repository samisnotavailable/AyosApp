package com.example.ayosapp.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.ayosapp.data.BookingsData
import com.example.ayosapp.databinding.ItemBookingWorkerBinding
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WorkerBookingAdapter(
    private val context: Context,
    private val bookingsList: ArrayList<BookingsData>,
    private val UID: String
) :
    RecyclerView.Adapter<WorkerBookingAdapter.WorkerBookingsViewHolder>() {

    inner class WorkerBookingsViewHolder(val binding: ItemBookingWorkerBinding) :
        RecyclerView.ViewHolder(binding.root)

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerBookingsViewHolder {
        return WorkerBookingsViewHolder(
            ItemBookingWorkerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: WorkerBookingsViewHolder, position: Int) {
        val currentItem = bookingsList[position]
        holder.binding.apply {
            //gets address details from address collection using addressid
            firestore.collection("address").document(currentItem.addressID!!).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        //gets address line of address to pass to textview
                        val addressline = documentSnapshot.getString("address")
                        AddressLine.text = addressline
                    } else {
                        // Document does not exist
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle errors
                }

            val time = timestampToString(currentItem.timeScheduled)
            ServiceBookingWorker.text = currentItem.service
            itemDate2.text = time

            ProblemDescription.text = currentItem.details
        }
        holder.binding.GetBookingBtn.setOnClickListener{
            ConfirmationDialog(currentItem.bookingId)
        }
    }

    override fun getItemCount(): Int {
        return bookingsList.size
    }

    //parses firebase timestamp to java SimpleDateFormat
    fun timestampToString(timestamp: com.google.firebase.Timestamp?): String {
        val date = timestamp?.toDate()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val dateString = dateFormat.format(date!!)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeString = timeFormat.format(date)

        return "$dateString\nat $timeString"
    }

    //changes values to assign booking to working
    private fun AssignBooking(documentId: String?) {
        //connects to firestore
        val db = Firebase.firestore
        //gets local time of device
        val timeNow = Calendar.getInstance().time
        //finds booking using bookingID
        val query = db.collection("booking").whereEqualTo("bookingId", documentId)
        query.get().addOnSuccessListener {
            //changes values
            val updateMap = mapOf(
                "workerAssigned" to UID,
                "status" to "matched",
                "timeUpdated" to timeNow
            )
            //passes data to collection "booking" of document with documentId
            db.collection("booking").document(documentId.toString()).update(updateMap)
            Toast.makeText(context, "Booking Assigned!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context,"Error, please try again", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkBookingLimitForDay(doc: String?) {
        // Get the current date
        val currentDate = Calendar.getInstance()
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH) + 1 // Month is zero-based, so add 1
        val day = currentDate.get(Calendar.DAY_OF_MONTH)

        // Construct the start and end of the current day
        val startOfDay = Timestamp(Date(year - 1900, month - 1, day, 0, 0))
        val endOfDay = Timestamp(Date(year - 1900, month - 1, day, 23, 59, 59))
        Log.d("TAG", "Start of day: $startOfDay")
        Log.d("TAG", "End of day: $endOfDay")
        // Query Firestore to get all bookings made by the user for the that day
        val db = FirebaseFirestore.getInstance()
        val query = db.collection("booking")
            .whereEqualTo("workerAssigned", UID) // Assuming UID is the ID of the current user
            .whereGreaterThanOrEqualTo("timeScheduled", startOfDay)
            .whereLessThanOrEqualTo("timeScheduled", endOfDay)

        query.get().addOnSuccessListener { documents ->
            if (documents.size() >= 3) {
                // User has already booked three times for the day, disable further bookings
                nomoreDialog()
            } else {
                // User can make a new booking
                AssignBooking(doc)
            }
        }.addOnFailureListener { exception ->
            // Handle any errors
            Log.e("TAG", "Error getting bookings", exception)
        }
    }
    //shows confirmation dialog to ensure worker wants to accept booking
    private fun ConfirmationDialog(doc: String?) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Accept Booking")
        builder.setMessage("Are you sure you want to accept this booking?")
        builder.setPositiveButton("YES") { dialog, _ ->
            AssignBooking(doc)
        }
        builder.setNegativeButton("NO") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }
    private fun nomoreDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Limit at 3 bookings")
        builder.setMessage("Sorry we cant give you this booking as you've reached your limit for that day")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }
}