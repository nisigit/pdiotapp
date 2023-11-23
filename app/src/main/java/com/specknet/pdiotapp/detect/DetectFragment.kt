package com.specknet.pdiotapp.detect

import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.gms.internal.zzhu.runOnUiThread
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.utils.Constants

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DetectFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DetectFragment : Fragment() {



    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet
    lateinit var classifierSpinner: Spinner

    var time = 0f
    lateinit var allRespeckData: LineData

    lateinit var respeckChart: LineChart
    lateinit var detectedActivity: TextView

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper

    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detect, container, false)
        setupCharts(view)
        setupClassifierSpinner(view)


        return view
    }

    fun setupClassifierSpinner(view: View) {
        val classifiers = resources.getStringArray(R.array.respeckClassifiers)
        classifierSpinner = view.findViewById(R.id.classifierSpinner)
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, classifiers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // make the spinner text colour white
        classifierSpinner.adapter = adapter
        classifierSpinner.setSelection(0, false)

        classifierSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {

                val newClassifier = when (parent.getItemAtPosition(position).toString()) {
                    "General activities" -> GENERAL_CLASSIFIER
                    "Stationary activities" -> STATIONARY_CLASSIFIER
                    else -> TASK3_CLASSIFIER
                }

                if (newClassifier != currentClassifier) {
                    currentClassifier.clearInputBuffer()
                    currentClassifier = newClassifier
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                classifierSpinner.setSelection(0, false)
                currentClassifier = GENERAL_CLASSIFIER
            }
        }

    }

    fun setupCharts(view: View) {
        respeckChart = view.findViewById(R.id.respeck_chart_detect)

        // Respeck
        time = 0f
        val entries_res_accel_x = ArrayList<Entry>()
        val entries_res_accel_y = ArrayList<Entry>()
        val entries_res_accel_z = ArrayList<Entry>()

        dataSet_res_accel_x = LineDataSet(entries_res_accel_x, "Accel X")
        dataSet_res_accel_y = LineDataSet(entries_res_accel_y, "Accel Y")
        dataSet_res_accel_z = LineDataSet(entries_res_accel_z, "Accel Z")

        dataSet_res_accel_x.setDrawCircles(false)
        dataSet_res_accel_y.setDrawCircles(false)
        dataSet_res_accel_z.setDrawCircles(false)

        dataSet_res_accel_x.color = ContextCompat.getColor(
            requireContext(),
            R.color.red
        )
        dataSet_res_accel_y.color = ContextCompat.getColor(
            requireContext(),
            R.color.green
        )
        dataSet_res_accel_z.color = ContextCompat.getColor(
            requireContext(),
            R.color.blue
        )

        val dataSetsRes = ArrayList<ILineDataSet>()
        dataSetsRes.add(dataSet_res_accel_x)
        dataSetsRes.add(dataSet_res_accel_y)
        dataSetsRes.add(dataSet_res_accel_z)

        allRespeckData = LineData(dataSetsRes)
        respeckChart.data = allRespeckData
        respeckChart.invalidate()

    }


    fun updateGraph(graph: String, x: Float, y: Float, z: Float) {
        // take the first element from the queue
        // and update the graph with it
        if (graph == "respeck") {
            dataSet_res_accel_x.addEntry(Entry(time, x))
            dataSet_res_accel_y.addEntry(Entry(time, y))
            dataSet_res_accel_z.addEntry(Entry(time, z))

            runOnUiThread {
                allRespeckData.notifyDataChanged()
                respeckChart.notifyDataSetChanged()
                respeckChart.invalidate()
                respeckChart.setVisibleXRangeMaximum(150f)
                respeckChart.moveViewToX(respeckChart.lowestVisibleX + 40)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        GENERAL_CLASSIFIER = Classifier(
            requireContext(),
            "ten_sec.tflite",
            50,
            3,
            10,
            11,
            GENERAL_ACTIVITIES
        )

        STATIONARY_CLASSIFIER = Classifier(
            requireContext(),
            "conv_model_task2_50_3.tflite",
            50,
            3,
            10,
            15,
            STATIONARY_ACTIVITIES
        )

        TASK3_CLASSIFIER = Classifier(
            requireContext(),
            "cnn_model_task3_100_20.tflite",
            100,
            3,
            20,
            TASK3_ACTIVITIES.size,
            TASK3_ACTIVITIES
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterReceiver(respeckLiveUpdateReceiver)
        looperRespeck.quit()
    }

    companion object {

        val GENERAL_ACTIVITIES = listOf(
            "Sitting/Standing",
            "Lying down on left",
            "Lying down on right",
            "Lying down on back",
            "Lying Down on Stomach",
            "Walking normally",
            "Running",
            "Descending stairs",
            "Ascending stairs",
            "Shuffle walking",
            "Miscellaneous movements"
        )

        val STATIONARY_ACTIVITIES = listOf(
            "Sitting/standing and breathing normally",
            "Lying down left and breathing normally",
            "Lying down right and breathing normally",
            "Lying down on back and breathing normally",
            "Lying down on stomach and breathing normally",
            "Sitting/standing and coughing",
            "Lying down on left and coughing",
            "Lying down on right and coughing",
            "Lying down on back and coughing",
            "Lying down on stomach and coughing",
            "Sitting/standing and hyperventilating",
            "Lying down on left and hyperventilating",
            "Lying down on right and hyperventilating",
            "Lying down on back and hyperventilating",
            "Lyging down on stomach and hyperventilating"
        )

        val TASK3_ACTIVITIES = listOf(
            "sitting_standing breathingNormal",
            "lyingLeft breathingNormal",
            "lyingRight breathingNormal",
            "lyingBack breathingNormal",
            "lyingStomach breathingNormal",
            "sitting_standing coughing",
            "lyingLeft coughing",
            "lyingRight coughing",
            "lyingBack coughing",
            "lyingStomach coughing",
            "sitting_standing hyperventilating",
            "lyingLeft hyperventilating",
            "lyingRight hyperventilating",
            "lyingBack hyperventilating",
            "lyingStomach hyperventilating",
            "sitting_standing singing",
            "lyingLeft singing",
            "lyingRight singing",
            "lyingBack singing",
            "lyingStomach singing",
            "sitting_standing laughing",
            "lyingLeft laughing",
            "lyingRight laughing",
            "lyingBack laughing",
            "lyingStomach laughing",
            "sitting_standing talking",
            "lyingLeft talking",
            "lyingRight talking",
            "lyingBack talking",
            "lyingStomach talking",
            "sitting/standing eating",
        )


    }
}